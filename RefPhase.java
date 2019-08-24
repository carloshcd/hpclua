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
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.Token;
import java.util.List;

public class RefPhase extends HpcLuaBaseListener {
    LuaAnalyser analyser; 
    Scope currentScope; 

    public RefPhase(LuaAnalyser a) {
        this.analyser = a;
    }
        
    public void error(Token t, String m) {
       analyser.reportError(t.getLine(), t.getCharPositionInLine(), m);
    }   

    public void error(int l, String m) {
       analyser.reportError(l, m);
    } 
    
    public void checkUnOp(String name, int line, LuaType t1) {
       Symbol op = currentScope.resolveName(name);
       if (op != null) {
          LuaType dt = (LuaType) op.getType();
          if (dt.freeTVars().size() > 0) 
             dt = LuaAnalyser.unbindVariables(LuaType.copy(dt));
          if (dt instanceof FunctionType) { 
             boolean fail = true;
             SequenceType paramtypes = ((FunctionType) dt).getParamTypes();
             LuaType paramtype = paramtypes.getElements().get(0);
             LuaType argtype = t1; 
             Scope s1 = paramtype.unifiesWith(currentScope, argtype);
             if (s1 != null) {
                currentScope.mergeWith(s1);
                fail = false;
             }
             if (fail)
                error(line, "The types in an expression do not unify");
          } else
             new Throwable().printStackTrace();
       } else
          new Throwable().printStackTrace();
    }

    public void checkBinOp(String name, int line, LuaType t1, LuaType t2) {
       Symbol op = currentScope.resolveName(name);
       if (op != null) {
          LuaType dt = (LuaType) op.getType();
          if (dt.freeTVars().size() > 0)
             dt = LuaAnalyser.unbindVariables(LuaType.copy(dt));
          if (dt instanceof FunctionType) { 
             boolean fail = true;
             SequenceType paramtypes = ((FunctionType) dt).getParamTypes();
             LuaType paramtype = paramtypes.getElements().get(0);
             LuaType argtype = t1; 
             Scope s1 = argtype.unifiesWith(currentScope, paramtype);
             if (s1 != null) {
                paramtype = paramtypes.getElements().get(1);
                argtype = t2;
                Scope s2 = paramtype.unifiesWith(s1, argtype);
                if (s2 != null) {
                   currentScope.mergeWith(s2);
                   fail = false;
                }
             }
             if (fail)
                error(line, "The types in an expression do not unify");
          } 
       } else
             new Throwable().printStackTrace();
    }

    public void checkFunct(String name, int line, 
                           HpcLuaParser.FunctBodyContext ctx) {
       Symbol funct = currentScope.resolveName(name);
       if (funct != null) {
          Scope s1 = null;
          boolean fail = true;

          LuaType functtype = (LuaType) funct.getType();
          LuaType paramtype = ((FunctionType) functtype).getParamTypes();
          LuaType rettype   = ((FunctionType) functtype).getRetTypes();

          LuaType argtype = LuaType._Bottom; 
          if (ctx.parList() != null)
             argtype = ctx.parList().type;
          if (!(argtype instanceof SequenceType))
             argtype = new SequenceType (argtype);
          if (((SequenceType) paramtype).getElements().size() == 1 &&
              ((SequenceType) argtype).getElements().size() != 1)
             paramtype = ((SequenceType) paramtype).getElements().get(0);
          s1 = argtype.unifiesWith(currentScope, paramtype);
          if (s1 != null) {
             argtype = LuaType._Any;
             if (ctx.anyTypeCast() != null) {
                argtype = ctx.anyTypeCast().anyObjectType().type;
             }
             if (!(argtype instanceof SequenceType))
                argtype = new SequenceType (argtype);
             Scope s2 = rettype.unifiesWith(s1, argtype);
             if (s2 != null) {
                currentScope.mergeWith(s2);
                fail = false;
             }
          }
          if (fail)
             error(line, "The types in a statement do not unify");
          else {
             functtype = ((FunctionType) 
                            (functtype.expand(currentScope))).bindFree();
             funct.setType(functtype);
          }
       } else
          new Throwable().printStackTrace();
    }

    public void checkFunctCall(String name, int line, LuaType vartype,
                               HpcLuaParser.ArgsContext ctx) {
       Symbol funct = currentScope.resolveName(name);
       if (funct != null) {
          Scope s1 = null;
          boolean fail = true;
          
          LuaType functtype = (LuaType) funct.getType();
          if (functtype instanceof QuantifiedType)
              functtype = LuaAnalyser.unbindVariables(LuaType.copy(functtype));
          if (!(functtype instanceof FunctionType)) {
             LuaType type = LuaAnalyser.unbindVariables(
                               LuaType.copy(
                                  LuaTypeSystem._genericFunctionType));
             s1 = functtype.unifiesWith(currentScope, type);
             if (s1 != null) {
                currentScope.mergeWith(s1);
                String varname = ((VarType) functtype).getVarName(); 
                functtype = type;
                type = ((FunctionType) type).getRetTypes();
                currentScope.defineName(
                   new VariableSymbol(analyser.genRetVarName(varname),
                                      type));

             } else
                error(line, "The variable "+name+" is not a function");
          }
          LuaType paramtype = ((FunctionType) functtype).getParamTypes();
          LuaType rettype   = ((FunctionType) functtype).getRetTypes();
          
          LuaType argtype = ctx.type; 
          if (!(argtype instanceof SequenceType))
             argtype = new SequenceType (argtype);
          if (((SequenceType) paramtype).getElements().size() == 1 &&
              ((SequenceType) argtype).getElements().size() != 1)
             paramtype = ((SequenceType) paramtype).getElements().get(0);
          s1 = argtype.unifiesWith(currentScope, paramtype);
          if (s1 != null) {
             argtype = vartype;
             if (!(argtype instanceof SequenceType))
                argtype = new SequenceType (argtype);
             Scope s2 = rettype.unifiesWith(s1, argtype);
             if (s2 != null) {
                currentScope.mergeWith(s2);
                fail = false;
             }
          }
          if (fail)
             error(line, "The types in the function call do not unify");
       } else
          new Throwable().printStackTrace();
    }

                           
    public void enterChunk(HpcLuaParser.ChunkContext ctx) {
        currentScope = analyser.getSymbolTable().getGlobals();
    }
    
    public void exitChunk(HpcLuaParser.ChunkContext ctx) {
    }

    public void enterBlock(HpcLuaParser.BlockContext ctx) {
        currentScope = analyser.getSymbolTable().getScopes().get(ctx);
    }
    
    // Block 
    public void exitBlock(HpcLuaParser.BlockContext ctx) { 
        currentScope = currentScope.getEnclosingScope();
        // do nothing
    }
    
    public void enterVarType(HpcLuaParser.VarTypeContext ctx) {
       String name = ctx.NAME().getText();
       Symbol var = currentScope.resolveName(name);
       if ( var!=null ) {
          if (var.getType() != null && var.getType().equals(LuaType._Bottom))
             error(ctx.NAME().getSymbol(),"The label "+name+
                                          " was used as a type variable");
          else {
             LuaType type = (LuaType) var.getType();
             if (type instanceof VarType) 
                if (LuaAnalyser.isGenVarName(((VarType) type).getVarName()))
                   error(ctx.NAME().getSymbol(),"The program variable "+name+
                                                " defined in line "+
                                                var.getLine()+
                                                " was used as a type variable");
          }
       } else
          new Throwable().printStackTrace();
    }

    // RetStat
    public void exitRetStat(HpcLuaParser.RetStatContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       Symbol encFunct = currentScope.getEnclosingFunction();
       if (encFunct != null) {
          String name = analyser.genRetVarName(encFunct.getName());
          Symbol var = currentScope.resolveName(name);
          if (var != null) {
             LuaType functtype = (LuaType) var.getType();
             if (functtype != null) {
                SequenceType rettype = new SequenceType (LuaType._Nil); 
                if (ctx.expList() != null)
                   rettype = ctx.expList().type;
                if (functtype instanceof SequenceType) {
                   if (((SequenceType) functtype).getElements().size() == 1 &&
                       rettype.getElements().size() != 1)
                      functtype = ((SequenceType) functtype).getElements().get(0);
                }
                Scope s = functtype.unifiesWith(currentScope, rettype);
                if (s != null) 
                   currentScope.mergeWith(s);
                else
                   error(ctx.start.getLine(),
                         "The types in a statement do not unify");
             } // in the else case no type was declared for this function
          } else
             new Throwable().printStackTrace();
       } // in the else case the return value is ignored
    }
    
    // GlobAssign
    public void exitGlobalVarAssignStat(HpcLuaParser.GlobalVarAssignStatContext 
                                           ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       List<LuaType> varTypes = ctx.varList().type.getElements();
       List<LuaType> expTypes = ctx.expList().type.getElements();
       Scope s = LuaType.unifiesLists(analyser.getSymbolTable().getGlobals(), 
                                      varTypes, expTypes, false);
       if (s != null) 
          analyser.getSymbolTable().getGlobals().mergeWith(s);
       else
          error(ctx.start.getLine(), "The types in a statement do not unify");
    }
    
    public void enterFunctCallStat(HpcLuaParser.FunctCallStatContext ctx) {
        HpcLuaParser.VarOrExpContext vOectx = ctx.functCall().varOrExp();
        if (vOectx != null ) {
           HpcLuaParser.VarContext varctx = vOectx.var();
           if (varctx != null ) {
              HpcLuaParser.VarNameAndTypeContext varNaTctx = 
                                                    varctx.varNameAndType();
              if (varNaTctx != null) {  
                 String functName = varNaTctx.varName().NAME().getText();
                 Symbol funct = currentScope.resolveName(functName);
                 if ( funct==null )
                    error(varNaTctx.varName().NAME().getSymbol(),
                          "No such function "+functName);
               }
           }
        }
    }
    
    // FunctApp
    public void exitFunctCallStat(HpcLuaParser.FunctCallStatContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       HpcLuaParser.VarOrExpContext vOectx = ctx.functCall().varOrExp();
        if (vOectx != null ) {
           HpcLuaParser.VarContext varctx = vOectx.var();
           if (varctx != null ) {
              HpcLuaParser.VarNameAndTypeContext varNaTctx = 
                                                    varctx.varNameAndType();
              if (varNaTctx != null) {  
                 String functName = varNaTctx.varName().NAME().getText();
                 checkFunctCall(functName,ctx.start.getLine(), LuaType._Any,
                                ctx.functCall().nameAndArgs().args());
              }
           }
        }
    }
    
    public void enterLabelStat(HpcLuaParser.LabelStatContext ctx) { 
       String label = ctx.label().NAME().getText();
       Symbol place = currentScope.resolveName(label);
       if (place != null) {
          int previous = ctx.label().NAME().getSymbol().getLine();
          if (place.getLine() != previous)
             error(ctx.label().NAME().getSymbol(),"The label "+label+
                   " has already been defined in line "+place.getLine());
       }
    }

    // Break
    public void exitBreakStat(HpcLuaParser.BreakStatContext ctx) {
       // do nothing
    }
    
    public void enterGotoStat(HpcLuaParser.GotoStatContext ctx) { 
       String label = ctx.NAME().getText();
       Symbol place = currentScope.resolveName(label);
       if (place == null)
          error(ctx.NAME().getSymbol(),"No such label "+label);   
       else {
          LuaType type = (LuaType) place.getType();
          if (type instanceof VarType)
             error(ctx.NAME().getSymbol(),"Goto refers to a variable");   
       }
    }

    // Goto
    public void exitGotoStat(HpcLuaParser.GotoStatContext ctx) { 
       // do nothing
    }

    // Do 
    public void exitDoStat(HpcLuaParser.DoStatContext ctx) { 
       // do nothing
    }
    
    // While
    public void exitWhileStat(HpcLuaParser.WhileStatContext ctx) { 
       // do nothing
    }

    // Repeat
    public void exitRepeatStat(HpcLuaParser.RepeatStatContext ctx) { 
       // do nothing
    }

    // IfThenElse
    public void exitIfThenElseStat(HpcLuaParser.IfThenElseStatContext ctx) { 
       // do nothing
    }
    
    // For
    public void enterForLocalNameStat(HpcLuaParser.ForLocalNameStatContext ctx) {
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       currentScope = analyser.getSymbolTable().getScopes().get(ctx);
       boolean fail = true;
       Scope s1 = (ctx.varNameAndType().type).unifiesWith(currentScope, 
                                                          ctx.exp(0).type);
       if (s1 != null) {
          Scope s2 = (ctx.varNameAndType().type).unifiesWith(s1, ctx.exp(1).type);
          if (s2 != null) {
             if (ctx.exp(2) != null) {
                Scope s3 = (ctx.varNameAndType().type).unifiesWith(s2, 
                                                          ctx.exp(2).type);
                if (s3 != null) {
                   currentScope.mergeWith(s3);
                   fail = false;
                }
             } else {
                currentScope.mergeWith(s2);
                fail = false;
             }
          }
       }
       if (fail)
          error(ctx.start.getLine(),"The types in a statement do not unify");
    }
    
    public void exitForLocalNameStat(HpcLuaParser.ForLocalNameStatContext ctx) {     
       currentScope = currentScope.getEnclosingScope();
    }

    // For
    public void enterForLocalExpStat(HpcLuaParser.ForLocalExpStatContext ctx) {
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       currentScope = analyser.getSymbolTable().getScopes().get(ctx);
       List<LuaType> nameTypes = ctx.nameList().type.getElements();
       List<LuaType> expTypes = ctx.expList().type.getElements();
       Scope s1 = LuaType.unifiesLists(currentScope, nameTypes, expTypes, true);
       if (s1 != null) 
          currentScope.mergeWith(s1);
       else
          error(ctx.start.getLine(),"The types in a statement do not unify");
    }

    // For
    public void exitForLocalExpStat(HpcLuaParser.ForLocalExpStatContext ctx) { 
        currentScope = currentScope.getEnclosingScope();
    }

    public void enterFunctStat(HpcLuaParser.FunctStatContext ctx) { 
        currentScope = analyser.getSymbolTable().getScopes().get(ctx);
    }
    
    // FuncAbs
    public void exitFunctStat(HpcLuaParser.FunctStatContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       checkFunct(ctx.functName().varName(0).NAME().getText(),
                  ctx.start.getLine(), ctx.functBody());
       currentScope = currentScope.getEnclosingScope();
    }
	
    public void enterLocalFunctStat(HpcLuaParser.LocalFunctStatContext ctx) {
        currentScope = analyser.getSymbolTable().getScopes().get(ctx);
    }
    
    // FuncAbs
    public void exitLocalFunctStat(HpcLuaParser.LocalFunctStatContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       checkFunct(ctx.varName().NAME().getText(),
                  ctx.start.getLine(), ctx.functBody());
       currentScope = currentScope.getEnclosingScope();
    }

    // LocAssign
    public void exitLocalVarAssignStat(HpcLuaParser.LocalVarAssignStatContext 
                                          ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       List<LuaType> nameTypes = ctx.nameList().type.getElements();
       List<LuaType> expTypes = ctx.expList().type.getElements();
       Scope s = LuaType.unifiesLists(currentScope, nameTypes, expTypes, false);
       if (s != null) 
          currentScope.mergeWith(s);
       else
          error(ctx.start.getLine(),"The types in a statement do not unify");
    }
   
    // BOp 
    public void exitPowerOpExp(HpcLuaParser.PowerOpExpContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       checkBinOp(ctx.operatorPower().getText(), ctx.start.getLine(),
                  ctx.exp(0).type, ctx.exp(1).type);
    }
    
    // UOp 
    public void exitUnaryOpExp(HpcLuaParser.UnaryOpExpContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       checkUnOp(ctx.operatorUnary().getText(), ctx.start.getLine(),
                 ctx.exp().type);
    }
    
    // BOp 
    public void exitMulDivModOpExp(HpcLuaParser.MulDivModOpExpContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       checkBinOp(ctx.operatorMulDivMod().getText(), ctx.start.getLine(),
                  ctx.exp(0).type, ctx.exp(1).type);
    }

    // BOp 
    public void exitAddSubOpExp(HpcLuaParser.AddSubOpExpContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       checkBinOp(ctx.operatorAddSub().getText(), ctx.start.getLine(),
                  ctx.exp(0).type, ctx.exp(1).type);
    }

    // BOp 
    public void exitStrCatOpExp(HpcLuaParser.StrCatOpExpContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       checkBinOp(ctx.operatorStrCat().getText(), ctx.start.getLine(),
                  ctx.exp(0).type, ctx.exp(1).type);
    }
    
    // BOp 
    public void exitCompOpExp(HpcLuaParser.CompOpExpContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       checkBinOp(ctx.operatorComparison().getText(), ctx.start.getLine(),
                  ctx.exp(0).type, ctx.exp(1).type);
    }
    
    // BOp 
    public void exitAndOpExp(HpcLuaParser.AndOpExpContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       checkBinOp(ctx.operatorAnd().getText(), ctx.start.getLine(),
                  ctx.exp(0).type, ctx.exp(1).type);
    }
    
    // BOp 
    public void exitOrOpExp(HpcLuaParser.OrOpExpContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       checkBinOp(ctx.operatorOr().getText(), ctx.start.getLine(),
                  ctx.exp(0).type, ctx.exp(1).type);
    }
    
    // BOp 
    public void exitBitOpExp(HpcLuaParser.BitOpExpContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       checkBinOp(ctx.operatorBitwise().getText(), ctx.start.getLine(),
                  ctx.exp(0).type, ctx.exp(1).type);
    }

    // TypeAsrc   
    public void exitTypeCastExp(HpcLuaParser.TypeCastExpContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       if (ctx.typeCast() != null) {
          Scope s = (ctx.e.type).unifiesWith(currentScope, ctx.typeCast().type);
          if (s != null)
             currentScope.mergeWith(s);
          else
             error(ctx.start.getLine(),"The types in expression "+ctx.getText()+
                                       " do not unify");
       } 
    }
   
    public void enterFunctCall(HpcLuaParser.FunctCallContext ctx) {
        HpcLuaParser.VarOrExpContext vOectx = ctx.varOrExp();
        if (vOectx != null ) {
           HpcLuaParser.VarContext varctx = vOectx.var();
           if (varctx != null ) {
              HpcLuaParser.VarNameAndTypeContext varNaTctx = 
                                                    varctx.varNameAndType();
              if (varNaTctx != null) {  
                 String functName = varNaTctx.varName().NAME().getText();
                 Symbol funct = currentScope.resolveName(functName);
                 if ( funct==null ) 
                    error(varNaTctx.varName().NAME().getSymbol(),
                          "No such function "+functName);
              }
           }
        }
    }

    // FunctApp
    public void exitFunctCall(HpcLuaParser.FunctCallContext ctx) {
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       HpcLuaParser.VarOrExpContext vOectx = ctx.varOrExp();
       if (vOectx != null ) {
          HpcLuaParser.VarContext varctx = vOectx.var();
          if (varctx != null ) {
             HpcLuaParser.VarNameAndTypeContext varNaTctx = 
                                                   varctx.varNameAndType();
             if (varNaTctx != null) {  
               String functName = varNaTctx.varName().NAME().getText();
                checkFunctCall(functName,ctx.start.getLine(), ctx.type,
                               ctx.nameAndArgs().args());
             }
          }
       }
    }

    public void exitVar(HpcLuaParser.VarContext ctx) {
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       LuaType type = LuaType._Bottom;
       HpcLuaParser.VarNameAndTypeContext vnatctx = ctx.varNameAndType();
       HpcLuaParser.ExpContext ectx = ctx.exp();
       if (vnatctx != null) 
          type = vnatctx.type;
       else 
          type = ectx.type;
       int index = 0;
       while (ctx.varSuffix(index) != null) {
          if (type instanceof VarType) 
              type = LuaType.expand(currentScope, ((VarType) type).getVarName());
          HpcLuaParser.VarSuffixContext varSuffix = ctx.varSuffix(index++);
          if (!(type.equals(LuaType._Bottom))) {
             if (((varSuffix.type).equals(LuaType._Bottom)) ||
                 (!(type instanceof TableType))) 
                type = LuaType._Bottom;
             else // TabCase
                type = ((TableType) type).select(currentScope, varSuffix.type);
          }
       }
       Scope s = ctx.type.unifiesWith(currentScope, type);
       if (s != null)
          currentScope.mergeWith(s);
       else
          error(ctx.start.getLine(),"The types in expression "+ctx.getText()+
                                    " do not unify");
    }
 
    public void enterFunctDef(HpcLuaParser.FunctDefContext ctx) {
        currentScope = analyser.getSymbolTable().getScopes().get(ctx);
    }

    // FuncAbs
    public void exitFunctDef(HpcLuaParser.FunctDefContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       String name = ((FunctionSymbol) currentScope).getName();
       checkFunct(name, ctx.start.getLine(), ctx.functBody());
       currentScope = currentScope.getEnclosingScope();
    }

    public void enterVarName(HpcLuaParser.VarNameContext ctx) {
        String name = ctx.NAME().getText();
        Symbol symb = currentScope.resolveName(name);
        if ( symb == null )
            error(ctx.NAME().getSymbol(), "No such variable "+name);
        else {
           LuaType type = (LuaType) symb.getType();
           if (type != null) {
              if ( type instanceof VarType ) { 
                 Symbol typeSymb = currentScope.resolveName(
                                      ((VarType) type).getVarName());
                 if (typeSymb == null) { 
                       // check if you leave this here, since appears in DefPhase
                       error(ctx.NAME().getSymbol(),"The type variable "+name+
                             " defined in line " +symb.getLine()+
                             " was used as a program variable");
                 }
              } else 
                 if (type.equals(LuaType._Bottom))
                    error(ctx.NAME().getSymbol(),"The label "+name+
                          " defined in line " + symb.getLine() +
                          " was used as a program variable");
           } else
             new Throwable().printStackTrace();
        }
    }    

    public void exitVarName(HpcLuaParser.VarNameContext ctx) {
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       Symbol symb = currentScope.resolveName(ctx.NAME().getText());
       LuaType type = (LuaType) symb.getType();
       if (symb != null && (!ctx.type.equals(type))) {
          Scope s = ctx.type.unifiesWith(currentScope, type);
          if (s != null) 
             currentScope.mergeWith(s);
          else
             error(ctx.start.getLine(),"The types in expression "+ctx.getText()+
                                       " do not unify");
       }
    }

    // TypeAsrc
    public void exitVarNameAndType(HpcLuaParser.VarNameAndTypeContext ctx) { 
       System.out.printf("%s:%s\n",new Object(){}.getClass().getEnclosingMethod().getName(),ctx.getText());
       if (ctx.typeCast() != null) {
          Scope s = (ctx.varName().type).unifiesWith(currentScope, 
                                                     ctx.typeCast().type);
          if (s != null) 
             currentScope.mergeWith(s);
          else
             error(ctx.start.getLine(),"The types in expression "+ctx.getText()+
                                       " do not unify");
       }
    }
}
