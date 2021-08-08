/***
  LuaTreePrinterListener.java

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

public class LuaTreePrinterListener implements ParseTreeListener {
    SymbolTable symtab;
    Scope currentScope;

    private List<String> ruleNames;
    private StringBuilder builder = new StringBuilder();
    Map<RuleContext,ArrayList<String>> stack = 
       new HashMap<RuleContext,ArrayList<String>>();
    
    private boolean stripTypes;
    private boolean inferTypes;
    private boolean printTree;

    private static String treeTab = "  ";
    private static String codeTab = "   ";
    private static String eol = "\n";
    
    public LuaTreePrinterListener(Parser parser, SymbolTable st) {
        this.ruleNames = Arrays.asList(parser.getRuleNames());
        this.symtab = st;
        this.stripTypes = true;
        this.inferTypes = true;
        this.printTree = true;
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
       if (text.startsWith(" ") || text.endsWith(" "))
          text = "'" + text + "'";
       if (!text.equals("<EOF>"))
          stack.get(node.getParent()).add(text);
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        stack.get(node.getParent()).add(
           Utils.escapeWhitespace(Trees.getNodeText(node,ruleNames),false));
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
              if ( stripTypes && ruleName.equals("typeAscr") ) {
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
        
        if ( proceed && printTree) {
           StringBuilder sb = new StringBuilder();
           sb.append(ruleName);
           stack.get(ctx).add(sb.toString());
        }

        if ( inferTypes ) {
           if (ctx instanceof HpcLuaParser.ChunkContext) 
              currentScope = symtab.getGlobals();
           else 
              if (ctx instanceof HpcLuaParser.BlockContext || 
                  ctx instanceof HpcLuaParser.DoStatContext ||
                  ctx instanceof HpcLuaParser.ForNumStatContext ||
                  ctx instanceof HpcLuaParser.ForGenStatContext ||
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
        String ruleName;
        for (Map.Entry<RuleContext,ArrayList<String>> c : stack.entrySet()) {
           RuleContext ct = c.getKey();
           if (ct != null) {
              ruleIndex = ct.getRuleIndex();
              if (ruleIndex >= 0 && ruleIndex < ruleNames.size())
                 ruleName = ruleNames.get(ruleIndex);
              else 
                 ruleName = Integer.toString(ruleIndex);
              if ( stripTypes && ruleName.equals("typeAscr") ) {
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
        String startName = ctx.getStart().getText();
        
        StringBuilder sb = new StringBuilder();
        if ( (ruleName.equals("varName") || ruleName.equals("varType")) &&
             inferTypes && !stripTypes)
           sb.append("(");
        
        ArrayList<String> ruleStack = stack.remove(ctx);
        boolean brackit = false;
        String addition = "";
        if ( proceed ) {
           brackit = ruleStack.size()>1; 
           if (brackit && printTree) 
              sb.append("(");
           int stackSize = ruleStack.size();
           boolean indent = false;
           for(int i=0; i < stackSize; i++) {
              addition = ruleStack.get(i);
              if (!printTree) {
                 if (ruleName.equals("stat")) {
                    if (indent) {
                       addition = indentCode(addition);
                       indent = false;
                    }
                    boolean toBeIndented = false;
                    if ((startName.equals("do") || 
                         startName.equals("for") || 
                         startName.equals("while")) && addition.equals("do"))
                       toBeIndented = true;
                    else if (startName.equals("repeat") && 
                             addition.equals("repeat"))
                       toBeIndented = true;
                    else if (startName.equals("if")) {
                       if (addition.equals("then") || 
                           addition.equals("else")) 
                          toBeIndented = true;
                    }
                    if (toBeIndented) { 
                       addition = addition + eol;
                       indent = true;
                    }
                 } else 
                    if (ruleName.equals("functBody") && 
                        i == stackSize - 2)
                       addition = eol + indentCode(addition);
              }
              if (i != 0 && addition.length() != 0 && 
                  !sb.toString().endsWith(eol)) { 
                 addition = " " + addition;
              }
              sb.append(addition);
           }
           if (!printTree) {
              if (ruleName.equals("stat") ||
                  ruleName.equals("retStat"))
                 sb.append(eol);
           } else 
              if (brackit && printTree)
                 sb.append(")");
        }
       
        String type = ""; 
        if ( (ruleName.equals("varName") || ruleName.equals("varType")) && 
             inferTypes ) {
           if ( !stripTypes ) {
              String name = ctx.getStop().getText();
              Symbol var = currentScope.resolveName(name);
              if (var != null) 
                 type = ":." + 
                    ((LuaType) var.getType()).expand(currentScope).fold();
              else
                 type = ":." + LuaType._Bottom.fold();
              sb.append(type);
              sb.append(")");
           }
        }
        
        if (sb.length() >= 80 && printTree) { 
           sb.setLength(0);
           if (brackit)
              sb.append("(");
           while (!ruleStack.isEmpty()){
              addition = ruleStack.remove(0);
              if (ruleStack.isEmpty())
                 addition = addition + type;
              sb.append(indentTree(addition)).append(eol); 
           }
           if (brackit)
              sb.append(")");
        }
        
        stack.get(ctx.parent).add(sb.toString());
        if (ctx.parent == null) {
           if (printTree) 
              sb.append(eol);
           builder.append(sb.toString());
        }

        if (inferTypes &&
            (ctx instanceof HpcLuaParser.BlockContext || 
             ctx instanceof HpcLuaParser.DoStatContext ||
             ctx instanceof HpcLuaParser.ForNumStatContext ||
             ctx instanceof HpcLuaParser.ForGenStatContext ||
             ctx instanceof HpcLuaParser.FunctStatContext ||
             ctx instanceof HpcLuaParser.LocalFunctStatContext ||
             ctx instanceof HpcLuaParser.FunctDefContext)) {  
           currentScope = currentScope.getEnclosingScope();
           if (currentScope == null) 
              currentScope = symtab.getGlobals();
        }
    }
    
    private String indentTree(String input) {
       return input.replaceAll(eol + "(.)",eol + treeTab + "$1"); 
    }

    private String indentCode(String input) {
       int len = input.length();
       String result = "";
       int i = 0;
       while (i < len) {
          int eolp = input.indexOf(eol,i);
          result = result + codeTab;
          if (eolp > 0) {
             result = result + input.substring(i,eolp + eol.length());
             i = eolp + eol.length();
          } else {
             result = result + input.substring(i,len);
             i = len;
          }
       }
       return result;
    }

    @Override
    public String toString() {
        return builder.toString();
    }
} 
