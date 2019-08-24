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
import org.antlr.v4.runtime.tree.ParseTreeProperty;

public class DefPhase extends HpcLuaBaseListener {
    LuaAnalyser analyser;    
    Scope currentScope; 

    public DefPhase(LuaAnalyser a) {
        this.analyser = a;
    }
    
    public void error(Token t, String m) {
       analyser.reportError(t.getLine(), t.getCharPositionInLine(), m);
    }

    void saveScope(ParserRuleContext ctx, Scope s) { 
       analyser.getSymbolTable().getScopes().put(ctx,s); 
    }

    public void enterChunk(HpcLuaParser.ChunkContext ctx) {
        currentScope = analyser.getSymbolTable().getGlobals();
    }
	
    public void exitChunk(HpcLuaParser.ChunkContext ctx) {
    }

    public void enterBlock(HpcLuaParser.BlockContext ctx) {
        currentScope = new LocalScope(currentScope);
        saveScope(ctx, currentScope);
    }

    public void exitBlock(HpcLuaParser.BlockContext ctx) {
        currentScope = currentScope.getEnclosingScope(); 
    }

    public void exitVarType(HpcLuaParser.VarTypeContext ctx) {
        defineTVar(currentScope, ctx.NAME().getText(), 
                   ctx.NAME().getSymbol().getLine());
    }
    
    public void exitGlobalVarAssignStat(HpcLuaParser.GlobalVarAssignStatContext 
                                           ctx) {
       defineVars(analyser.getSymbolTable().getGlobals(), ctx.varList());
    }
    
    public void exitLabelStat(HpcLuaParser.LabelStatContext ctx) {
       Scope scope = analyser.getSymbolTable().getGlobals();
       String name = ctx.label().NAME().getText();
       Symbol previous = scope.resolveName(name);
       if (previous == null) {
          int line = ctx.label().NAME().getSymbol().getLine();
          scope.defineName(new Symbol(name, LuaType._Bottom, line));
       }
    }

    public void enterForLocalNameStat(HpcLuaParser.ForLocalNameStatContext ctx) {
       currentScope = new LocalScope(currentScope);
       saveScope(ctx, currentScope);
       defineVar(currentScope, ctx.varNameAndType());
    }
    
    public void exitForLocalNameStat(HpcLuaParser.ForLocalNameStatContext ctx) {
        currentScope = currentScope.getEnclosingScope(); 
    }
    
    public void enterForLocalExprStat(HpcLuaParser.ForLocalExpStatContext ctx) {
        currentScope = new LocalScope(currentScope);
        saveScope(ctx, currentScope);
        defineNames(currentScope, ctx.nameList());
    }

    public void exitForLocalExprStat(HpcLuaParser.ForLocalExpStatContext ctx) {
        currentScope = currentScope.getEnclosingScope(); 
    }
    
    public void enterFunctStat(HpcLuaParser.FunctStatContext ctx) {
        String name = ctx.functName().varName(0).NAME().getText();
        int line = ctx.functName().varName(0).NAME().getSymbol().getLine();
        LuaType type = LuaAnalyser.unbindVariables(
                          LuaType.copy(
                             LuaTypeSystem._genericFunctionType));
        FunctionSymbol function = new FunctionSymbol(name, type, line, 
                                                     currentScope);
        analyser.getSymbolTable().getGlobals().defineName(function); 
        currentScope = function;  
        saveScope(ctx, currentScope);  

        type = ((FunctionType) type).getRetTypes();
        currentScope.defineName(new VariableSymbol(analyser.genRetVarName(name), 
                                                   type));
    }

    public void exitFunctStat(HpcLuaParser.FunctStatContext ctx) {
        currentScope = currentScope.getEnclosingScope(); 
    }

    public void enterLocalFunctStat(HpcLuaParser.LocalFunctStatContext ctx) {
        String name = ctx.varName().NAME().getText();
        int line = ctx.varName().NAME().getSymbol().getLine();
        LuaType type = LuaAnalyser.unbindVariables(
                          LuaType.copy(
                             LuaTypeSystem._genericFunctionType));
        FunctionSymbol function = new FunctionSymbol(name, type, line, 
                                                     currentScope);
        currentScope.defineName(function); 
        currentScope = function;
        saveScope(ctx, currentScope);

        type = ((FunctionType) type).getRetTypes();
        currentScope.defineName(new VariableSymbol(analyser.genRetVarName(name),
                                                   type));
    }

    public void exitLocalFunctStat(HpcLuaParser.LocalFunctStatContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    public void exitLocalVarAssignStat(HpcLuaParser.LocalVarAssignStatContext 
                                          ctx) {
       defineNames(currentScope, ctx.nameList());
    }

    public void exitParList(HpcLuaParser.ParListContext ctx) {
       defineNames(currentScope, ctx.nameList());
    } 

    public void enterFunctDef(HpcLuaParser.FunctDefContext ctx) {
        String name = LuaAnalyser.genVarName();
        int line = ctx.start.getLine();
        LuaType type = ctx.type;
        FunctionSymbol function = new FunctionSymbol(name, type, line, 
                                                     currentScope);
        currentScope.defineName(function); 
        currentScope = function;
        saveScope(ctx, currentScope);

        type = ((FunctionType) type).getRetTypes();
        currentScope.defineName(new VariableSymbol(analyser.genRetVarName(name),
                                                   type));
    }

    public void exitFunctDef(HpcLuaParser.FunctDefContext ctx) {
        currentScope = currentScope.getEnclosingScope(); 
    }

    public void exitFunctCall(HpcLuaParser.FunctCallContext ctx) {
       if (ctx.type instanceof VarType) {
          Scope scope = analyser.getSymbolTable().getGlobals();
          String name = ctx.type.getVarName();
          defineTVar(scope, name, 0);
       } else  
          new Throwable().printStackTrace();
    }
       
    public void exitVar(HpcLuaParser.VarContext ctx) {
       if (ctx.type instanceof VarType) {
          Scope scope = analyser.getSymbolTable().getGlobals();
          String name = ctx.type.getVarName();
          defineTVar(scope, name, 0);
       } else  
          new Throwable().printStackTrace();
    }
       
    public void exitVarName(HpcLuaParser.VarNameContext ctx) {
       Symbol symb = currentScope.resolveName(ctx.NAME().getText());
       if (symb != null && ctx.type instanceof VarType) {
          LuaType type = (LuaType) symb.getType();
          if (type != null && type instanceof VarType && 
              type.equals(VarType._Unknown))
             error(ctx.NAME().getSymbol(),"The type variable "+
                symb.getName() + " defined in line " + symb.getLine()+
                " was used as a program variable");

          else { 
             Scope scope = analyser.getSymbolTable().getGlobals();
             String name = ctx.type.getVarName();
             defineTVar(scope, name, 0);
          }
       } 
    }
    
    public void defineVars(Scope focusScope, HpcLuaParser.VarListContext ctx) {
       HpcLuaParser.VarListContext varList = ctx;
       while (varList != null) {
          if (varList.var().varNameAndType() != null)
             defineVar(focusScope, varList.var().varNameAndType());
          varList = varList.varList();
       }
    }
    
    public void defineNames(Scope focusScope, HpcLuaParser.NameListContext ctx) {
       HpcLuaParser.NameListContext nameList = ctx;
       while (nameList != null) {
          defineVar(focusScope, nameList.varNameAndType());
          nameList = nameList.nameList();
       }
    }
    
    public void defineVar(Scope focusScope, HpcLuaParser.VarNameAndTypeContext 
                                               ctx) {
        String name = ctx.varName().NAME().getText();
        Symbol previous = focusScope.resolveName(name);
        boolean override = 
           (ctx.parent instanceof HpcLuaParser.NameListContext) || 
           (ctx.parent instanceof HpcLuaParser.ForLocalExpStatContext);
        if (previous == null || override) {
           int line = ctx.varName().NAME().getSymbol().getLine();
           LuaType type = ctx.varName().type;
           focusScope.defineName(new VariableSymbol(name, type, line));
           if (type instanceof VarType) {
              Scope scope = analyser.getSymbolTable().getGlobals();
              defineTVar(scope, ((VarType) type).getVarName(), line);
           } else  
             new Throwable().printStackTrace();
        } 
    }    
    
    public void defineTVar(Scope focusScope, String name, Integer line) {
        Symbol previous = focusScope.resolveName(name);
        if (previous == null) 
           focusScope.defineName(new VariableSymbol(name, VarType._Unknown, 
                                                    line));
    } 
}
