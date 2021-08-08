#
#  Makefile - runLua tool makefile
#
#  [The "BSD license"]
#  Copyright (c) 2020 Carlos Henrique Cabral Duarte
#  All rights reserved.
#  
#  Redistribution and use in source and binary forms, with or without
#  modification, are permitted provided that the following conditions
#  are met:
#  1. Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#  2. Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer in the
#     documentation and/or other materials provided with the distribution.
#  3. The name of the author may not be used to endorse or promote products
#     derived from this software without specific prior written permission.
#  THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
#  IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
#  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
#  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
#  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
#  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
#  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
#  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
#  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
#  THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

OS_TYPE = $(shell uname)
OS = -D_LINUX_

# DEBUG = -g
CFLAGS = $(DEBUG) -fPIC -march=native -mtune=native -Ofast -flto -fwhole-program $(OS) -fno-common -fstrict-aliasing -fpermissive -Wall -Wstrict-aliasing 

INC_LUA = /usr/local/include
INC_LUAJIT = /usr/local/include/luajit-2.0

LIBS = -ldl -lm -lpthread -lrt
LIB_LUA = -llua
LIB_LUAJIT = -lluajit-5.1

LIBS_LUA = -L/usr/local/lib
LIBS_LUAJIT = -L/usr/local/lib

ARCHTYPE = $(shell uname -p)

ifeq ($(shell uname -m),x86_64)
ARCH = -D__x86_64__
endif

CFLAGS += $(ARCH)

AR = ar
RANLIB = ranlib
LDFLAGS =

HOME = .

PHOENIX = phoenix
LIB_PHOENIX = lib$(PHOENIX)

INC_PDIR = include
LIB_PDIR = lib

TARGET = $(LIB_PHOENIX).a
LIB_DEP = $(HOME)/$(LIB_PDIR)/$(TARGET)
LIBS_PHOENIX = -L$(HOME)/$(LIB_PDIR) -l$(PHOENIX)

SEQ_LUA_OBJS = luarun-seq.o 
NSEQ_LUA_OBJS = luarun-nseq.o
SEQ_LUAJIT_OBJS = luajitrun-seq.o
NSEQ_LUAJIT_OBJS = luajitrun-nseq.o
PROGS = luarun-seq luarun-nseq luajitrun-seq luajitrun-nseq

default : all

all: $(PROGS)

luarun-seq: $(SEQ_LUA_OBJS) 
	$(CXX) $(CFLAGS) -o $@ $(SEQ_LUA_OBJS) $(LIBS) $(LIB_LUA) $(LIBS_LUA)

luarun-nseq: $(NSEQ_LUA_OBJS) $(LIB_DEP)
	$(CXX) $(CFLAGS) -o $@ $(NSEQ_LUA_OBJS) $(LIBS) $(LIBS_PHOENIX) $(LIB_LUA) $(LIBS_LUA)

luajitrun-seq: $(SEQ_LUAJIT_OBJS) $(LIB_DEP)
	$(CXX) $(CFLAGS) -o $@ $(SEQ_LUAJIT_OBJS) $(LIBS) $(LIB_LUAJIT) $(LIBS_LUAJIT)

luajitrun-nseq: $(NSEQ_LUAJIT_OBJS) $(LIB_DEP)
	$(CXX) $(CFLAGS) -o $@ $(NSEQ_LUAJIT_OBJS) $(LIBS) $(LIBS_PHOENIX) $(LIB_LUAJIT) $(LIBS_LUAJIT)
	
luarun-seq.o: luarun-seq.cpp
	$(CXX) $(CFLAGS) -c $< -o $@ -I$(HOME)/$(INC_PDIR) -I$(INC_LUA)

luarun-nseq.o: luarun-nseq.cpp
	$(CXX) $(CFLAGS) -c $< -o $@ -I$(HOME)/$(INC_PDIR) -I$(INC_LUA)
	
luajitrun-seq.o: luajitrun-seq.cpp
	$(CXX) $(CFLAGS) -c $< -o $@ -I$(HOME)/$(INC_PDIR) -I$(INC_LUAJIT)

luajitrun-nseq.o: luajitrun-nseq.cpp
	$(CXX) $(CFLAGS) -c $< -o $@ -I$(HOME)/$(INC_PDIR) -I$(INC_LUAJIT)

clean:
	rm -f $(PROGS) *.o *.a *~ *.tmp *.bak *.log
