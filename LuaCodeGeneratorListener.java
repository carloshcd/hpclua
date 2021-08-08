/***
  LuaCodeGeneratorListener.java

  [The "BSD license"]
  Copyright (c) 2020 Carlos Henrique Cabral Duarte  
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.antlr.v4.runtime.misc.Utils;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.Trees;

import java.io.InputStream;
import java.io.IOException;
import java.lang.InterruptedException;
import java.io.PrintWriter;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileNotFoundException;

public class LuaCodeGeneratorListener implements ParseTreeListener {
    SymbolTable symtab;
    Scope currentScope;

    private List<String> ruleNames;
    private StringBuilder builder = new StringBuilder();
    Map<RuleContext,ArrayList<String>> stack = 
       new HashMap<RuleContext,ArrayList<String>>();
       
    private Language language;       
    private String generator;
    private String source;
       
    private String newcode = null;

    private static String codeTab     = "   ";
    private static String comment     = "-- ";
    private static String eol = "\n";
    
    private static String cppExt = "cpp";
    private static String objExt = "-obj";
    private static String libExt = "so";

    private static char LP = '(';
    private static char RP = ')';
    private static char LB = '{';
    private static char RB = '}'; 
    
    private static String LOCAL = "local";
    private static String FUNCTION = "function";
    
    public LuaCodeGeneratorListener(Parser parser, LuaAnalyser analyser) {
        this.ruleNames = Arrays.asList(parser.getRuleNames());
        this.symtab = analyser.getSymbolTable();
        this.language = analyser.getLanguage();
        this.source = analyser.getSourceCodeName();
        this.generator = "";
    }
    
    public String getGenerator() {
       return this.generator;
    }
    
    public void setGenerator(String gen) {
       this.generator = gen;
    }
    
    public Language getLanguage() {
       return this.language;
    }
    
    public String getSource() {
       return this.source;
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
        if(!stack.containsKey(ctx.parent))
            stack.put(ctx.parent, new ArrayList<String>());
        if(!stack.containsKey(ctx))
            stack.put(ctx, new ArrayList<String>());
            
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

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {
        boolean has_newcode = false;
        String generated = "";
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
              if ( ruleName.equals("typeAscr") ) {
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
        String nextName = "";
        String funcName = "";
                   
        StringBuilder sb = new StringBuilder();        
        ArrayList<String> ruleStack = stack.remove(ctx);
        String addition = "";
        if ( proceed ) {
           int stackSize = ruleStack.size();
           boolean indent = false;
           for(int i=0; i < stackSize; i++) {
              addition = ruleStack.get(i);
              if (i == 1) 
                 nextName = addition;
              if ((i == 1 && startName.equals("function")) ||
                  (i == 2 && nextName.equals("function")))
                 funcName = addition;                    
              if (ruleName.equals("stat")) {
                 if (indent) {
                    addition = treatCode(addition,codeTab);
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
                if (ruleName.equals("functBody") && i == stackSize - 2)
                   addition = eol + treatCode(addition,codeTab);
              if (i != 0 && addition.length() != 0 && 
                  !sb.toString().endsWith(eol))  
                 addition = " " + addition;
              sb.append(addition);
           }
           if (ruleName.equals("stat") ||
               ruleName.equals("retStat")) {
              if (!startName.equals("do") &&
                  !startName.equals("while") &&
                  !startName.equals("repeat") &&
                  !startName.equals("if") &&
                  !startName.equals("for") &&
                  !startName.equals("function") &&
                  !startName.equals("local function"))
                 sb.append(";");
              sb.append(eol);
           }
           if (ruleName.equals("stat") &&  
               (startName.equals("function") || nextName.equals("function"))) {
              Symbol var = currentScope.resolveName(funcName);
              if (var != null) {               
                 FunctionSymbol scheduler = var.getScheduler();
                 if ((var.isScheduler() && 
                      Boolean.TRUE.equals(var.
                                          getDealsJustWithNumKeyTables())) || 
                     (scheduler != null && 
                      Boolean.TRUE.equals(scheduler.
                                          getDealsJustWithNumKeyTables()))) {
                    String original = FUNCTION + " " + funcName + 
                                      " " + addition;
                    if (var.isScheduler()) {
                       generated = treatCode(
                          (!funcName.equals(nextName)?LOCAL + " ":"") + 
                          original + eol, comment);
                    } else {
                       generated = luaFunctionSerialize(funcName, original, 
                          !funcName.equals(nextName));
                       luaFunctionGenerateCpp(var,original, 
                          !funcName.equals(nextName));
                    }
                    sb = new StringBuilder();
                    sb.append(generated);
                 }
              }
           } else
              if (ruleName.equals("functionCall") ||
                  ruleName.equals("prefixExp")) {
                 HpcLuaParser.VarOrExpContext voectx = null;
                 HpcLuaParser.NameAndArgsContext naactx = null;
                 if (ruleName.equals("functionCall")) {
                    voectx = ((HpcLuaParser.FunctionCallContext) ctx).
                                varOrExp();
                    naactx = ((HpcLuaParser.FunctionCallContext) ctx).
                                nameAndArgs(0);
                 } else {
                    voectx = ((HpcLuaParser.PrefixExpContext) ctx).varOrExp();
                    naactx = ((HpcLuaParser.PrefixExpContext) ctx).
                                nameAndArgs(0);
                 }
                 LuaType type = LuaType.expand(currentScope, voectx.type);
                 if (type.subtype(LuaTypeSystem._mapType)) {
                    if (naactx != null && naactx.args != null) {
                       HpcLuaParser.ExpContext firstArg = 
                          naactx.args.expList.exp;
                       LuaType firstArgType = LuaType.expand(currentScope, 
                                                             firstArg.type);
                       HpcLuaParser.ExpContext secondArg = 
                          naactx.args.expList.el.exp;
                       LuaType secondArgType = LuaType.expand(currentScope, 
                                                              secondArg.type);
                       if (firstArgType instanceof FunctionType && 
                           firstArgType.isNumericType() && 
                           secondArgType instanceof TableType && 
                           secondArgType.hasJustNumericKeys()) {
                          String call = voectx.getText() + " ( _main, " + 
                                        secondArg.getText() + " ) ";
                          sb = new StringBuilder();
                          sb.append(call);
                          generated = "_main = " + firstArg.getText() + " " + 
                                      comment + eol;
                          has_newcode = true;
                       }
                    }
                 }
              }
        }
        
        if (newcode != null && ruleName.equals("stat")) {
           stack.get(ctx.parent).add(newcode);
           newcode = null;
        }
        stack.get(ctx.parent).add(sb.toString());
        if (ctx.parent == null)
           builder.append(sb.toString());
        if (has_newcode)
           newcode = generated;

        if (ctx instanceof HpcLuaParser.BlockContext || 
            ctx instanceof HpcLuaParser.DoStatContext ||
            ctx instanceof HpcLuaParser.ForNumStatContext ||
            ctx instanceof HpcLuaParser.ForGenStatContext ||
            ctx instanceof HpcLuaParser.FunctStatContext ||
            ctx instanceof HpcLuaParser.LocalFunctStatContext ||
            ctx instanceof HpcLuaParser.FunctDefContext) {  
           currentScope = currentScope.getEnclosingScope();
           if (currentScope == null) 
              currentScope = symtab.getGlobals();
        }
    }
    
   private String treatCode(String input, String insert) {
      int len = input.length();
      String result = "";
      int i = 0;
      while (i < len) {
         int eolp = input.indexOf(eol,i);
         result = result + insert;
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
    
   public String luaFunctionSerialize(String name, String code, 
                                      boolean local) { 
      String preamble = treatCode((local?LOCAL + " ":"") + code, comment);
      String template = 
      "bin = string.dump(" + name + ")"  
      + eol +  
      "fbin = \"\""  
      + eol + 
      "for i = 1, string.len(bin) do"  
      + eol +
      "   dec,_ = (\"" + "\\\\" + 
                  "%3d\"):format(bin:sub(i, i):byte()):gsub(\' \', \'0\')"
      + eol +  
      "   fbin = fbin .. dec" 
      + eol + 
      "end" 
      + eol + 
      "print(\"" + name + "\" .. \" = \" .. \"load \\\"\" .. fbin .. \"\\\"\")" 
      + eol;
      String source = code + eol + eol + template;
      
      String generated = local?LOCAL + " ":"";
      try {
         PrintWriter file = new PrintWriter(name + objExt + "." + 
                                   this.getLanguage().getExt());
         file.println(source);
         file.close();
         boolean luajit = !this.getGenerator().equals(
                              this.getLanguage().getExt());
         ProcessBuilder pb;
         if (luajit) 
            pb = new ProcessBuilder(this.getGenerator(), "-O3",
                                    name + objExt + "." + 
                                    this.getLanguage().getExt());
         else
            pb = new ProcessBuilder(this.getGenerator(), 
                                    name + objExt + "." + 
                                    this.getLanguage().getExt());
         try {
            Process process = pb.start();
            InputStream is = process.getInputStream();
            int c;
            while ( (c = is.read()) != -1 )
               if (c != '\n')
                  generated = generated + ((char) c);
            process.waitFor();
         } catch (InterruptedException ie) {
            ie.printStackTrace();
            System.exit(1);
         } finally {
            File f = new File(name + objExt + "." + 
                              this.getLanguage().getExt());
            f.delete();
         }
      } catch (IOException ioe) {
         ioe.printStackTrace();
         System.exit(1);
      }
      return preamble + eol + generated + " " + comment + eol;
   }

   public static final Map<String,String> LuaToCTranslation =
      new LinkedHashMap<String,String>() {
      private static final long serialVersionUID = 1601L; {
         String Name = "[a-zA-Z_][a-zA-Z_0-9]*";
         String NameList = Name + " (, " + Name +")*";
         String Exp = "\\S*";
         
         /* logical operations (just those that suffer changes) */
         put(" or ", " || ");
         put(" and ", " && ");
         put("not", "!");
         put("~=", "!=");
                  
         /* arithmetic operatiors */
         put(" ("+Exp+") // ("+Exp+") ", " floor( $1 / $2 ) "); 
         put(" ("+Exp+") \\^ ("+Exp+") ", " pow( $1 , $2 ) "); 
         put(" ("+Exp+") % ("+Exp+") ", " fmod( $1 , $2 ) "); 
         
         /* statements */
         put(";", ";");
         put("=", "=");
         put("break", "break");
         put("goto", "goto");
         put("if", "if");
         put("then", "{");
         put("else", "} else {");
         put("elseif", "} else if");
         put("do", "{"); 
         put("end", "}");
         put("while", "while {"); 
         put("repeat", "do {");
         put("until", "} while !"); 
         put("for ("+Name+") = ("+Exp+") [, ("+Exp+") [, ("+Exp+")]]", "for ($1=$2; $1 <= $3; $1 += $4) "); 
         put("for "+NameList+" in ", null);
         put("function", "");
         put("return", "return");
         put("local", null);
         
         /* predefined symbols */
         put("# arg (^\\[)", "main_argc $1"); 
         put("arg \\[(.*)\\]", "*main_argv[ $1 + 1 ]");
         put("tonumber ("+Exp+")", "(lua_Number) ($1)");

         /* values */
         put("nil", "null");
         put("false", "0");
         put("true", "1");

         /* types */
         put("Number", "lua_Number");
         put("Double", "lua_Number");
         put("Single", "lua_Number");
         put("Long", "lua_Integer");
         put("Int", "lua_Integer");
         put("Nil", "(void *)");
   }};
   
   public String luaFunctionTranslateToC(String code) {
      String result = code;
      Set<Map.Entry<String,String>> tes = LuaToCTranslation.entrySet();
      for(Map.Entry<String,String> te : tes ) {
         String key = te.getKey();
         String value = te.getValue();
         int index = result.indexOf(key);
         if (index >= 0 && value == null)
            new Throwable().printStackTrace();
         else {
            if (LOCAL.equals(key)) { 
               while (index >= 0) {
                  result = result.replace(key,value);
                  index = result.indexOf(key);
               }
            } else 
               result = result.replaceAll(key,value);
         }
      }
      return result;
   }
   
   public String insertSignature(FunctionType type, String code) {
      List<LuaType> results = type.getRetTypes().getElements();
      String result = results.get(0).fold() + " ";
      List<LuaType> params = type.getParamTypes().getElements();
      if (params.size() == 1 && params.get(0) instanceof SequenceType)
         params = ((SequenceType) params.get(0)).getElements();
      int paramidx = 0;
      boolean foundLP = false;
      boolean foundRP = false;
      int len = code.length();
      for(int i=0;i<len;i++) {
         char c = code.charAt(i);
         String add = Character.toString(c);
         if (c == LP && !foundLP) {
            foundLP = true;
            LuaType numType = params.get(paramidx).mostGeneralNumericType();
            if (type == null) 
               new Throwable().printStackTrace();
            else {
               add = add + numType.fold() + " ";
               paramidx++;
            }
         } else
            if (c == RP && !foundRP) {
               foundRP = true;
               add = add + " do";
            } else
               if (c == ',' && foundLP && !foundRP) {
                  LuaType numType = params.get(paramidx).
                                       mostGeneralNumericType();
                  if (numType == null) 
                     new Throwable().printStackTrace();
                  else {
                     add = add + " " + numType.fold() + " ";
                     paramidx++;
                  }
               }
         result = result + add;
      }
      return result;
   }

   public String insertExtern(String code) {
      String signature = code.substring(0,code.indexOf(LB)) + ";";
      String externDcl = "extern \"C\" " + LB + eol + 
                          signature + eol + 
                          RB + eol + eol;
      return externDcl + code;
   }

   public String stripExt(String f) {
      return f.substring(0,f.lastIndexOf('.'));
   }

   public String luaFunctionGenerateCpp(Symbol var, String code, 
                                        boolean local) { 
      String name = var.getName();
      FunctionType type = (FunctionType) var.getType();
      String preamble = treatCode((local?LOCAL + " ":"") + code, comment);

      String source = "";
      String genCodeFileName = this.stripExt(this.getSource()) + "." + cppExt;
      File genCodeFile = new File(genCodeFileName);
      boolean append = false;

      if (!genCodeFile.exists()) {
         boolean luajit = !this.getGenerator().equals(
                                                  this.getLanguage().getExt());
         source = "#include <stdio.h>" + eol + 
            "#include <stdlib.h>" + eol +  
            "#include <math.h>" + eol +   
            "extern \"C\" {" + eol + 
            "   #include \"lua.h\"" + eol + 
            "   #include \"lauxlib.h\"" + eol + 
            "   #include \"lualib.h\"" + eol + 
            (luajit?"   #include \"luajit.h\"" + eol:"") + "}" + eol +  
            eol +
            "extern \"C\" {" + eol + 
            "   int main_argc = 0;" + eol + 
            "   lua_Number** main_argv = NULL;" + eol +
            "}" + eol;
      } else
            append = true;
      source = source + eol + insertExtern(
                                 luaFunctionTranslateToC(
                                    insertSignature(type, code))) + eol;
      try {
         PrintWriter file = new PrintWriter(new FileOutputStream(
            new File(genCodeFileName), append));
         file.append(source);
         file.close();
      } catch (FileNotFoundException ioe) {
         ioe.printStackTrace();
         System.exit(1);
      }
      return preamble + eol + comment + name + " " + comment + eol;
   }
   
   public void compileGeneratedCpp() { 
      String genCodeFileName = this.stripExt(this.getSource());
      File genCodeFile = new File(genCodeFileName + "." + cppExt);
      if (genCodeFile.exists()) {
         String errMsg = "";
         ProcessBuilder pb = new ProcessBuilder(
                    "g++", "-fPIC", "-march=native", "-mtune=native", "-Ofast",
                    "-flto", 
                     "-D_LINUX", "-fno-common", "-shared", "-Wall", "-Wextra", 
                    "-o", 
                    genCodeFileName + "." + libExt, 
                    genCodeFileName + "." + cppExt);
         try {
            Process process = pb.start();
            InputStream is = process.getErrorStream();
            int c;
            while ( (c = is.read()) != -1 ) {
               if ("".equals(errMsg)) 
                  errMsg = eol;
               errMsg = errMsg + ((char) c);
            }
            process.waitFor();
         } catch (InterruptedException ie) {
            ie.printStackTrace();
            System.exit(1);
         } catch (IOException ioe) { 
            ioe.printStackTrace();
            System.exit(1);
         } finally {
            File file = new File(genCodeFileName + "." + libExt);
            if (!file.exists() || !("".equals(errMsg))) {
               System.out.printf("Error message: %s\n", errMsg);
               new Throwable().printStackTrace();
               System.exit(1);
            } else {
               file = new File(genCodeFileName + "." + cppExt);
               file.delete();
            }
         }
      } 
   }
} 

