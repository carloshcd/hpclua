/***
  LuaAnalyser.java

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
***/
public class LuaAnalyser implements Analyser { 

    private static SymbolTable symtab = new SymbolTable(); 
    private Language lang;

    public LuaAnalyser (int n, int f, int i) { 
       this.lang = new LuaLanguage(n, f, i);
       symtab.setGlobals(lang.getBuiltIns());
    }
    
    public SymbolTable getSymbolTable() { return symtab; }
    public Language getLanguage() { return lang; }

    public void setSymbolTable(SymbolTable st) { LuaAnalyser.symtab = st; }
    public void setLanguage(Language l) { this.lang = l; }

    public void reportError(Integer l, Integer p, String m) {
       System.err.printf("line %d:%d %s\n", l, p, m);
       System.exit(1);
    }
   
    public void reportError(Integer l, String m) {
       System.err.printf("line %d %s\n", l, m);
       System.exit(1);
    }
 
    public void reportError(String m) {
       System.err.printf(m+"\n");
       System.exit(1);
    }
    
    public static String genVarName() { 
       int nextVar = symtab.genNewName();
       return LuaLanguage.varPrefix + nextVar;
    }

    public String genRetVarName(String name) { 
       return LuaLanguage.returnVarPrefix + name;
    }
     
    public static boolean isGenVarName(String name) {
       return (name.startsWith(LuaLanguage.varPrefix));
    }

    public static boolean isGenRetVarName(String name) {
       return (name.startsWith(LuaLanguage.returnVarPrefix));
    }

    public static LuaType unbindVariables(LuaType t) {
       LuaType tp = t;
       while (tp instanceof QuantifiedType) {
          VarType vo = ((QuantifiedType) tp).getTVar();
          VarType vn = new VarType(genVarName());
          String name = vn.getVarName();
          LuaType te = ((QuantifiedType) tp).getTExpr();
          tp = te.subst(vo,vn);
          Scope scope = symtab.getGlobals();
          scope.defineName(new VariableSymbol(name, VarType._Unknown, 0));
       }
       return tp;
    }    
   
    public static LuaType opReturnType(String name) {
       Symbol symb = symtab.getGlobals().resolveName(name);
       LuaType type = LuaType.copy((LuaType)symb.getType()); 
       type = unbindVariables(type); 
       if (type instanceof FunctionType) { 
          type = ((FunctionType) type).getRetTypes();
          if (type instanceof SequenceType) {
             if (((SequenceType) type).getElements().size() == 1)
                type = ((SequenceType) type).getElements().get(0);
          }
       }
       return type;
    }
}
