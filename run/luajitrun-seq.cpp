/***
  luajitrun-seq.pp
  Runs LuaJIT scripts without parallelizing map applications  

  [The "BSD license"]
  Copyright (c) 2019-2021 Carlos Henrique Cabral Duarte
  Portions Copyright (C) 2005-2017 Mike Pall
  All rights reserved.
  
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:
  1. Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
  2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
  3. The name of the author may not be used to endorse or promote products
     derived from this software without specific prior written permission.
  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
***/
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <iostream>

extern "C" {
   #include "lua.h"
   #include "lauxlib.h"
   #include "lualib.h"
   #include "luajit.h"
}

static void push_args(lua_State *L, int c, char *v[]) {
   int i;
   lua_createtable(L, c - 2, 0);
   for(i = 0; i < c - 2; i++) {
      lua_pushstring(L, v[i + 2]); /* push value */
      lua_rawseti(L, -2, i + 1); /* assign key */
   }
   lua_setglobal(L, "arg"); /* get named table */    
}

/* based on code from Programming in Lua, 4th edition, page 255 */
static int lua_map (lua_State *L) {
   const lua_Integer n = lua_objlen(L, 2); /* #a */
   register lua_Integer i;
   lua_createtable(L, n, 0); /* new b */
   for(i=n; i; ) {
      lua_pushvalue(L, 1); /* push f */
      lua_rawgeti(L, 2, i); /* push a[i] */
      lua_call(L, 1, 1); /* apply f(a[i]) */
      lua_rawseti(L, -2, i--); /* b[i] = pop */
   }
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
    for (;;) {  /* Split arguments. */
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
static int dojitopt(lua_State *L, const char *opt)
{
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
      fseek(f, 0, SEEK_END);
      const long length = ftell(f);
      fseek(f, 0, SEEK_SET);
      buffer = (char *) malloc(length);
      if (buffer) {
         fread(buffer, 1, length, f);
         fclose (f);
      }
   } else {
      fprintf(stderr, "lua: cannot open/read %s: No such file or directory.\n",
              filename);
      exit(1);
   }

   if (buffer) {

      lua_State *L = luaL_newstate(); /* opens Lua */
      luaL_openlibs(L); /* opens the standard libraries */
   
      // Define wrapper function and enable it.
      lua_pushlightuserdata(L, (void *)wrap_exceptions);
      luaJIT_setmode(L, -1, LUAJIT_MODE_WRAPCFUNC|LUAJIT_MODE_ON);
      lua_pop(L, 1);
      
      // Set optimization on 
      dojitopt(L, "-O3");

      /* code for registering the map function */
      lua_pushcfunction(L, lua_map);
      lua_setglobal(L, "map");
   
      /* takes arguments from command line */
      push_args(L, argc, argv);
        
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

      free(buffer);
      lua_close(L);
   } else {
      fprintf(stderr, "lua: cannot allocate memory.\n");
      exit(1);
   }
   return 0;
}
