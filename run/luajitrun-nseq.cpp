/***
  luajitrun-nseq.cpp
  Runs Lua scripts by parallelizing map applications  
 
  [The "BSD license"]
  Copyright (c) 2019-2021, Carlos Henrique Cabral Duarte
  Portions Copyright (c) 2007-2011, Stanford University
  Portions Copyright (C) 2005-2017 Mike Pall
  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:
  1. Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
  2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
  3. Neither the name of Stanford University nor the names of its 
     contributors may be used to endorse or promote products derived from 
     this software without specific prior written permission.
  THIS SOFTWARE IS PROVIDED BY STANFORD UNIVERSITY ``AS IS'' AND ANY
  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
  DISCLAIMED. IN NO EVENT SHALL STANFORD UNIVERSITY BE LIABLE FOR ANY
  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
***/ 

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <iostream>
#include <dlfcn.h>
#include <limits.h>
#include <errno.h>

#include "map_reduce.h"

extern "C" {
   #include "lua.h"
   #include "lauxlib.h"
   #include "lualib.h"
   #include "luajit.h"
}

/* shared definitions for code analysis */
#define DASH   '-'
#define SPACE  ' '
#define NL     '\n'
#define END    '\0'
#define SQUOTE '\''
#define DQUOTE '\"'
#define SLASH  '\\'
#define EQUAL  '='

/* The standard library names for Lua 5.1 (aka LuaJIT 2.01 compatibility) */
static char const *standard_libs[] = {
    "base", 
    "package", 
    "string", 
    "table",
    "math",
    "io",
    "os",
    "debug",
    "jit"
};

/* shared variables with all pthreads */
#define NUM_LIBS 9
static int load_lib[NUM_LIBS];

int *main_argc;
lua_Number ***main_argv;

static long gsize = 0;
static char *generated = NULL;

struct COMPILED {
    char *name;
    char *code;
    int csize;
    struct COMPILED *next;
};
static struct COMPILED* compiled = NULL;

static void *lib = NULL;
lua_Number (*_mainCFunc)(lua_Number) = NULL;

/* verifies if a given string contains a number */
static int isnumber(char *s) {
   char *endptr;
   const double val = strtod(s, &endptr);
   errno = 0;
   if ((endptr == s) ||
       (errno && val == 0) ||
       (errno == ERANGE && (val == LONG_MAX || val == LONG_MIN)))
      return 0;
   else
      return 1;
}

/* global functions and procedures */ 
static void init_loadlibs() {
   int i = 0;
   while (i < NUM_LIBS)
      load_lib[i++] = 0;
   load_lib[0] = 1;
   load_lib[8] = 1;
}

static void checkset_loadlib(char *s) {
   char *q = s;
   while (q && *q != SQUOTE && *q != DQUOTE)
       q++;
   if (q++) {
      int i = 0;
      while (i < NUM_LIBS) {
         if (strncmp(q, standard_libs[i], strlen(standard_libs[i])) == 0) {
            load_lib[i] = 1;
            if (i > 1) 
               load_lib[1] = 1;
            i = NUM_LIBS;
         } else
            i += 1;
      }
   }
}

static void lua_loadlibs(lua_State *L) {
   int i = 0;
   lua_gc(L, LUA_GCSTOP, 0);
   while (i < NUM_LIBS) {
      if (load_lib[i]) {
         switch (i) { 
            case 0:
               luaopen_base(L);
               break;
            case 1:
               luaopen_package(L);
               break;
            case 2:
               luaopen_string(L);
               break;
            case 3:
               luaopen_table(L);
               break;
            case 4:
               luaopen_math(L);
               break;
            case 5:
               luaopen_io(L);
               break;
            case 6:
               luaopen_os(L);
               break;
            case 7:
               luaopen_debug(L);
               break;
            case 8:
               luaopen_jit(L);
               break;
         }
         lua_setglobal(L, standard_libs[i]);
      }
      i += 1;
   }
   lua_gc(L, LUA_GCRESTART, -1);
}

#define LUA_REQUIRE "require"
#define LUA_MAP_MAIN "_main"
static char *_mainLuaFunc = NULL;

static void read_generated(char *b, int l) {
   long i = 3;
   long k = 0;
   char *c = generated;
   *c = END;
   while (i <= l) {
      const char *bi = (char *) b + i;
      if (i == l || *bi == NL) {
          
         /* identifies generated code */
         if ((*(bi - 1) == SPACE) &&
             (*(bi - 2) == DASH) && 
             (*(bi - 3) == DASH)) {
             
            /* checks if the generated code is not a comment to be ignored */ 
            char *bk = (char *) b + k;
            if (*bk != DASH ||
                *(bk + 1) != DASH) {
            
               /* deals with generated code which is not a comment */
               long asize = 0;
               while (asize <= i - k - 4) 
                  *(c + asize++) = *bk++;

               /* identifies main function */
               if (!_mainCFunc && !_mainLuaFunc) {
                  if (strncmp(c, LUA_MAP_MAIN, sizeof(LUA_MAP_MAIN) - 1) == 0) {
                     *(c + asize - 1) = END;
                     if (lib) { 
                        _mainCFunc = dlsym(lib, c + 8);
                        if (!_mainCFunc) {
                           fprintf(stderr, 
                              "lua: error loading main function in dynamic library: %s.\n", 
                              dlerror());
                           exit(1);
                        }
                     } else
                        _mainLuaFunc = strdup(c + 8);
                     *(c + asize - 1) = SPACE;
                  }
               } 

               /* finalize line separation */
               c += asize;
               *c++ = NL; *c = END;
               gsize += ++asize;
            }
         } 
         k = i + 1;
      } else 
          
         /* checks if any dynamic library is required */
         if (strncmp(bi, LUA_REQUIRE, sizeof(LUA_REQUIRE) - 1) == 0)
            checkset_loadlib(bi);
      i += 1;
   }
}

#define LUA_LOAD "load"

static void treat_generated() {
   int i = 0;
   char *c = generated;
   char *name = NULL;
   char *code = NULL;
   struct COMPILED *current = compiled;
   while (i < gsize) {
      while (*c == DASH) {
         while (*c != NL) {
            c++; i++; 
         }
         c++; i++;
      }
      name = c;
      while (*c != EQUAL) {
         c++; i++;
      }
      code = c + 2;
      if (strncmp(code, LUA_LOAD, sizeof(LUA_LOAD) - 1) == 0) {
          
         /* separates compiled functions from the generated code */ 
         *(c - 1) = END;          
         while (*c != SQUOTE && *c != DQUOTE) {
            c++; i++;
         }
         c++; i++;
         code = c;
         while (*c != SQUOTE && *c != DQUOTE) {
            c++; i++;
         }
         *c = END;         
         c++; i++;
         
         /* stores name and code generated on a linked list */
         struct COMPILED *link = 
            (struct COMPILED *) malloc(sizeof(struct COMPILED));
         CHECK_ERROR (link == NULL);
         link->name = strdup(name);
         const int bsize = strlen(code) / 4;
         char *b = (char *) malloc (bsize * sizeof(char));
         CHECK_ERROR (b == NULL);
         char dec[4];
         int j = 0;
         while (j < bsize) {
             strncpy(dec, code + j*4 + 1, 3);
             dec[3]=END;
             *(b + j++) = atoi(dec);
         }
         link->code = b;
         link->csize = bsize;
         
         /* sets up linked list structure */
         link->next = NULL;
         if (compiled == NULL)
            compiled = link;
         else
            current->next = link;
         current = link;
      }
      while (*c != NL) {
         c++; i++;
      }
      c++; i++;
   }
}

static void load_generated(lua_State *L) {
   struct COMPILED *current = compiled;
   while (current) {
      luaL_loadbuffer(L, current->code, current->csize, current->name);
      lua_setglobal(L, current->name);
      current = current->next;
   }
}

static void free_compiled() {
   struct COMPILED *current = compiled;
   while (current) {
      struct COMPILED *old = current;
      free(current->name);
      free(current->code);
      current = current->next;
      free(old);
   }
}

/* procedures for dealing with Lua stacks */
static void push_args(lua_State *L, int c, lua_Number *v[]) {
   int i;
   lua_Number m;
   lua_createtable(L, c - 2, 0);
   for(i=0; i < c - 2; i++) {
      m = *v[i + 2];
      /* push_value */
      if ((m - ((lua_Integer) m)) > 0)
         lua_pushnumber(L, m); 
      else
         lua_pushinteger(L, m);
      lua_rawseti(L, -2, i + 1); /* assign key */
   }
   lua_setglobal(L, "arg"); /* get named table */    
}

/* definitions for Map Reduce */
// #define MUST_USE_HASH

struct ENTRY {
    lua_Integer key;
    lua_Number val;
};

/* functions and procedures for Map Reduce */
#ifdef MUST_USE_HASH
class LuaMR : public MapReduce<LuaMR, ENTRY, lua_Integer, lua_Number, hash_container< lua_Integer, lua_Number, one_combiner, std::tr1::hash<lua_Integer>
#elif defined(MUST_USE_FIXED_HASH)
class LuaMR : public MapReduce<LuaMR, ENTRY, lua_Integer, lua_Number, fixed_hash_container< lua_Integer, lua_Number, one_combiner, 131072, std::tr1::hash<lua_Integer>
#else
class LuaMR : public MapReduce<LuaMR, ENTRY, lua_Integer, lua_Number, common_array_container< lua_Integer, lua_Number, one_combiner, 3001135 // 1857860 // 1270608
#endif
> >
{
public:
    void map(data_type const& e, map_container& out) const
    {
       if (lib) 
          emit_intermediate(out, e.key, _mainCFunc(e.val));
       else { 
          lua_State *L1 = luaL_newstate(); 
          lua_loadlibs(L1); 
          push_args(L1, *main_argc, *main_argv); 
          load_generated(L1); 
          lua_getglobal(L1, _mainLuaFunc); 
          lua_pushnumber(L1, e.val); 
          lua_call(L1,1,1); 
          emit_intermediate(out, e.key, lua_tonumber(L1, -1));
          lua_close(L1);
       } 
    }
};

/* based on code from Programming in Lua, 4th edition, page 255 */
static int lua_map(lua_State *L) {
    const lua_Integer n = lua_objlen(L, 2); /* #a */
    register lua_Integer i;

    lua_createtable(L, n, 0); /* new b */ 
    ENTRY* fdata = (ENTRY *) malloc (n * sizeof(ENTRY));
    CHECK_ERROR (fdata == NULL);

    for(i = n; i;) { 
       lua_rawgeti(L, 2, i--);
       fdata[i].key = i+1;
       fdata[i].val = lua_tonumber(L, 4);
       lua_pop(L, 1); /* removes value from main_stack */
    }
    
    std::vector<LuaMR::keyval> result;
    LuaMR mapReduce;
    CHECK_ERROR( mapReduce.run(fdata, n, result) < 0);    

    for(i = n; i; ) { 
       lua_pushnumber(L, result[--i].val); /* push b[i] */
       lua_rawseti(L, -2, result[i].key); /* b[i] = pop */
    }

    free (fdata);
    return 1; /* results in a table */
}

/* excerpt from the LuaJIT web page */ 
// Catch C++ exceptions and convert them to Lua error messages.
static int wrap_exceptions(lua_State *L, lua_CFunction f) {
  try {
      // Call wrapped function and return result
      return f(L);  
   } // Catch and convert exceptions
     catch (const char *s) {  
      lua_pushstring(L, s);
   } catch (std::exception& e) {
      lua_pushstring(L, e.what());
   } catch (...) {
      lua_pushliteral(L, "caught (...). Please check your code.");
   }
   // Rethrow as a Lua error.
   return lua_error(L);  
}

/* Run command with options. */
static int runcmdopt(lua_State *L, const char *opt) {
   int narg = 0;
   if (opt && *opt) {
      for(;;) {  /* Split arguments. */
          const char *p = strchr(opt, ',');
          narg++;
          if (!p) break;
          if (p == opt)
             lua_pushnil(L);
          else
             lua_pushlstring(L, opt, (size_t)(p - opt));
          opt = p + 1;
       }
       if (*opt)
          lua_pushstring(L, opt);
       else
          lua_pushnil(L);
   }
   return lua_pcall(L, narg, 0, 0);
}

/* Optimization flags. */
static int dojitopt(lua_State *L, const char *opt) {
   lua_getfield(L, LUA_REGISTRYINDEX, "_LOADED");
   lua_getfield(L, -1, "jit.opt");  /* Get jit.opt.* module table. */
   lua_remove(L, -2);
   lua_getfield(L, -1, "start");
   lua_remove(L, -2);
   return runcmdopt(L, opt);
}

int main (int argc, char *argv[]) {
   char *filename = NULL;
   char *buffer = NULL;
   
   if (argc > 1)
      filename = argv[1];
   FILE *f = fopen (filename, "rb");
   if (f) {
       
      /* loads the source code file in memory */
      fseek(f, 0, SEEK_END);
      const long length = ftell(f);
      fseek(f, 0, SEEK_SET);
      buffer = (char *) malloc(length *  sizeof(char));
      CHECK_ERROR (buffer == NULL);
      fread(buffer, 1, length, f);
      fclose (f);

      /* checks if a dynamic library exists and loads it in this case */
      const int stripped = strlen(filename) - 4;
      char *libfilename = (char *) malloc((stripped + 3) * sizeof(char));
      CHECK_ERROR (libfilename == NULL);
      strncpy(libfilename, filename, stripped);
      strncpy(libfilename + stripped, ".so\0", 4);
      f = fopen (libfilename, "rb");
      if (f) { 
         fclose(f);
         lib = dlopen(libfilename, RTLD_NOW);
         if (lib) { 
            main_argc = (int *) dlsym(lib, "main_argc");
            if (!main_argc) {
               fprintf(stderr,
                       "lua: error loading argc in dynamic library: %s\n", 
                       dlerror());
               exit(1);
            }
            main_argv = (lua_Number ***) dlsym(lib, "main_argv");
            if (!main_argv) { 
               fprintf(stderr, 
                       "lua: error loading argv in dynamic library: %s\n",
                       dlerror());
               exit(1);
            }
            dlerror(); /* just to clean previous messages */
         } else {
            fprintf(stderr, "lua: error loading dynamic library %s: %s.\n",
               libfilename, dlerror());
            exit(1);
         } 
      } else {
         main_argc = (int *) malloc(sizeof(int));
         CHECK_ERROR (main_argc == NULL);
         main_argv = (lua_Number ***) malloc(sizeof(lua_Number **));
         CHECK_ERROR (main_argv == NULL);
      }

      /* deals with precompiled code in the source file */
      if (buffer) {
         init_loadlibs();
         generated = (char *) malloc(length * sizeof(char));
         CHECK_ERROR (generated == NULL);
         read_generated(buffer, length);
         if (!lib) 
            treat_generated();
      }
   } else {
       
      /* treats source file loading error */
      fprintf(stderr, "lua: cannot open/read %s: No such file or directory.\n",
              filename);
      exit(1);
   }
   
   if (buffer && generated) { 
       
      lua_State *L = luaL_newstate(); /* opens Lua */
      lua_loadlibs(L); /* loads the required libs */
   
      // Define wrapper function and enable it.
      lua_pushlightuserdata(L, (void *)wrap_exceptions);
      luaJIT_setmode(L, -1, LUAJIT_MODE_WRAPCFUNC|LUAJIT_MODE_ON);
      lua_pop(L, 1);

      // Set optimization on 
      dojitopt(L, "-O3");
      
      /* registers the map function */
      lua_pushcfunction(L, lua_map);
      lua_setglobal(L, "map");
   
      /* prepares and takes arguments from command line */
      *main_argc = argc;
      lua_Number **cargv = (lua_Number **) malloc (argc * sizeof(lua_Number *));
      CHECK_ERROR (cargv == NULL);
      *main_argv = cargv;
      int i;
      for(i=0; i<argc; i++) {
         if (i && isnumber(argv[i])) {
            cargv[i] = (lua_Number *) malloc(sizeof(lua_Number));
            CHECK_ERROR (cargv[i] == NULL);
            *(cargv[i]) = atof(argv[i]);
         } else
            cargv[i] = NULL;
      }
      push_args(L, *main_argc, *main_argv);

      /* loads the source code */      
      int error = luaL_loadstring(L, buffer);
      if (error)
         fprintf(stderr, "lua: cannot load %s\n", lua_tostring(L, -1));
      else {

         /* runs the program */
         error = lua_pcall(L, 0, 0, 0);
         if (error)
            fprintf(stderr, "lua: cannot execute %s\n", lua_tostring(L, -1));
      }
      if (error) 
          
         /* pop error message from the stack */
         lua_pop(L, 1); 

      lua_close(L);
      if (compiled) 
         free_compiled();
      free(generated);
      if (lib) 
         dlclose(lib);
      else {
         free(main_argc);
         free(main_argv);
      }
      free(buffer);
   }
   return 0;
}
