/***
  luarun-seq.cpp
  Runs Lua scripts without parallelizing map applications  

  [The "BSD license"]
  Copyright (c) 2019-2021 Carlos Henrique Cabral Duarte
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

extern "C" {
   #include "lua.h"
   #include "lauxlib.h"
   #include "lualib.h"
}

/* procedures for dealing with Lua stacks */
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
   const lua_Integer n = luaL_len(L, 2); /* #a */
   register lua_Integer i;
   lua_createtable(L, n, 0); /* new b */ 
   for (i=n; i; ) {
      lua_pushvalue(L, 1); /* push f */
      lua_rawgeti(L, 2, i); /* push a[i] */
      lua_call(L, 1, 1); /* appy f(a[i]) */
      lua_rawseti(L, -2, i--); /* b[i] = pop */
   }
   return 1; /* results in a table */
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
