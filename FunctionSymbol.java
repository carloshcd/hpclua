/***
  FunctionSymbol.java
 
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
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.Boolean;

public class FunctionSymbol extends Symbol implements Scope {

    Map<String,Symbol> symbols;
    Scope enclosingScope;
    List<FunctionSymbol> calledFuncts;
    Boolean dealsJustWithNumKeyTables;
    
    public FunctionSymbol(String n, Type t, Scope s) {
        super(n, t);
        this.symbols = new HashMap<String,Symbol>();
        this.enclosingScope = s;
        this.calledFuncts = new ArrayList<FunctionSymbol>();
        this.dealsJustWithNumKeyTables = null;
    }

    public FunctionSymbol(String n, Type t, int l, Scope s) {
        super(n, t, l);
        this.symbols = new HashMap<String,Symbol>();
        this.enclosingScope = s;
        this.calledFuncts = new ArrayList<FunctionSymbol>();
        this.dealsJustWithNumKeyTables = null;
    }

    @Override
    public Scope getEnclosingScope() { return this.enclosingScope; }
   
    @Override 
    public void setEnclosingScope(Scope s) { this.enclosingScope = s; }
   
    @Override 
    public Map<String, Symbol> getSymbols() { return this.symbols; }
   
    @Override 
    public void setSymbols(Map<String, Symbol> s) { this.symbols = s; }    
    
    @Override 
    public void defineName(Symbol symb, int verb) {
       symbols.put(symb.getName(), symb);
       symb.setScope(this);  
       if ( verb > 1 )   
          System.out.printf("Def: [%s]\n", symb);
    }
   
    @Override 
    public Symbol resolveName(String n) {
       Scope scope = this;
       do { 
          Symbol symb = scope.getSymbols().get(n);
          if (symb == null) 
             scope = scope.getEnclosingScope();
          else
             return symb;
       } while (scope != null);
       return null;
    }

    @Override
    public String getScopeName() { return super.name; }
    
    @Override
    public String toString() { 
       String s = getName();
       if (this.symbols != null) 
          s = s + "("+stripBrackets(symbols.keySet().toString())+")"; 
       if (this.getType() != null) 
          s = s + ":." + this.getType();
       return s; 
    }

    @Override 
    public void mergeWith(Scope s) {
       if (s != null) 
          this.getSymbols().putAll(s.getSymbols());
    }

    @Override
    public FunctionSymbol getEnclosingFunction(boolean anon) {
       if (!isGen(this.getName()) || anon)
          return this;
       else
          if (enclosingScope == null)
             return null;
          else
             return enclosingScope.getEnclosingFunction(anon);
    }

    public List<FunctionSymbol> getCalledFuncts() { 
       return this.calledFuncts; 
    }

    public void addCalledFunct(FunctionSymbol f) {
       if (!this.equals(f))
          this.calledFuncts.add(f);
    }

    public Boolean getDealsJustWithNumKeyTables() { 
       return this.dealsJustWithNumKeyTables; 
    }

    public void setDealsJustWithNumKeyTables(boolean b) {
       if (this.dealsJustWithNumKeyTables == null)
          this.dealsJustWithNumKeyTables = b;
       else 
          if (this.dealsJustWithNumKeyTables.equals(Boolean.TRUE) && 
              b == false)
             this.dealsJustWithNumKeyTables = false;
    }
}
