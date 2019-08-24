/***
  Symbol.java

  Excerpted from "The Definitive ANTLR 4 Reference",
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
import java.io.Serializable;

public class Symbol implements Serializable { 

    private static final long serialVersionUID = 1L;

    String name;  
    Type type;    
    int line;    
    Scope scope;  

    public Symbol() { 
       this.name = "";
       this.type = null;
       this.line = 0;
    }

    public Symbol(String n) { 
       this.name = n; 
       this.type = null; 
       this.line = 0;
    }

    public Symbol(Type t) {
       this.name = "";
       this.type = t;
       this.line = 0;
    }

    public Symbol(String n, Type t) { 
       this.name = n; 
       this.type = t; 
       this.line = 0;
    }

    public Symbol(String n, Type t, int l) {
       this.name = n;  
       this.type = t; 
       this.line = l;
    }
    
    public String getName() { return name; }
    public Type getType() { return type; }
    public int getLine() { return line; }
    public Scope getScope() { return scope; }

    public void setName(String n) { this.name = n; }
    public void setType(Type t) { this.type = t; }
    public void setScope(Scope s) { this.scope = s; }

    public static String stripBrackets(String s) {
        return s.substring(1,s.length()-1);
    }
    
    public String toString() {
        String s = line + ":";
        if ( scope != null ) 
           s = scope.getScopeName()+".";
        s = s+getName();
        if ( type!=null ) 
           return ""+s+":."+type+"";
        else
           return s;
    }
}
