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
import java.lang.StackTraceElement;

public class LuaAnalyser implements Analyser { 

    private static SymbolTable symtab = new SymbolTable(); 
    private static int verbosity = 0;
    private Language lang;
    private String cmdLineName;
    private String sourceCodeName;
    private String generator;

    public LuaAnalyser (String h, String x, 
                        int n, int f, int i, int s, int v, String c) { 
       this.lang = new LuaLanguage(n, f, i, s);
       this.cmdLineName = h;
       this.sourceCodeName = x;
       this.generator = c;
       verbosity = v;
       symtab.setGlobals(lang.getBuiltIns());
    }
    
    public SymbolTable getSymbolTable() { return symtab; }
    public static int getVerbosity() { return verbosity; }
    public Language getLanguage() { return lang; }
    public String getCmdLineName() { return cmdLineName; }
    public String getSourceCodeName() { return sourceCodeName; }
    public String getGenerator() { return generator; }

    @Override
    public void reportError(Integer l, Integer p, String m) {
       System.err.printf(cmdLineName+": line %d(%d): %s\n", l, p, m);
       System.exit(1);
    }
   
    @Override
    public void reportError(Integer l, String m) {
       System.err.printf(cmdLineName+": line %d: %s\n", l, m);
       System.exit(1);
    }

    @Override 
    public void reportError(String m) {
       System.err.printf(cmdLineName+": "+m+"\n");
       System.exit(1);
    }
    
    public static String genVarName() { 
       int nextVar = symtab.genNewName();
       String name = LuaLanguage.varPrefix + nextVar;
       return name;
    }

    public String genOutVarName(String name) { 
       return LuaLanguage.outVarPrefix + name;
    }

    public String genInVarName(String name) {
       return LuaLanguage.inVarPrefix + name;
    }

    public String genForVarName() {
       int nextVar = symtab.genNewName();
       String name = LuaLanguage.forVarPrefix + nextVar;
       return name;
    }
     
    public static boolean isGenVarName(String name) {
       return (name.startsWith(LuaLanguage.varPrefix));
    }

    public static boolean isGenOutVarName(String name) {
       return (name.startsWith(LuaLanguage.outVarPrefix));
    }

    public static boolean isGenInVarName(String name) {
       return (name.startsWith(LuaLanguage.inVarPrefix));
    }

    public static LuaType mkNewGenVarType() {
       int verb = verbosity;
       if ( verb > 1 ) {
          StackTraceElement[] stackTraceElements = 
                                 Thread.currentThread().getStackTrace();
          StackTraceElement ste = stackTraceElements[2];
          System.out.printf("%s:Reaching %s\n", 
                             ste.getClassName(), ste.getMethodName());
       }
       Scope scope = symtab.getGlobals();
       VarType va = new VarType(genVarName());
       String name = va.getVarName();
       scope.defineName(new VariableSymbol(name,VarType._Unknown,0),verb);
       return va;
    }

    public static LuaType mkNewUnbGenFunctType() {
       int verb = verbosity;
       if ( verb > 1 ) {
          Object o = new Object(){};
          System.out.printf("%s:Reaching %s\n", o.getClass().getName(),
                             o.getClass().getEnclosingMethod().getName());
       }
       Scope scope = symtab.getGlobals();
       VarType va = new VarType(genVarName());
       String name = va.getVarName();
       scope.defineName(new VariableSymbol(name,VarType._Unknown,0),verb);
       VarType vb = new VarType(genVarName());
       name = vb.getVarName();
       scope.defineName(new VariableSymbol(name,VarType._Unknown,0),verb);
       return new FunctionType(va, vb);
    }

    public static LuaType unbindVariables(LuaType t, int verb) {
       LuaType tp = t;
       while (tp instanceof QuantifiedType) {
          if ( verb > 0 ) 
             System.out.printf("Unbinding: [%s]\n", tp);
          VarType vo = ((QuantifiedType) tp).getTVar();
          VarType vn = (VarType) mkNewGenVarType();
          LuaType te = ((QuantifiedType) tp).getTExpr();
          tp = te.subst(vo,vn);
       }
       System.gc();
       return tp;
    }    
 
    public static LuaType opReturnType(String name) {
       int verb = verbosity;
       if ( verb > 0 ) 
          System.out.printf("Operation: [%s]\n", name);
       Symbol symb = symtab.getGlobals().resolveName(name);
       LuaType type = LuaType.copy((LuaType)symb.getType()); 
       type = unbindVariables(type,verb); 
       if (type instanceof FunctionType) { 
          type = ((FunctionType) type).getRetTypes();
          if ((type instanceof SequenceType) &&
              (((SequenceType) type).getElements().size() == 1))
             type = ((SequenceType) type).getElements().get(0);
       }
       return type;
    }

    public static void reportRule(String s) {
       int verb = verbosity;
       if ( verb > 1 )
          System.out.printf("Applying: %s\n", s);
    }
}
