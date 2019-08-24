/***
  SymbolTable.java

  Excerpted from "Language Implementation Patterns",
  published by The Pragmatic Bookshelf.

  [The "BSD license"]
  Copyright (c) 2012 Terence Parr
  Portions Copyright (c) 2019 Carlos Henrique Cabral Duarte
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
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class SymbolTable {
    GlobalScope globals;
    ParseTreeProperty<Scope> scopes;
    int lastGeneratedName;

    public static final int firstGenName = 1;
    
    public SymbolTable() {
        this.globals = new GlobalScope(null);
        this.scopes = new ParseTreeProperty<Scope>();
        this.lastGeneratedName = firstGenName;
    }
        
    public GlobalScope getGlobals() { return globals; }
    public ParseTreeProperty<Scope> getScopes() { return scopes; }
    
    public void setGlobals(GlobalScope globs) { this.globals = globs; }
    public void setScopes(ParseTreeProperty<Scope> scops) { this.scopes = scops; }
    
    public int getLastGen() { return lastGeneratedName; }
    public int genNewName() { return ++lastGeneratedName; }
    
    public String genName(String radix) { 
       int nextVar = genNewName();
       return radix + nextVar;
    }
    
    public boolean isGenName(String radix, String name) {
       if (name.equals("")) 
          return false;
       else
          return (name.startsWith(radix));
    }
    
    public String toString() { return globals.toString(); }
}
