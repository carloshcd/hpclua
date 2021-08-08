/***
  DefPhase.java

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
import org.antlr.v4.runtime.Token;

public class DefPhase extends HpcLuaFullListener {
    LuaAnalyser analyser;    
    int verb;

    public DefPhase(LuaAnalyser a) {
        super();
        this.analyser = a;
        this.verb = LuaAnalyser.getVerbosity();
    }
    
    public void error(Token t, String m) {
       analyser.reportError(t.getLine(), t.getCharPositionInLine(), m);
    }

    void saveScope(ParserRuleContext ctx, Scope s) { 
       analyser.getSymbolTable().getScopes().put(ctx,s); 
    }

    public void reportReach(Object o,
                            ParserRuleContext ctx) {
       if ( this.verb > 1) 
          System.out.printf("%s:Reaching %s line %d:%s\n", 
                             o.getClass().getName(),
                             o.getClass().getEnclosingMethod().getName(),
                             ctx.start.getLine(), ctx.getText());
    }

    public void reportReach(Object o,
                            ParserRuleContext ctx,
                            Token t) {
       if ( this.verb > 1) 
          System.out.printf("%s:Reaching %s line %d(%d):%s\n", 
                            o.getClass().getName(),
                            o.getClass().getEnclosingMethod().getName(),
                            ctx.start.getLine(), t.getCharPositionInLine(),
                            ctx.getText());
    }

    public void reportLeave(Object o,
                            ParserRuleContext ctx) {
       if ( this.verb > 1) 
          System.out.printf("%s:Leaving %s line %d:%s\n", 
                             o.getClass().getName(),
                             o.getClass().getEnclosingMethod().getName(),
                             ctx.start.getLine(), ctx.getText());
    }

    public void reportLeave(Object o,
                            ParserRuleContext ctx,
                            Token t) {
       if ( this.verb > 1) 
          System.out.printf("%s:Leaving %s line %d(%d):%s\n", 
                            o.getClass().getName(),
                            o.getClass().getEnclosingMethod().getName(),
                            ctx.start.getLine(), t.getCharPositionInLine(),
                            ctx.getText());
    }

    public void enterVarType(HpcLuaParser.VarTypeContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    } 

    public void exitVarType(HpcLuaParser.VarTypeContext ctx) {
       reportReach(new Object(){}, ctx);
       defineTVar(this.getCurrentScope(), ctx.NAME().getText(), 
                  ctx.NAME().getSymbol().getLine());
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
       this.setCurrentScope(new LocalScope(this.getCurrentScope()));
       saveScope(ctx, this.getCurrentScope());
       reportLeave(new Object(){}, ctx);
    }

    public void exitBlock(HpcLuaParser.BlockContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope()); 
       reportLeave(new Object(){}, ctx);
    }
 
    public void enterGAsgnStat(HpcLuaParser.GAsgnStatContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setReverseTraversal(null);
       reportLeave(new Object(){}, ctx);
    }

     public void exitGAsgnStat(HpcLuaParser.GAsgnStatContext ctx) {
       reportReach(new Object(){}, ctx);
       defineVars(analyser.getSymbolTable().getGlobals(),ctx.varList());
       reportLeave(new Object(){}, ctx);
    }

    public void enterFunctionCallStat(HpcLuaParser.FunctionCallStatContext 
                                                   ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

     public void exitFunctionCallStat(HpcLuaParser.FunctionCallStatContext 
                                                   ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterLabelStat(HpcLuaParser.LabelStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitLabelStat(HpcLuaParser.LabelStatContext ctx) {
       reportReach(new Object(){}, ctx);
       Scope scope = analyser.getSymbolTable().getGlobals();
       String name = ctx.label().NAME().getText();
       Symbol previous = scope.resolveName(name);
       if (previous == null) {
          int line = ctx.label().NAME().getSymbol().getLine();
          scope.defineName(new Symbol(name, LuaType._Bottom, line),verb);
       }
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterBreakStat(HpcLuaParser.BreakStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitBreakStat(HpcLuaParser.BreakStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterGotoStat(HpcLuaParser.GotoStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitGotoStat(HpcLuaParser.GotoStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void enterDoStat(HpcLuaParser.DoStatContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(new LocalScope(this.getCurrentScope()));
       saveScope(ctx, this.getCurrentScope());
       reportLeave(new Object(){}, ctx);
    }

    public void exitDoStat(HpcLuaParser.DoStatContext ctx) { 
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterWhileStat(HpcLuaParser.WhileStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitWhileStat(HpcLuaParser.WhileStatContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterRepeatStat(HpcLuaParser.RepeatStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitRepeatStat(HpcLuaParser.RepeatStatContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterIfStat(HpcLuaParser.IfStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitIfStat(HpcLuaParser.IfStatContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void enterForNumStat(HpcLuaParser.ForNumStatContext 
                                             ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(new LocalScope(this.getCurrentScope()));
       saveScope(ctx, this.getCurrentScope());
       defineVar(this.getCurrentScope(), ctx.varNameAndType());
       this.setSecondFirstTraversal(this.getCurrentScope());
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       reportLeave(new Object(){}, ctx);
    }
    
    public void exitForNumStat(HpcLuaParser.ForNumStatContext 
                                            ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterForGenStat(HpcLuaParser.ForGenStatContext 
                                             ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(new LocalScope(this.getCurrentScope()));
       saveScope(ctx, this.getCurrentScope());
       defineNames(this.getCurrentScope(), ctx.nameList());
       this.setSecondFirstTraversal(this.getCurrentScope());
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       reportLeave(new Object(){}, ctx);
    }

    public void exitForGenStat(HpcLuaParser.ForGenStatContext 
                                            ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterFunctStat(HpcLuaParser.FunctStatContext ctx) {
       reportReach(new Object(){}, ctx);
       String name = ctx.functName().varName(0).NAME().getText();
       int line = ctx.functName().varName(0).NAME().getSymbol().getLine();
       LuaType functtype = LuaAnalyser.mkNewUnbGenFunctType(); 
       FunctionSymbol function = new FunctionSymbol(name, functtype, line, 
                                                    this.getCurrentScope());
       analyser.getSymbolTable().getGlobals().defineName(function,verb); 
       this.setCurrentScope(function);  
       saveScope(ctx, this.getCurrentScope());  

       LuaType type = ((FunctionType) functtype).getRetTypes();
       this.getCurrentScope().defineName(new 
          VariableSymbol(analyser.genOutVarName(name), type), verb);
       type = ((FunctionType) functtype).getParamTypes();
       this.getCurrentScope().defineName(new
          VariableSymbol(analyser.genInVarName(name), type), verb);
       reportLeave(new Object(){}, ctx);
    }

    public void exitFunctStat(HpcLuaParser.FunctStatContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope()); 
       reportLeave(new Object(){}, ctx);
    }

    public void enterLocalFunctStat(HpcLuaParser.LocalFunctStatContext ctx) {
       reportReach(new Object(){}, ctx);
       String name = ctx.varName().NAME().getText();
       int line = ctx.varName().NAME().getSymbol().getLine();
       LuaType functtype = LuaAnalyser.mkNewUnbGenFunctType(); 
       FunctionSymbol function = new FunctionSymbol(name, functtype, line, 
                                                    this.getCurrentScope());
       this.getCurrentScope().defineName(function,verb); 
       this.setCurrentScope(function);
       saveScope(ctx, this.getCurrentScope());

       LuaType type = ((FunctionType) functtype).getRetTypes();
       this.getCurrentScope().defineName(new 
          VariableSymbol(analyser.genOutVarName(name), type), verb);
       type = ((FunctionType) functtype).getParamTypes();
       this.getCurrentScope().defineName(new
          VariableSymbol(analyser.genInVarName(name), type), verb);
       reportLeave(new Object(){}, ctx);
    }

    public void exitLocalFunctStat(HpcLuaParser.LocalFunctStatContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       reportLeave(new Object(){}, ctx);
    }

    public void enterLAsgnStat(HpcLuaParser.LAsgnStatContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setReverseTraversal(this.getCurrentScope());
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope());
       reportLeave(new Object(){}, ctx);
    }
    
    public void exitLAsgnStat(HpcLuaParser.LAsgnStatContext ctx) {
       reportReach(new Object(){}, ctx);
       defineNames(this.getCurrentScope(), ctx.nameList());
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterRetStat(HpcLuaParser.RetStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void exitRetStat(HpcLuaParser.RetStatContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterVar(HpcLuaParser.VarContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitVar(HpcLuaParser.VarContext ctx) {
       reportReach(new Object(){}, ctx);
       if (ctx.type instanceof VarType) {
          Scope scope = analyser.getSymbolTable().getGlobals();
          String name = ((VarType) ctx.type).getVarName();
          defineTVar(scope, name, 0);
       } else  
          new Throwable().printStackTrace();
       reportLeave(new Object(){}, ctx);
    }
      
   public void enterPowerOpExp(HpcLuaParser.PowerOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitPowerOpExp(HpcLuaParser.PowerOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterUnaryOpExp(HpcLuaParser.UnaryOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitUnaryOpExp(HpcLuaParser.UnaryOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterMulDivModOpExp(HpcLuaParser.MulDivModOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitMulDivModOpExp(HpcLuaParser.MulDivModOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void enterAddSubOpExp(HpcLuaParser.AddSubOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitAddSubOpExp(HpcLuaParser.AddSubOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void enterStrCatOpExp(HpcLuaParser.StrCatOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }

    public void exitStrCatOpExp(HpcLuaParser.StrCatOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterCompOpExp(HpcLuaParser.CompOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitCompOpExp(HpcLuaParser.CompOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterAndOpExp(HpcLuaParser.AndOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitAndOpExp(HpcLuaParser.AndOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterOrOpExp(HpcLuaParser.OrOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitOrOpExp(HpcLuaParser.OrOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
   
    public void enterBitOpExp(HpcLuaParser.BitOpExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitBitOpExp(HpcLuaParser.BitOpExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void enterTypeAscrExp(HpcLuaParser.TypeAscrExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitTypeAscrExp(HpcLuaParser.TypeAscrExpContext ctx) { 
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterPrefixExp(HpcLuaParser.PrefixExpContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void exitPrefixExp(HpcLuaParser.PrefixExpContext ctx) {
       reportReach(new Object(){}, ctx);
       if (ctx.type instanceof VarType) {
          Scope scope = analyser.getSymbolTable().getGlobals();
          String name = ((VarType) ctx.type).getVarName();
          defineTVar(scope, name, 0);
       } else  
          new Throwable().printStackTrace();
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterFunctionCall(HpcLuaParser.FunctionCallContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void exitFunctionCall(HpcLuaParser.FunctionCallContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void enterFunctDef(HpcLuaParser.FunctDefContext ctx) {
       reportReach(new Object(){}, ctx);
       String name = LuaAnalyser.genVarName();
       int line = ctx.start.getLine();
       LuaType functtype = ctx.type;
       FunctionSymbol function = new FunctionSymbol(name, functtype, line, 
                                                    this.getCurrentScope());
       this.getCurrentScope().defineName(function,verb); 
       this.setCurrentScope(function);
       saveScope(ctx, this.getCurrentScope());

       LuaType type = ((FunctionType) functtype).getRetTypes();
       this.getCurrentScope().defineName(new 
          VariableSymbol(analyser.genOutVarName(name), type), verb);
       type = ((FunctionType) functtype).getParamTypes();
       this.getCurrentScope().defineName(new
          VariableSymbol(analyser.genInVarName(name), type), verb);
       reportLeave(new Object(){}, ctx);
    }

    public void exitFunctDef(HpcLuaParser.FunctDefContext ctx) {
       reportReach(new Object(){}, ctx);
       this.setCurrentScope(this.getCurrentScope().getEnclosingScope()); 
       reportLeave(new Object(){}, ctx);
    }

    public void enterParList(HpcLuaParser.ParListContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    } 

    public void exitParList(HpcLuaParser.ParListContext ctx) {
       reportReach(new Object(){}, ctx);
       defineNames(this.getCurrentScope(), ctx.nameList());
       reportLeave(new Object(){}, ctx);
    } 

    public void enterVarName(HpcLuaParser.VarNameContext ctx) {
       reportReach(new Object(){}, ctx, ctx.NAME().getSymbol());
       reportLeave(new Object(){}, ctx, ctx.NAME().getSymbol());
    }
 
    public void exitVarName(HpcLuaParser.VarNameContext ctx) {
       reportReach(new Object(){}, ctx, ctx.NAME().getSymbol());
       Symbol symb = this.getCurrentScope().resolveName(ctx.NAME().getText());
       boolean pvtverr = false;
       if (symb != null && ctx.type instanceof VarType) {
          LuaType type = (LuaType) symb.getType();
          if ((type != null) && (type instanceof VarType) && 
              type.equals(VarType._Unknown)) {
             pvtverr = true;
             error(ctx.NAME().getSymbol(),"The type variable '"+
                symb.getName() + "' defined on line " + symb.getLine()+
                " was used as a program variable");
          } 
       }
       if (!pvtverr) { 
          Scope scope = analyser.getSymbolTable().getGlobals();
          String name = ((VarType) ctx.type).getVarName();
          defineTVar(scope, name, 0);
       } 
       reportLeave(new Object(){}, ctx, ctx.NAME().getSymbol());
    }
 
    public void enterVarNameAndType(HpcLuaParser.VarNameAndTypeContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
    
    public void exitVarNameAndType(HpcLuaParser.VarNameAndTypeContext ctx) {
       reportReach(new Object(){}, ctx);
       reportLeave(new Object(){}, ctx);
    }
 
    public void defineVars(Scope focusScope, 
                           HpcLuaParser.VarListContext ctx) {
       reportReach(new Object(){}, ctx);
       HpcLuaParser.VarListContext varList = ctx;
       while (varList != null) {
          if (varList.var().varNameAndType() != null)
             defineVar(focusScope, varList.var().varNameAndType());
          varList = varList.varList();
       }
       reportLeave(new Object(){}, ctx);
    }
    
    public void defineNames(Scope focusScope, 
                            HpcLuaParser.NameListContext ctx) {
       reportReach(new Object(){}, ctx);
       HpcLuaParser.NameListContext nameList = ctx;
       while (nameList != null) {
          defineVar(focusScope, nameList.varNameAndType());
          nameList = nameList.nameList();
       }
       reportLeave(new Object(){}, ctx);
    }
    
    public void defineVar(Scope focusScope, 
                          HpcLuaParser.VarNameAndTypeContext ctx) {
       reportReach(new Object(){}, ctx);
       String name = ctx.varName().NAME().getText();
       Symbol previous = focusScope.resolveName(name);
       boolean override = 
           (ctx.parent instanceof HpcLuaParser.NameListContext) || 
           (ctx.parent instanceof HpcLuaParser.ForNumStatContext);
       if (previous == null || override) {
          int line = ctx.varName().NAME().getSymbol().getLine();
          LuaType type = ctx.varName().type;
          focusScope.defineName(new VariableSymbol(name, type, line),verb);
          if (type instanceof VarType) {
             Scope scope = analyser.getSymbolTable().getGlobals();
             defineTVar(scope, ((VarType) type).getVarName(), line);
          } else  
             new Throwable().printStackTrace();
       } 
       reportLeave(new Object(){}, ctx);
    }    
    
    public void defineTVar(Scope focusScope, String name, Integer line) {
       Symbol previous = focusScope.resolveName(name);
       if (previous == null) 
          focusScope.defineName(new 
             VariableSymbol(name, VarType._Unknown, line),verb);
    } 
}
