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
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.Serializable;

public class FunctionSymbol extends Symbol implements Scope, Serializable {

    private static final long serialVersionUID = 1L;

    Map<String,Symbol> symbols;
    Scope enclosingScope;

    public FunctionSymbol(String n, Type t, Scope s) {
        super(n, t);
        this.symbols = new HashMap<String,Symbol>();
        this.enclosingScope = s;
    }

    public FunctionSymbol(String n, Type t, int l, Scope s) {
        super(n, t, l);
        this.symbols = new HashMap<String,Symbol>();
        this.enclosingScope = s;
    }

    public Scope getEnclosingScope() { return this.enclosingScope; }
    
    public void setEnclosingScope(Scope s) { this.enclosingScope = s; }
    
    public Map<String, Symbol> getSymbols() { return this.symbols; }
    
    public void setSymbols(Map<String, Symbol> s) { this.symbols = s; }    
    
    public void defineName(Symbol symb) {
        symbols.put(symb.getName(), symb);
        symb.setScope(this);  
    }
    
    public Symbol resolveName(String n) {
        Symbol symb = symbols.get(n);
        if ( symb != null ) 
           return symb;
        else
           if ( getEnclosingScope() != null ) 
              return getEnclosingScope().resolveName(n);
           else
              return null; 
    }
 
    public Symbol getEnclosingFunction() {
       return this;
    }

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

    public Scope copy() {
        FunctionSymbol obj = null;
        try {
        
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            out.close();

            ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray()));
            obj = (FunctionSymbol) in.readObject();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        catch(ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return obj;
    }
    
    public void mergeWith(Scope s) {
       if (s != null) {
          Map<String,Symbol> m = s.getSymbols();
          for(Map.Entry<String, Symbol> me : m.entrySet()) 
             m.put(me.getKey(), me.getValue());
       }
    }
}
