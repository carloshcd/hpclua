/***
  RefPhase.java

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
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.List;
import java.util.ArrayList;

public class RefPhase extends HpcLuaFullListener {
    LuaAnalyser analyser; 
    int strict;
    int verb;

    public RefPhase(LuaAnalyser a) {
        super();
        this.analyser = a;
        this.strict = LuaTypeSystem.getStrictness();
        this.verb = LuaAnalyser.getVerbosity();
    }
        
    public void error(Token t, String m) {
       analyser.reportError(t.getLine(), t.getCharPositionInLine(), m);
    }   

    public void error(int l, String m) {
       analyser.reportError(l, m);
    } 
   
    public void reportReach(Object o) {
       if ( this.verb > 1 ) 
          System.out.printf("%s:Reaching %s\n", o.getClass().getName(), 
                             o.getClass().getEnclosingMethod().getName());
    }

    public void reportReach(Object o, 
                            ParserRuleContext ctx) {
       if ( this.verb > 1 ) 
          System.out.printf("%s:Reaching %s line %d:%s\n", 
                             o.getClass().getName(),
                             o.getClass().getEnclosingMethod().getName(),
                             ctx.start.getLine(), ctx.getText());
    }

    public void reportReach(Object o, 
                            ParserRuleContext ctx,
                            Token t) {
       if ( this.verb > 1 ) 
          System.out.printf("%s:Reaching %s line %d(%d):%s\n", 
                            o.getClass().getName(),
                            o.getClass().getEnclosingMethod().getName(),
                            ctx.start.getLine(), t.getCharPositionInLine(),
                            ctx.getText());
    }
    
    public void reportLeave(Object o) {
       if ( this.verb > 1 ) 
          System.out.printf("%s:Leaving %s\n", o.getClass().getName(), 
                             o.getClass().getEnclosingMethod().getName());
    }

    public void reportLeave(Object o, 
                            ParserRuleContext ctx) {
       if ( this.verb > 1 ) 
          System.out.printf("%s:Leaving %s line %d:%s\n", 
                             o.getClass().getName(),
                             o.getClass().getEnclosingMethod().getName(),
                             ctx.start.getLine(), ctx.getText());
    }

    public void reportLeave(Object o, 
                            ParserRuleContext ctx,
                            Token t) {
       if ( this.verb > 1 ) 
          System.out.printf("%s:Leaving %s line %d(%d):%s\n", 
                            o.getClass().getName(),
                            o.getClass().getEnclosingMethod().getName(),
                            ctx.start.getLine(), t.getCharPositionInLine(),
                            ctx.getText());
    }
 
    public void checkUnOp(String name, int line, 
                          LuaType ctxtype, LuaType t1) {
       reportReach(new Object(){});
       Symbol op = this.getCurrentScope().resolveName(name);
       if (op != null) {
          LuaType dt = (LuaType) op.getType();
          if (dt instanceof QuantifiedType || dt.freeTVars().size() > 0) {
              // Abs
              dt = LuaAnalyser.unbindVariables(LuaType.copy(dt),verb);
              LuaAnalyser.reportRule("Abs");
          }
          if (dt instanceof FunctionType) { // UOp
             boolean fail = true;
             SequenceType paramtypes = ((FunctionType) dt).getParamTypes();
             SequenceType rettype = ((FunctionType) dt).getRetTypes();
             LuaType paramtype = paramtypes.getElements().get(0);
             LuaType argtype = t1; 
             Scope s1 = paramtype.unifiesWith(this.getCurrentScope(),
                                              argtype,verb);
             if (s1 != null) {
                argtype = ctxtype;
                if (!(argtype instanceof SequenceType))
                   argtype = new SequenceType (argtype);
                Scope s2 = rettype.unifiesWith(s1,argtype,verb);
                if (s2 != null) {
                   this.getCurrentScope().mergeWith(s2);
                   fail = false;
                   LuaAnalyser.reportRule("UOp");
                }
             }
             if (fail)
                error(line, "The types in an expression do not unify");
          } else
             new Throwable().printStackTrace();
       } else
          new Throwable().printStackTrace();
      reportLeave(new Object(){});
     }

    public void checkBinOp(String name, int line, 
                           LuaType ctxtype, LuaType t1, LuaType t2) {
       reportReach(new Object(){});
       Symbol op = this.getCurrentScope().resolveName(name);
       if (op != null) {
          LuaType dt = (LuaType) op.getType();
          if (dt instanceof QuantifiedType || dt.freeTVars().size() > 0) {
             // Abs
             dt = LuaAnalyser.unbindVariables(LuaType.copy(dt),verb); 
             LuaAnalyser.reportRule("Abs");
          }
          if (dt instanceof FunctionType) { // BOp
             boolean fail = true;
             SequenceType paramtypes = ((FunctionType) dt).getParamTypes();
             SequenceType rettype = ((FunctionType) dt).getRetTypes();
             LuaType paramtype = paramtypes.getElements().get(0);
             LuaType argtype = t1; 
             Scope s1 = argtype.unifiesWith(this.getCurrentScope(),
                                            paramtype,verb);
             if (s1 != null) {
                paramtype = paramtypes.getElements().get(1);
                argtype = t2;
                Scope s2 = paramtype.unifiesWith(s1,argtype,verb);
                if (s2 != null) {
                   argtype = ctxtype;
                   if (!(argtype instanceof SequenceType))
                      argtype = new SequenceType (argtype);
                   Scope s3 = rettype.unifiesWith(s2,argtype,verb);
                   if (s3 != null) {
                      this.getCurrentScope().mergeWith(s3);
                      fail = false;
                      LuaAnalyser.reportRule("BOp");
                   }
                }
             }
             if (fail)
                error(line, "The types in an expression do not unify");
          } else
             new Throwable().printStackTrace(); 
       } else
          new Throwable().printStackTrace();
       reportLeave(new Object(){});
    }

    public void checkFunctDef(String name, int line, 
                              HpcLuaParser.FunctBodyContext ctxf) {
       reportReach(new Object(){});
       Symbol funct = this.getCurrentScope().resolveName(name);
       if (funct != null) { // (L)FAbs
          Scope s1 = null;
          boolean fail = true;

          LuaType functtype = (LuaType) funct.getType();
          LuaType paramtype = ((FunctionType) functtype).getParamTypes();
          LuaType rettype   = ((FunctionType) functtype).getRetTypes();

          LuaType argtype = LuaType._Bottom; 
          if (ctxf.parList() != null)
             argtype = ctxf.parList().type;
          if (!(argtype instanceof SequenceType))
             argtype = new SequenceType (argtype);
          else if (((SequenceType) argtype).size() > 1) {
                  paramtype = ((SequenceType) paramtype).unpack();
          }
          s1 = argtype.unifiesWith(this.getCurrentScope(),paramtype,verb);
          if (s1 != null) {
             LuaType restype = LuaType._Any;
             if (ctxf.anyTypeAscr() != null) 
                restype = ctxf.anyTypeAscr().anyObjectType().type;
             if (!(restype instanceof SequenceType))
                restype = new SequenceType (restype);
             Scope s2 = rettype.unifiesWith(s1,restype,verb);
             if (s2 != null) {
                this.getCurrentScope().mergeWith(s2);
                fail = false;
                LuaAnalyser.reportRule("(L)FAbs");
             }
          }
          if (fail)
             error(line, "The types in a statement do not unify");
          else {
             if (paramtype instanceof VarType &&
                 paramtype.expand(this.getCurrentScope()).subtype(
                    argtype.expand(this.getCurrentScope()))) {
                String varname = ((VarType) paramtype).getVarName();
                Symbol varsymb = this.getCurrentScope().resolveName(varname);
                varsymb.setType(argtype);
             }
             functtype = ((FunctionType) 
                            (functtype.expand(this.getCurrentScope()))).
                                bindFree();
             funct.setType(functtype);

             if (!"".equals(analyser.getGenerator())) {
                if (functtype.subtype(LuaTypeSystem._mapType)) {
                   funct.setAsScheduler(true);
                }
             } 
          }
       } else
          new Throwable().printStackTrace();
       reportLeave(new Object(){});
    }

    public SequenceType adjustSequence(SequenceType sequence) {
       reportReach(new Object(){});
       List<LuaType> result = new ArrayList<LuaType>();
       List<LuaType> seq = sequence.getElements();
       int last = seq.size()-1; 
       for(int i=0;i<=last;i++) {
          LuaType obj = seq.get(i);
          obj = LuaType.expand(this.getCurrentScope(), obj);
          if (obj instanceof SequenceType) {
             if (i == last) 
                result.addAll(((SequenceType) obj).getElements());
             else 
                result.add(((SequenceType) obj).getElements().get(0));
          } else
             result.add(obj); 
       }
       reportLeave(new Object(){});
       return new SequenceType(result); 
    }

    public SequenceType completeSequence(SequenceType param, int n) {
       List<LuaType> result = new ArrayList<LuaType>();
       List<LuaType> seq = param.getElements();
       int nel = seq.size(); 
       int last = ((n > nel) ? n : nel) - 1;
       for(int i = 0; i <= last; i++) { 
          if (i < nel) 
             result.add(seq.get(i));
          else
             result.add(LuaType._Any);
       }   
       return new SequenceType(result);
    }

    public boolean isAllNumeric(Scope s, FunctionSymbol a) {
       LuaType type = LuaType.expand(s, (LuaType) a.getType());
       if (type instanceof FunctionType && type.isNumericType()) { 
          for (FunctionSymbol g : a.getCalledFuncts()) {
             if (!isAllNumeric(s, g))
                return false;
          }
          return true;
       }
       return false;
    }    
    
    public boolean isAllNumeric(Scope s, HpcLuaParser.ExpContext arg) {
       Symbol argsymb = s.resolveName(arg.getText());
       if (argsymb == null && arg instanceof HpcLuaParser.PrefixExpExpContext) {
           HpcLuaParser.VarOrExpContext voectx = 
              ((HpcLuaParser.PrefixExpExpContext) arg).prefixExp().varOrExp();
           argsymb = s.resolveName(voectx.getText());
       }
       if (argsymb != null && argsymb instanceof FunctionSymbol) { 
          LuaType type = LuaType.expand(s, (LuaType) argsymb.getType());
          if (type instanceof FunctionType && type.isNumericType() ) { 
             for (FunctionSymbol g : 
                     ((FunctionSymbol) argsymb).getCalledFuncts()) {
                if (!isAllNumeric(s, g))
                   return false;
             }
             return true;
          }
       } 
       return false;
    }

    public void setAllScheduled(Scope s, FunctionSymbol a, FunctionSymbol f) {
       a.setScheduled(f);
       for (FunctionSymbol b : a.getCalledFuncts()) {
          setAllScheduled(s, b, f);
       }
    }    
    
    public void setAllScheduled(Scope s, HpcLuaParser.ExpContext arg, 
                                FunctionSymbol f) {
       Symbol argsymb = s.resolveName(arg.getText());
       if (argsymb == null && arg instanceof HpcLuaParser.PrefixExpExpContext) {
           HpcLuaParser.VarOrExpContext voectx = 
              ((HpcLuaParser.PrefixExpExpContext) arg).prefixExp().varOrExp();
           argsymb = s.resolveName(voectx.getText());
       }
       if (argsymb != null && argsymb instanceof FunctionSymbol) {    
          argsymb.setScheduled(f);
          for (FunctionSymbol b : 
                  ((FunctionSymbol) argsymb).getCalledFuncts()) {
             setAllScheduled(s, b, f);
          }
       }
    }
    
    public LuaType checkFunctCall(String functname, LuaType functtype, 
            int line, LuaType ctxtype, HpcLuaParser.ArgsContext ctxf) {
       reportReach(new Object(){});
       Scope s1 = null;
       boolean fail = true;
       LuaType ftype = functtype;
     
       ftype = LuaType.expand(this.getCurrentScope(), ftype);
       if (ftype instanceof QuantifiedType) {
           // Abs 
           ftype = LuaAnalyser.unbindVariables(LuaType.copy(ftype),verb); 
           LuaAnalyser.reportRule("Abs");
       }
       if (!(ftype instanceof FunctionType)) {
          if (ftype instanceof SequenceType &&
              ((SequenceType) ftype).size() == 1)
             ftype = ((SequenceType) ftype).unpack();
          LuaType type = LuaAnalyser.mkNewUnbGenFunctType(); 
          s1 = ftype.unifiesWith(this.getCurrentScope(),type,verb);
          if (s1 != null) {
             this.getCurrentScope().mergeWith(s1);
             String tempname = ((VarType) ftype).getVarName(); 
             ftype = type;

             LuaType tmptype = ((FunctionType) type).getRetTypes();
             this.getCurrentScope().defineName(
                new VariableSymbol(analyser.genOutVarName(tempname),
                                   tmptype), verb); 
             tmptype = ((FunctionType) type).getParamTypes();
             this.getCurrentScope().defineName(
                new VariableSymbol(analyser.genInVarName(tempname),
                                   tmptype), verb);
          } else
             error(line, "The applied expression is not a function");
       }
       LuaType paramtype = ((FunctionType) ftype).getParamTypes();
       LuaType rettype   = ((FunctionType) ftype).getRetTypes();
      
       // FApp or PDFApp
       LuaType argtype = ctxf.type; 
       boolean paramVar = false;
       if (!(argtype instanceof SequenceType))
          argtype = new SequenceType (argtype);
       else if (((SequenceType) argtype).size() > 1) {
               paramtype = ((SequenceType) paramtype).unpack();
               paramVar = (paramtype instanceof VarType);
               if (!(paramtype instanceof SequenceType) && !paramVar)
                  paramtype = new SequenceType (paramtype);
       }
       argtype = adjustSequence((SequenceType) argtype);
       if (!paramVar) 
          paramtype = completeSequence((SequenceType) paramtype, 
                                       ((SequenceType) argtype).size());
       s1 = paramtype.unifiesWith(this.getCurrentScope(),argtype,verb);
       Scope s2 = null;
       if (s1 != null) {
          LuaType restype = ctxtype;
          if (!(restype instanceof SequenceType) &&
              ((SequenceType) rettype).size() == 1)
             restype = new SequenceType (restype);
          s2 = rettype.unifiesWith(s1,restype,verb);
          if (s2 != null) {
             this.getCurrentScope().mergeWith(s2);
             fail = false;
             LuaAnalyser.reportRule("FApp | PDFApp");
          }
       }
       if (fail)
          error(line,"The types in function call do not unify "+
                     "with formal parameters");
       else {
          if (!"".equals(analyser.getGenerator())) {
             Symbol functsymb = s2.resolveName(functname);
             if (functsymb != null && functsymb instanceof FunctionSymbol) {
                LuaType newfuncttype = (LuaType) functsymb.getType(); 
                if (newfuncttype.subtype(LuaTypeSystem._mapType)) {
                   HpcLuaParser.ExpContext firstArg = null;
                   HpcLuaParser.ExpContext secondArg = null;
                   if (ctxf.expList.getText() != null){
                      firstArg = ctxf.expList.exp;
                      if (ctxf.expList.el.getText() != null) 
                         secondArg = ctxf.expList.el.exp;
                   }
                   if (isAllNumeric(s2, firstArg))
                      setAllScheduled(s2, firstArg, (FunctionSymbol) functsymb);
                   if (secondArg != null) { 
                      LuaType type = LuaType.expand(s2, secondArg.type);
                      if (type.hasJustNumericKeys()) 
                         ((FunctionSymbol) functsymb).
                            setDealsJustWithNumKeyTables(true);
                      else
                         ((FunctionSymbol) functsymb). 
                            setDealsJustWithNumKeyTables(false);
                   }
                } else {
                   FunctionSymbol g = s2.getEnclosingFunction(false);
                   if (g != null) {
                      g.addCalledFunct((FunctionSymbol) functsymb);
                   }
                }
             }
          }
       }
       reportLeave(new Object(){});
       return rettype;
    }

    public List<String> nameListToVarNameList(ParserRuleContext ctx,
                                              boolean global) {
       List<String> result = new ArrayList<String>();
       ParserRuleContext vars = ctx;
       while (vars != null) {
          String name;
          if (global)
             name = ((HpcLuaParser.VarListContext) vars).var().
                       varNameAndType().varName().NAME().getText();
          else
             name = ((HpcLuaParser.NameListContext) vars).
                       varNameAndType().varName().NAME().getText();
          result.add(name);
          if (global)
             vars = ((HpcLuaParser.VarListContext) vars).varList();
          else
             vars = ((HpcLuaParser.NameListContext) vars).nameList();
       }
       return result; 
    }

    public Scope unifiesOrExtendsLists (Scope s, 
                    List<String> vars,
                    boolean global,
                    List<LuaType> l1, List<LuaType> l2, 
                    int verb) {
       reportReach(new Object(){});
       Scope result = s;
       LuaType obj1;
       LuaType obj2;
       int nel = l1.size();
       for(int i=0;i<nel;i++) {
          obj1 = l1.get(i);
          if (i >= l2.size())
             obj2 = LuaType._Nil;
          else
             obj2 = l2.get(i);
          Scope s1 = obj1.unifiesWith(result,obj2,verb);
          if (s1 == null) {
             if (((strict == 0 || strict == 1) && global) || 
                 (strict == 0 && !global)) {
                String name = vars.get(i);
                LuaType elem = obj2;
                LuaType ntype = elem;
                while (global && elem instanceof VarType) {
                   Symbol symb = s.resolveName(((VarType) elem).getVarName());
                   if (symb != null) {
                      ntype = (LuaType) symb.getType();
                      if (ntype == null || ntype.equals(VarType._Unknown)) 
                         break;
                      else
                         elem = ntype;
                   }
                }
                if (global && ntype!=null && !ntype.equals(VarType._Unknown)) {
                   List<LuaType> ext = new ArrayList<LuaType>();
                   ext.add(obj1);
                   ext.add(obj2);
                   obj2 = new UnionType (
                                 adjustSequence(new SequenceType (ext))); 
                } else 
                   if (strict == 0) 
                      obj2 = LuaType._Nil;
                   else
                      obj2 = null;
                if (obj2 != null) {
                   Symbol var = s.resolveName(name);
                   result = obj1.unifiesWith(result,obj2,verb);
                   if (obj1 instanceof VarType && var != null) { 
                      var.setType(obj2);
                      ((VarType) obj1).setType(obj2); 
                      l1.set(i,obj2);
                   }
                } else
                   result = null;
             } else 
                result = null;
          } else
             result = s1;
       }
       reportLeave(new Object(){});
       return result;
    }
    
    public void enterVarType(HpcLuaParser.VarTypeContext ctx) {
       reportReach(new Object(){}, ctx, ctx.NAME().getSymbol());
       String name = ctx.NAME().getText();
       Symbol var = this.getCurrentScope().resolveName(name);
       if ( var!=null ) {
          if (var.getType() != null && var.getType().equals(LuaType._Bottom))
             error(ctx.NAME().getSymbol(),"The label '"+name+
                                          "' is used as a type variable");
          else {
             LuaType type = (LuaType) var.getType();
             if ((type instanceof VarType) &&
                 ((LuaAnalyser.isGenVarName(((VarType) type).getVarName()))))
                   error(ctx.NAME().getSymbol(),"The program variable '"+name+
                                                "' in line "+ var.getLine()+
                                                " is used as a type variable");
          }
       } else
          new Throwable().printStackTrace();
       reportLeave(new Object(){}, ctx, ctx.NAME().getSymbol());
    }

    public void exitVarType(HpcLuaParser.ChunkContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void enterChunk(HpcLuaParser.ChunkContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(analyser.getSymbolTable().getGlobals());
       reportLeave(new Object(){}, ctx);
    }
    
    public void exitChunk(HpcLuaParser.ChunkContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void enterBlock(HpcLuaParser.BlockContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(analyser.getSymbolTable().getScopes().get(ctx));
       reportLeave(new Object(){}, ctx);
    }
    
    public void exitBlock(HpcLuaParser.BlockContext ctx) { // Blk 
       reportReach(new Object(){}, ctx);
       LuaAnalyser.reportRule("Blk");
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterGAsgnStat(HpcLuaParser.GAsgnStatContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setReverseTraversal(null);
       reportLeave(new Object(){}, ctx);
    }

    public void exitGAsgnStat(HpcLuaParser.GAsgnStatContext ctx) { // GAsgn
       reportReach(new Object(){}, ctx);
       List<LuaType> varTypes = ctx.varList().type.getElements();
       SequenceType rettype = new SequenceType (LuaType._Nil);
       if (ctx.expList() != null)
          rettype = ctx.expList().type;
       List<LuaType> expTypes = adjustSequence(rettype).getElements();
       Scope s = unifiesOrExtendsLists(analyser.getSymbolTable().getGlobals(), 
                    nameListToVarNameList(ctx.varList(),true),
                    true,varTypes,expTypes,verb); 
       if (s != null) {
          analyser.getSymbolTable().getGlobals().mergeWith(s);
          LuaAnalyser.reportRule("GAsgn");
       } else 
          error(ctx.start.getLine(), "The types in a statement do not unify");
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterFunctionCallStat(HpcLuaParser.FunctionCallStatContext 
                                         ctx) {
       reportReach(new Object(){}, ctx);
       HpcLuaParser.VarOrExpContext vOectx = ctx.functionCall().varOrExp();
       if (vOectx != null ) {
          HpcLuaParser.VarContext varctx = vOectx.var();
          if (varctx != null ) {
             HpcLuaParser.VarNameAndTypeContext varNaTctx =
                                                   varctx.varNameAndType();
             if (varNaTctx != null) {
                String functName = varNaTctx.varName().NAME().getText();
                Symbol funct = this.getCurrentScope().resolveName(functName);
                if ( funct == null )
                   error(varNaTctx.varName().NAME().getSymbol(),
                         "No function '"+functName+"' has been defined");
             }
          }
       }
       reportLeave(new Object(){}, ctx);
    }
    
    public void exitFunctionCallStat(HpcLuaParser.FunctionCallStatContext 
                                        ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterLabelStat(HpcLuaParser.LabelStatContext ctx) { // Lbl
       reportReach(new Object(){}, ctx, ctx.label().NAME().getSymbol());
       String label = ctx.label().NAME().getText();
       Symbol place = this.getCurrentScope().resolveName(label);
       if (place != null) {
          int previous = ctx.label().NAME().getSymbol().getLine();
          if (place.getLine() != previous)
             error(ctx.label().NAME().getSymbol(),"The label '"+label+
                   "' is already defined on line "+place.getLine());
          else
             LuaAnalyser.reportRule("Lbl");
       }
       reportLeave(new Object(){}, ctx, ctx.label().NAME().getSymbol());
    }
    
    public void exitLabelStat(HpcLuaParser.LabelStatContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterBreakStat(HpcLuaParser.BreakStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitBreakStat(HpcLuaParser.BreakStatContext ctx) { // Brk
       reportReach(new Object(){}, ctx);
       LuaAnalyser.reportRule("Brk");
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterGotoStat(HpcLuaParser.GotoStatContext ctx) { // Goto 
       reportReach(new Object(){}, ctx, ctx.NAME().getSymbol());
       String label = ctx.NAME().getText();
       Symbol place = this.getCurrentScope().resolveName(label);
       if (place == null)
          error(ctx.NAME().getSymbol(),
                "No label '"+label+"' has been defined");   
       else {
          LuaType type = (LuaType) place.getType();
          if (type instanceof VarType)
             error(ctx.NAME().getSymbol(),
                "The goto command refers to a variable");  
          else
             LuaAnalyser.reportRule("Goto");
       }
       reportLeave(new Object(){}, ctx, ctx.NAME().getSymbol());
    }

    public void exitGotoStat(HpcLuaParser.GotoStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void enterDoStat(HpcLuaParser.DoStatContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(analyser.getSymbolTable().getScopes().get(ctx));
       reportLeave(new Object(){}, ctx);
    }

    public void exitDoStat(HpcLuaParser.DoStatContext ctx) { // Do
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       LuaAnalyser.reportRule("Do");
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterWhileStat(HpcLuaParser.WhileStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitWhileStat(HpcLuaParser.WhileStatContext ctx) { // While
       reportReach(new Object(){}, ctx);
       LuaAnalyser.reportRule("While");
       reportLeave(new Object(){}, ctx);
    }

    public void enterRepeatStat(HpcLuaParser.RepeatStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitRepeatStat(HpcLuaParser.RepeatStatContext ctx) { // Rep
       reportReach(new Object(){}, ctx);
       LuaAnalyser.reportRule("Rep");
       reportLeave(new Object(){}, ctx);
    }

    public void enterIfStat(HpcLuaParser.IfStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitIfStat(HpcLuaParser.IfStatContext ctx) { // If
       reportReach(new Object(){}, ctx);
       LuaAnalyser.reportRule("If");
       reportLeave(new Object(){}, ctx);
    }

    public void enterForNumStat(HpcLuaParser.ForNumStatContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(analyser.getSymbolTable().getScopes().get(ctx));
       this.setSecondFirstTraversal(this.getCurrentScope());
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       reportLeave(new Object(){}, ctx);
    }

    public void exitForNumStat(HpcLuaParser.ForNumStatContext ctx) { // ForNum
       reportReach(new Object(){}, ctx);
       boolean fail = true;
       Scope s1 = (ctx.varNameAndType().type).
                     unifiesWith(this.getCurrentScope(), 
                                 ctx.exp(0).type,verb);
       if (s1 != null) {
          Scope s2 = (ctx.varNameAndType().type).unifiesWith(s1, 
                                                   ctx.exp(1).type,verb);
          if (s2 != null) { 
             if (ctx.exp(2) != null) {
                Scope s3 = (ctx.varNameAndType().type).unifiesWith(s2, 
                                                         ctx.exp(2).type,
                                                         verb);
                if (s3 != null) {
                   this.getCurrentScope().mergeWith(s3);
                   fail = false;
                }
             } else {
                this.getCurrentScope().mergeWith(s2);
                fail = false;
             }
          }
       }
       if (fail)
          error(ctx.start.getLine(),"The types in a statement do not unify");
       else
          LuaAnalyser.reportRule("ForNum");
       reportLeave(new Object(){}, ctx);
    }

    public void enterForGenStat(HpcLuaParser.ForGenStatContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(analyser.getSymbolTable().getScopes().get(ctx));
       this.setSecondFirstTraversal(this.getCurrentScope());
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       reportLeave(new Object(){}, ctx);
    }

    public void exitForGenStat(HpcLuaParser.ForGenStatContext ctx) { // ForGen
       reportReach(new Object(){}, ctx);
       List<LuaType> nameTypes = ctx.nameList().type.getElements();
       List<LuaType> expTypes = adjustSequence(
                                   ctx.expList().type).getElements();

       String name;
       List<String> varNameList = new ArrayList<String>();

       VarType tf = (VarType) LuaAnalyser.mkNewGenVarType();
       name = analyser.genForVarName();
       varNameList.add(name);
       VariableSymbol vf = new VariableSymbol(name,tf);
       this.getCurrentScope().defineName(vf,verb);

       VarType ts = (VarType) LuaAnalyser.mkNewGenVarType();
       name = analyser.genForVarName();
       varNameList.add(name);
       VariableSymbol vs = new VariableSymbol(name,ts);
       this.getCurrentScope().defineName(vs,verb);

       VarType tv = (VarType) LuaAnalyser.mkNewGenVarType();
       name = analyser.genForVarName();
       varNameList.add(name);
       VariableSymbol vv = new VariableSymbol(name,tv);
       this.getCurrentScope().defineName(vv,verb);

       List<LuaType> newNameTypes = new ArrayList<LuaType>();
       newNameTypes.add(tf);
       newNameTypes.add(ts);
       newNameTypes.add(tv);
    
       Scope s = unifiesOrExtendsLists(this.getCurrentScope(), 
                   varNameList,false,newNameTypes,expTypes,verb);
       if (s != null) {
          Scope s1 = s;
          LuaType obj = tf;
          obj = LuaType.expand(this.getCurrentScope(), obj);
          if (obj instanceof FunctionType) {
             obj = ((FunctionType) obj).getRetTypes();
             if (!(obj instanceof SequenceType))
                obj = new SequenceType (obj);
             expTypes = adjustSequence((SequenceType) obj).getElements();
             s1 = unifiesOrExtendsLists(s,
                           nameListToVarNameList(ctx.nameList(),false),
                           false,nameTypes,expTypes,verb);
             if (s1 != null) 
                this.getCurrentScope().mergeWith(s1);
             else
                s = null; 
          } else
             s = null; 
       } 
       if (s == null)
          error(ctx.start.getLine(),"The types in a statement do not unify");
       else
          LuaAnalyser.reportRule("ForGen");
       reportLeave(new Object(){}, ctx);
    }

    public void enterFunctStat(HpcLuaParser.FunctStatContext ctx) { 
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(analyser.getSymbolTable().getScopes().get(ctx));
       reportLeave(new Object(){}, ctx);
    }
    
    public void exitFunctStat(HpcLuaParser.FunctStatContext ctx) { 
       reportReach(new Object(){}, ctx);
       String funcName = ctx.functName().varName(0).NAME().getText();
       checkFunctDef(funcName, ctx.start.getLine(), ctx.functBody());
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       reportLeave(new Object(){}, ctx);
    }
	
    public void enterLocalFunctStat(HpcLuaParser.LocalFunctStatContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(analyser.getSymbolTable().getScopes().get(ctx));
       reportLeave(new Object(){}, ctx);
    }
    
    public void exitLocalFunctStat(HpcLuaParser.LocalFunctStatContext ctx) { 
       reportReach(new Object(){}, ctx);
       String funcName = ctx.varName().NAME().getText();
       checkFunctDef(funcName, ctx.start.getLine(), ctx.functBody());
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       reportLeave(new Object(){}, ctx);
    }

    public void enterLAsgnStat(HpcLuaParser.LAsgnStatContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setReverseTraversal(this.getCurrentScope());
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       reportLeave(new Object(){}, ctx);
    }

    public void exitLAsgnStat(HpcLuaParser.LAsgnStatContext ctx) { // LAsgn 
       reportReach(new Object(){}, ctx);
       List<LuaType> nameTypes = ctx.nameList().type.getElements();
       SequenceType rettype = new SequenceType (LuaType._Nil);
       if (ctx.expList() != null)
          rettype = ctx.expList().type;
       List<LuaType> expTypes = adjustSequence(rettype).getElements();
       Scope s = unifiesOrExtendsLists(this.getCurrentScope(),
                    nameListToVarNameList(ctx.nameList(),false), 
                    false,nameTypes,expTypes,verb);
       if (s != null) {
          this.getCurrentScope().mergeWith(s);
          LuaAnalyser.reportRule("LAsgn");
       } else
          error(ctx.start.getLine(),"The types in a statement do not unify");
       reportLeave(new Object(){}, ctx);
    }

    public void enterRetStat(HpcLuaParser.RetStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitRetStat(HpcLuaParser.RetStatContext ctx) { // Ret 
       reportReach(new Object(){}, ctx);
       Scope curScope = this.getCurrentScope();
       Symbol encFunct = curScope.getEnclosingFunction(true);
       if (encFunct != null) {
          String name = analyser.genOutVarName(encFunct.getName());
          Symbol varout = curScope.resolveName(name);
          if (varout != null) {
             LuaType outtype = (LuaType) varout.getType(); 
             name = analyser.genInVarName(encFunct.getName());
             Symbol varin = curScope.resolveName(name);
             LuaType intype = (LuaType) varin.getType();
             if (outtype != null && intype != null) {
                SequenceType rettype = new SequenceType (LuaType._Nil); 
                if (ctx.expList() != null)
                   rettype = ctx.expList().type;
                Symbol symb;
                LuaType elem,ntype;
                boolean functUBound = false;
                boolean functIsTBound = false;
                if ((outtype instanceof SequenceType) && 
                    (((SequenceType) outtype).size() == 1)) {
                   elem = ((SequenceType) outtype).getElements().get(0);
                   while (elem instanceof VarType) {  
                      symb = curScope.resolveName(
                                ((VarType) elem).getVarName());
                      if (symb != null) {
                         ntype = (LuaType) symb.getType();
                         if (ntype == null || ntype.equals(VarType._Unknown)) {
                            functUBound = true; 
                            break;
                         } else {
                            if (ntype instanceof VarType) 
                               functIsTBound = true;
                            elem = ntype;
                         }
                      }
                   }
                   if (rettype.size() != 1)
                      outtype = ((SequenceType) outtype).
                                     getElements().get(0);
                } 
                rettype = adjustSequence(rettype);
                boolean retUBound = false;
                boolean retIsTVar = false;
                if (rettype.size() == 1) { 
                   elem = rettype.getElements().get(0);
                   while (elem instanceof VarType) {
                      retIsTVar = true;
                      symb = curScope.resolveName(
                                ((VarType) elem).getVarName());
                      if (symb != null) {
                         ntype = (LuaType) symb.getType();
                         if (ntype == null || ntype.equals(VarType._Unknown)) {
                            retUBound = true;
                            break;
                         } else
                            elem = ntype;
                      }
                   }
                }
                Scope s = null;
                if ((!functIsTBound && !retIsTVar && !retUBound)||
                   (functUBound && retIsTVar && retUBound))
                   s = outtype.unifiesWith(curScope,rettype,verb);
                if (s != null) {
                   curScope.mergeWith(s);
                   LuaAnalyser.reportRule("Ret");
                } else {
                   if ((strict == 0) || 
                       (strict == 1 && 
                        (!functUBound || retIsTVar || !retUBound)) ||
                       (strict == 2 && 
                        (!functUBound && (retIsTVar && !retUBound)))) 
                       {
                      if (!retUBound || !functUBound) {
                         if (!(outtype instanceof SequenceType))
                            outtype = new SequenceType(outtype);
                         rettype.mergeWith((SequenceType) outtype);
                         rettype = new SequenceType(new UnionType(rettype));
                      }
                      varout.setType(rettype);
                      ((FunctionType) ((FunctionSymbol) encFunct).
                         getType()).setRetTypes(rettype);
                      LuaAnalyser.reportRule("Ret");
                   } else
                      error(ctx.start.getLine(),
                            "The types in a statement do not unify"); 
                }
             } // in the else case no type was declared for this function
          } else
             new Throwable().printStackTrace();
       } // in the else case the return value is ignored
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterVar(HpcLuaParser.VarContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitVar(HpcLuaParser.VarContext ctx) {
       reportReach(new Object(){}, ctx);
       LuaType type;
       HpcLuaParser.VarSuffixContext vs = null;
       HpcLuaParser.VarNameAndTypeContext vnatctx = ctx.varNameAndType();
       HpcLuaParser.ExpContext ectx = ctx.exp();
       int i,j;
       LuaType context;
       HpcLuaParser.ArgsContext args; 
       boolean vnat = false;
       Scope s0 = this.getCurrentScope();
       Scope s1 = s0;
       LuaType tvs;
       String name = null;
       if (vnatctx != null) {
          name = vnatctx.varName().NAME().getText();
          type = vnatctx.type;
          vnat = true;
       } else {
          type = ectx.type;
          i = 0;
          context = LuaType._Any;
          vs = ctx.vs;
          tvs = LuaType.expand(s1,vs.type);
          while (vs.nameAndArgs(i) != null) {
             args = vs.nameAndArgs(i++).args();
             if (vs.nameAndArgs(i) == null &&
                 ctx.varSuffix(0) == null)
                context = ctx.type;
             type = checkFunctCall(name,type,ctx.start.getLine(),context,args);
          }
          if ( !tvs.equals(LuaType._Bottom) ) {
             type = LuaType.expand(s1,type);
             if (type instanceof VarType ||
                 type instanceof TableType) {
                VarType tv = (VarType) LuaAnalyser.mkNewGenVarType();
                TableType ntab = new TableType(tvs, tv);
                Scope stmp = type.unifiesWith(s1,ntab,verb);
                s1.mergeWith(stmp); 
                if (type instanceof TableType &&
                    ((TableType) type).size() == 0)
                    type = ntab;
             }
          }
          type = LuaType.expand(s1,type);
          if (!(type.equals(LuaType._Bottom))) {
              if ( tvs.equals(LuaType._Bottom) ||
                   (!(LuaType.isSelectable(type))) )
                 type = LuaType._Bottom;
              else { // Case
                 type = type.select(s1,tvs,verb);
                 if (type.equals(LuaType._Bottom))
                    type = LuaType._Nil;
                 else {
                    List<LuaType> ext = new ArrayList<LuaType>();
                    ext.add(type);
                    ext.add(LuaType._Nil);
                    type = new UnionType (new SequenceType (ext));
                 }
                 LuaAnalyser.reportRule("Case");
              }
          }
       }
       boolean vsfix = false;
       i = 0;
       Scope s2 = s1;
       while (ctx.varSuffix(i) != null) {
          vsfix = true;
          type = LuaType.expand(s0,type);
          vs = ctx.varSuffix(i++);
          tvs = LuaType.expand(s2,vs.type);
          j = 0;
          context = LuaType._Any;
          while (vs.nameAndArgs(j) != null) {
             args = vs.nameAndArgs(j++).args();
             if (vs.nameAndArgs(j) == null) 
                context = ctx.type;
             type = checkFunctCall(name,type,ctx.start.getLine(),context,args);
          }
          if (!(type.equals(LuaType._Bottom))) {
             if ( !tvs.equals(LuaType._Bottom) ) {
               type = LuaType.expand(s2,type);
               if (type instanceof VarType ||
                   type instanceof TableType) {
                  VarType tv = (VarType) LuaAnalyser.mkNewGenVarType();
                  TableType ntab = new TableType(tvs, tv);
                  Scope stmp = type.unifiesWith(s2,ntab,verb);
                  s2.mergeWith(stmp); 
                  if (type instanceof TableType &&
                      ((TableType) type).size() == 0)
                     type = ntab;
               }
             }
             type = LuaType.expand(s2,type);
             if ( (tvs.equals(LuaType._Bottom)) ||
                  (!(LuaType.isSelectable(type))) ) {
                type = LuaType._Bottom;
             } else { // Case
                type = type.select(s2,tvs,verb);
                if (type.equals(LuaType._Bottom))
                   type = LuaType._Nil;
                else {
                   List<LuaType> ext = new ArrayList<LuaType>();
                   ext.add(type);
                   ext.add(LuaType._Nil);
                   type = new UnionType (new SequenceType (ext));
                }
                LuaAnalyser.reportRule("Case");
             }
          }
       }

       // FApp or PDFApp
       Scope s3 = ctx.type.unifiesWith(s2,type,verb);
       if (s3 != null) {
          s0.mergeWith(s3);
          LuaAnalyser.reportRule("FApp | PDFApp");
       } else {
          boolean fail = true;
          if (vnat && !vsfix && strict == 0) { 
              Symbol symb = s0.resolveName(name);
              if (symb != null) {
                 fail = false;
                 symb.setType(LuaType._Nil);
              }
          } 
          if (fail) 
             error(ctx.start.getLine(),"The types in expression '"+
                                       ctx.getText()+"' do not unify");
       }
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterPowerOpExp(HpcLuaParser.PowerOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitPowerOpExp(HpcLuaParser.PowerOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       checkBinOp(ctx.operatorPower().getText(), ctx.start.getLine(),
                  ctx.type, ctx.exp(0).type, ctx.exp(1).type);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterUnaryOpExp(HpcLuaParser.UnaryOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitUnaryOpExp(HpcLuaParser.UnaryOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       checkUnOp(ctx.operatorUnary().getText(), ctx.start.getLine(),
                 ctx.type, ctx.exp().type);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterMulDivModOpExp(HpcLuaParser.MulDivModOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitMulDivModOpExp(HpcLuaParser.MulDivModOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       checkBinOp(ctx.operatorMulDivMod().getText(), ctx.start.getLine(),
                  ctx.type, ctx.exp(0).type, ctx.exp(1).type);
       reportLeave(new Object(){}, ctx);
    }

    public void enterAddSubOpExp(HpcLuaParser.AddSubOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitAddSubOpExp(HpcLuaParser.AddSubOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       checkBinOp(ctx.operatorAddSub().getText(), ctx.start.getLine(),
                  ctx.type, ctx.exp(0).type, ctx.exp(1).type);
       reportLeave(new Object(){}, ctx);
    }

    public void enterStrCatOpExp(HpcLuaParser.StrCatOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitStrCatOpExp(HpcLuaParser.StrCatOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       checkBinOp(ctx.operatorStrCat().getText(), ctx.start.getLine(),
                  ctx.type, ctx.exp(0).type, ctx.exp(1).type);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterCompOpExp(HpcLuaParser.CompOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitCompOpExp(HpcLuaParser.CompOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       checkBinOp(ctx.operatorComparison().getText(), ctx.start.getLine(),
                  ctx.type, ctx.exp(0).type, ctx.exp(1).type);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterAndOpExp(HpcLuaParser.AndOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitAndOpExp(HpcLuaParser.AndOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       checkBinOp(ctx.operatorAnd().getText(), ctx.start.getLine(),
                  ctx.type, ctx.exp(0).type, ctx.exp(1).type);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterOrOpExp(HpcLuaParser.OrOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitOrOpExp(HpcLuaParser.OrOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       checkBinOp(ctx.operatorOr().getText(), ctx.start.getLine(),
                  ctx.type, ctx.exp(0).type, ctx.exp(1).type);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterBitOpExp(HpcLuaParser.BitOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitBitOpExp(HpcLuaParser.BitOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       checkBinOp(ctx.operatorBitwise().getText(), ctx.start.getLine(),
                  ctx.type, ctx.exp(0).type, ctx.exp(1).type);
       reportLeave(new Object(){}, ctx);
    }

    public void enterTypeAscrExp(HpcLuaParser.TypeAscrExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitTypeAscrExp(HpcLuaParser.TypeAscrExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       if (ctx.typeAscr() != null) { // TypeAscr
          Scope s = (ctx.e.type).unifiesWith(this.getCurrentScope(),
                                    ctx.typeAscr().type,verb);
          if (s != null) {
             this.getCurrentScope().mergeWith(s);
             LuaAnalyser.reportRule("TypeAscr");
          } else
             error(ctx.start.getLine(),"The types in expression '"+
                ctx.getText()+"' do not unify");
       } 
       reportLeave(new Object(){}, ctx);
    }
  
    public void enterPrefixExp(HpcLuaParser.PrefixExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitPrefixExp(HpcLuaParser.PrefixExpContext ctx) {
       reportReach(new Object(){}, ctx);
       HpcLuaParser.VarOrExpContext vOectx = ctx.varOrExp();
       String name = null;
       if (vOectx.var != null) {
          HpcLuaParser.VarNameAndTypeContext vnatctx = 
             vOectx.var.varNameAndType();
          name = vnatctx.varName().NAME().getText();
       }
       LuaType type = vOectx.type;
       if (ctx.nameAndArgs(0) == null) { // FApp or PDFApp
          Scope s = ctx.type.unifiesWith(this.getCurrentScope(),type,verb);
          if (s != null) {
             this.getCurrentScope().mergeWith(s);
             LuaAnalyser.reportRule("FApp | PDFApp");
          } else
             error(ctx.start.getLine(),"The types in expression '"+
                                       ctx.getText()+"' do not unify");
       } else {
          int i = 0;
          LuaType context = LuaType._Any;
          while (ctx.nameAndArgs(i) != null) {
             HpcLuaParser.ArgsContext args = ctx.nameAndArgs(i++).args();
             if (ctx.nameAndArgs(i) == null) 
                context = ctx.type;
             type = checkFunctCall(name,type, ctx.start.getLine(), 
                                   context, args);
          }
       }
       reportLeave(new Object(){}, ctx);
    }

    public void enterFunctionCall(HpcLuaParser.FunctionCallContext ctx) {
       reportReach(new Object(){}, ctx);
       HpcLuaParser.VarOrExpContext vOectx = ctx.varOrExp();
       if (vOectx != null ) {
          HpcLuaParser.VarContext varctx = vOectx.var();
          if (varctx != null ) {
             HpcLuaParser.VarNameAndTypeContext varNaTctx = 
                                                   varctx.varNameAndType();
             if (varNaTctx != null) {  
                String functName = varNaTctx.varName().NAME().getText();
                Symbol funct = this.getCurrentScope().resolveName(functName);
                if ( funct == null ) 
                   error(varNaTctx.varName().NAME().getSymbol(),
                         "No function '"+functName+"' has been defined");
             }
          }
       }
       reportLeave(new Object(){}, ctx);
    }

    public void exitFunctionCall(HpcLuaParser.FunctionCallContext ctx) {
       reportReach(new Object(){}, ctx);
       String functName = null;
       HpcLuaParser.VarOrExpContext vOectx = ctx.varOrExp();
       LuaType functtype = null;
       HpcLuaParser.VarContext varctx = vOectx.var();
       if (varctx != null ) {
          HpcLuaParser.VarNameAndTypeContext varNaTctx = 
             varctx.varNameAndType();
          if (varNaTctx != null) {  
             functName = varNaTctx.varName().NAME().getText();
             Symbol funct = this.getCurrentScope().resolveName(functName);
             if (funct != null) 
                functtype = (LuaType) funct.getType();
             else
                new Throwable().printStackTrace();
           } else
              functtype = vOectx.type;
        } else
           functtype = vOectx.type;
        int i = 0;
        while (ctx.nameAndArgs(i) != null) {
           HpcLuaParser.ArgsContext args = ctx.nameAndArgs(i++).args();
           functtype = checkFunctCall(functName,functtype, ctx.start.getLine(), 
                                      LuaType._Any, args);
        }
       reportLeave(new Object(){}, ctx);
    }

    public void enterFunctDef(HpcLuaParser.FunctDefContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(analyser.getSymbolTable().getScopes().get(ctx));
       reportLeave(new Object(){}, ctx);
    }

    public void exitFunctDef(HpcLuaParser.FunctDefContext ctx) { 
       reportReach(new Object(){}, ctx);
       String funcName = ((FunctionSymbol) this.getCurrentScope()).getName();
       checkFunctDef(funcName, ctx.start.getLine(), ctx.functBody());
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterParList(HpcLuaParser.ParListContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    } 

    public void exitParList(HpcLuaParser.ParListContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    } 

    public void enterVarName(HpcLuaParser.VarNameContext ctx) {
       reportReach(new Object(){}, ctx, ctx.NAME().getSymbol());
       String name = ctx.NAME().getText();
       Symbol symb = this.getCurrentScope().resolveName(name);
       if ( symb == null ) {
           if (strict == 0) { 
              this.getCurrentScope().defineName( 
                 new VariableSymbol(name, LuaType._Nil), verb);
           } else
              error(ctx.NAME().getSymbol(), "No variable '"+name+
                                            "' has been defined");
       } else {
          LuaType type = (LuaType) symb.getType();
          if (type != null) {
             if ( type instanceof VarType ) { 
                Symbol typeSymb = this.getCurrentScope().resolveName(
                                     ((VarType) type).getVarName());
                if (typeSymb == null) { 
                   error(ctx.NAME().getSymbol(),"The type variable '"+name+
                         "' defined on line " +symb.getLine()+
                         " was used as a program variable");
                }
             } else 
                if (type.equals(LuaType._Bottom))
                   error(ctx.NAME().getSymbol(),"The label '"+name+
                         "' defined on line " + symb.getLine() +
                         " is used as a program variable");
          } else
            new Throwable().printStackTrace();
       }
       reportLeave(new Object(){}, ctx);
    }    

    public void exitVarName(HpcLuaParser.VarNameContext ctx) {
       reportReach(new Object(){}, ctx);
       Symbol symb = this.getCurrentScope().resolveName(ctx.NAME().getText());
       if (symb != null) { 
          LuaType type = (LuaType) symb.getType();
          if ((!ctx.type.equals(type))) {
             boolean builtIn = false;
             if (symb instanceof BuiltInSymbol) { 
                // Abs
                type = LuaAnalyser.unbindVariables(LuaType.copy(type),verb); 
                LuaAnalyser.reportRule("Abs");
                builtIn = true;
             }
             Scope s = ctx.type.unifiesWith(this.getCurrentScope(),type,verb);
             if (s != null) { // FApp or PDFApp
                this.getCurrentScope().mergeWith(s); 
                if (builtIn)
                   LuaAnalyser.reportRule("PDFApp");
                else 
                   LuaAnalyser.reportRule("FApp");
             } else 
                error(ctx.start.getLine(),"The types in expression '"+
                         ctx.getText()+"' do not unify"); 
          }
       } 
       reportLeave(new Object(){}, ctx);
    }

    public void enterVarNameAndType(HpcLuaParser.VarNameAndTypeContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitVarNameAndType(HpcLuaParser.VarNameAndTypeContext ctx) { 
       reportReach(new Object(){}, ctx);
       if (ctx.typeAscr() != null) { // TypeAscr
          Scope s = (ctx.varName().type).unifiesWith(this.getCurrentScope(), 
                                            ctx.typeAscr().type, 
                                            verb);
          if (s != null) {
             this.getCurrentScope().mergeWith(s);
             LuaAnalyser.reportRule("TypeAscr");
          } else
             error(ctx.start.getLine(),"The types in expression '"+
                ctx.getText()+"' do not unify");
       }
       reportLeave(new Object(){}, ctx);
    }
}
