/***
 TreePrinterListeners.java

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;

public class TreePrinterListener implements ParseTreeListener {
    SymbolTable symtab;
    Scope currentScope;

    private final List<String> ruleNames;
    private final StringBuilder builder = new StringBuilder();
    Map<RuleContext,ArrayList<String>> stack = 
       new HashMap<RuleContext,ArrayList<String>>();
    
    private boolean stripTypes;
    private boolean inferTypes;
    private boolean printTree;

    private String treeTab = "  ";
    private String codeTab = "   ";
    
    public TreePrinterListener(Parser parser, SymbolTable st) {
        this.ruleNames = Arrays.asList(parser.getRuleNames());
        this.symtab = st;
    }

    public void setStrip(boolean s) {
       this.stripTypes = s;
    }
    
    public void setTypeInfer(boolean s) {
       this.inferTypes = s;
    }
    
    public void setCode() {
       this.printTree = false;
    }
    
    public void setTree() {
       this.printTree = true;
    }
    
    @Override
    public void visitTerminal(TerminalNode node) {
    	String text = Utils.escapeWhitespace(
           Trees.getNodeText(node, ruleNames), false);
    	if(text.startsWith(" ") || text.endsWith(" "))
    		text = "'" + text + "'";
        stack.get(node.getParent()).add(text);
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        stack.get(node.getParent()).add(
           Utils.escapeWhitespace(Trees.getNodeText(node, ruleNames), false));
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {
        boolean proceed = true;
        int ruleIndex = 0;
        String ruleName;
        if(!stack.containsKey(ctx.parent))
            stack.put(ctx.parent, new ArrayList<String>());
        if(!stack.containsKey(ctx))
            stack.put(ctx, new ArrayList<String>());
        for (Map.Entry<RuleContext,ArrayList<String>> c : stack.entrySet()) {
           RuleContext ct = c.getKey();
           if (ct != null ) {
              ruleIndex = ct.getRuleIndex();
              if (ruleIndex >= 0 && ruleIndex < ruleNames.size())
                 ruleName = ruleNames.get(ruleIndex);
              else 
                 ruleName = Integer.toString(ruleIndex);
              if ( stripTypes && ruleName.equals("typeCast") ) {
                 proceed = false;
                 break;
              }
           }
        }
        ruleIndex = ctx.getRuleIndex();
        if (ruleIndex >= 0 && ruleIndex < ruleNames.size())
           ruleName = ruleNames.get(ruleIndex);
        else 
           ruleName = Integer.toString(ruleIndex);
        String tokenName = ctx.getStart().getText();
        
        if ( proceed && printTree) {
           final StringBuilder sb = new StringBuilder();
           sb.append(ruleName);
           stack.get(ctx).add(sb.toString());
        }

        if ( inferTypes ) {
           if (ctx instanceof HpcLuaParser.ChunkContext) 
              currentScope = symtab.getGlobals();
           else 
              if (ctx instanceof HpcLuaParser.BlockContext || 
                  ctx instanceof HpcLuaParser.ForLocalNameStatContext ||
                  ctx instanceof HpcLuaParser.ForLocalExpStatContext ||
                  ctx instanceof HpcLuaParser.FunctStatContext ||
                  ctx instanceof HpcLuaParser.LocalFunctStatContext ||
                  ctx instanceof HpcLuaParser.FunctDefContext)  
                 currentScope = symtab.getScopes().get(ctx);
        }
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        boolean proceed = true;
        int ruleIndex = 0;
        int level = 0;
        String ruleName;
        for (Map.Entry<RuleContext,ArrayList<String>> c : stack.entrySet()) {
           RuleContext ct = c.getKey();
           if (ct != null ) {
              ruleIndex = ct.getRuleIndex();
              if (ruleIndex >= 0 && ruleIndex < ruleNames.size())
                 ruleName = ruleNames.get(ruleIndex);
              else 
                 ruleName = Integer.toString(ruleIndex);
              if ( stripTypes && ruleName.equals("typeCast") ) {
                 proceed = false;
                 break;
              }
              if ( ruleName.equals("block") )
                 level = level + 1;
           }
        }
        ruleIndex = ctx.getRuleIndex();
        if (ruleIndex >= 0 && ruleIndex < ruleNames.size())
           ruleName = ruleNames.get(ruleIndex);
        else 
           ruleName = Integer.toString(ruleIndex);
        String tokenName = ctx.getStart().getText();
        StringBuilder sb = new StringBuilder();
        if ( (ruleName.equals("varName") || ruleName.equals("varType")) &&
             inferTypes )
           sb.append("(");
        
        ArrayList<String> ruleStack = stack.remove(ctx);
        boolean brackit = false;
        if ( proceed ) {
            brackit = ruleStack.size()>1; 
            if (brackit && printTree) 
                sb.append("(");
            for(int i=0; i<ruleStack.size(); i++) {
                String addition = ruleStack.get(i);
                if (i != 0 && addition.length() != 0 && 
                              !sb.toString().endsWith("\r\n")) 
                   sb.append(" ");
                sb.append(addition);
            }
            if(brackit && printTree)
                sb.append(")");
        }
        
        if ( (ruleName.equals("varName") || ruleName.equals("varType")) &&
             inferTypes ) {
           String name = ctx.getStop().getText();
           Symbol var = currentScope.resolveName(name);
           if (var != null)           
              sb.append(":. " + ((LuaType) var.getType()).expand(currentScope).toString() + ")");
           else
              sb.append(":. " + LuaType._Bottom.toString() + ")");
        }
        if (!printTree && ruleName.equals("stat"))
           sb.append("\r\n");
        if (sb.length() >= 80) { 
           sb.setLength(0);
           if (brackit && printTree)
              sb.append("(");
           while (!ruleStack.isEmpty()){
              if (printTree) 
                 sb.append(indentTree(ruleStack.remove(0))).append("\r\n"); 
              else 
                 sb.append(indentCode(ruleStack.remove(0))); 
           }
           if (brackit && printTree)
              sb.append(")");
        }
        stack.get(ctx.parent).add(sb.toString());
        if (ctx.parent == null) {
           sb.append("\r\n");
           builder.append(sb.toString());
        }

        if (inferTypes &&
            (ctx instanceof HpcLuaParser.BlockContext || 
            ctx instanceof HpcLuaParser.ForLocalNameStatContext ||
            ctx instanceof HpcLuaParser.ForLocalExpStatContext ||
            ctx instanceof HpcLuaParser.FunctStatContext ||
            ctx instanceof HpcLuaParser.LocalFunctStatContext ||
            ctx instanceof HpcLuaParser.FunctDefContext)) {  
           currentScope = currentScope.getEnclosingScope();
           if (currentScope == null) 
              currentScope = symtab.getGlobals();
        }
    }
    
    private String indentTree(String input) {
    	return input.replaceAll("\r\n(.)","\r\n" + treeTab + "$1"); 
    }

    private String indentCode(String input) {
       return input.replaceAll("\r\n(.)","\r\n" + codeTab + "$1");
    }

    @Override
    public String toString() {
        return builder.toString();
    }
} 
