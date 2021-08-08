
# README file for HpcLua released on 08.Aug.2021

This is a type inference and code generation tool for Lua.

## License ##

HpcLua is released under the BSD Licence below.

  [The "BSD license"]
  Copyright (c) 2019 Carlos Henrique Cabral Duarte
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

## Installing 

In order to install and run HpcLua in your Linux system, you have to install:

. gcc 
(normally included in most Linux distributions);

. Lua 5.3
(available from https://www.lua.org/ftp/lua-5.3.6.tar.gz)

. LuaJIT 2.05
(available from https://luajit.org/download/LuaJIT-2.0.5.tar.gz)

. Java 8 
(available from Oracle at https://www.java.com/pt-BR/download/)

. ANTLR 4 for Java
(available at https://github.com/antlr/antlr4/blob/master/doc/java-target.md)

Please check access paths and environment variables required to run these tools.

## Building ##

Once you have installed the software above in your systen, just run:

$ ./make

## Running ##

There is a script to run HpcLua. 

Just type:

$ ./hpcluatool -help

We have included in this distributions the following two lua program:

   original.lua
   heavy.lua

Thay are made available so that you can experimennt with HpcLua in you system.

In order to execute the generated software using some parallelizing standalone, 
copy the generated code to the "run" folder and use some executable therein.

