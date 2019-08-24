/***
  HpcLuaTool.java

  [The "BSD license"]
  Copyright (c) 2018 Terence Parr
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
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DiagnosticErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.atn.LexerATNSimulator;
import org.antlr.v4.runtime.atn.PredictionMode;

import java.nio.file.Files;
import java.io.File;
import java.io.IOException;
import java.lang.System;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

class HpcLuaTool {

   public static boolean profile = false;
   public static boolean printTree = false;
   public static boolean SLL = false;
   public static boolean diag = false;
   public static boolean bail = false;
   public static boolean x2 = false;
   public static boolean threaded = false;

   public static boolean verbose = false;
   public static boolean stype = false;
   public static boolean pprint = false;
   public static boolean help = false;

   public static boolean typei = false;
   public static int fdefault = 0;
   public static int idefault = 0;
   public static int ndefault = 0;
   
   public static Worker[] workers = new Worker[3];
   static int windex = 0;

   public static CyclicBarrier barrier;

   public static volatile boolean firstPassDone = false;

   public static class Worker implements Runnable {
      public long parserStart;
      public long parserStop;
      List<String> files;
      public Worker(List<String> files) {
         this.files = files;
      }
      @Override
      public void run() {
         parserStart = System.currentTimeMillis();
         for (String f : files) {
            parseFile(f);
         }
         parserStop = System.currentTimeMillis();
         try {
            barrier.await();
         }
         catch (InterruptedException ex) {
            return;
         }
         catch (BrokenBarrierException ex) {
            return;
         }
      }
   }

   public static void main(String[] args) {
      doAll(args);
   }

   public static void doAll(String[] args) {
      try {
         List<String> inputFiles = new ArrayList<String>();
            long start = System.currentTimeMillis();
            String warning = null;
         if (args.length > 0 ) {
            for(int i=0; i< args.length; i++) {
               if ( args[i].equals("-prof") ) profile = true;
               else if ( args[i].equals("-ptree") ) printTree = true;
               else if ( args[i].equals("-SLL") ) SLL = true;
               else if ( args[i].equals("-diag") ) diag = true;
               else if ( args[i].equals("-bail") ) bail = true;
               else if ( args[i].equals("-2x") ) x2 = true;
               else if ( args[i].equals("-threaded") ) threaded = true;
               else if ( args[i].equals("-verb") ) verbose = true;
               else if ( args[i].equals("-stype") ) stype = true;
               else if ( args[i].equals("-pprint") ) pprint = true;
               else if ( args[i].equals("-help") ) help = true;
               else if ( args[i].equals("-typei") ) typei = true;
               else if ( args[i].equals("-dFloat") ) ndefault = 1;
               else if ( args[i].equals("-dInteger") ) ndefault = 2;
               else if ( args[i].equals("-dDouble") ) fdefault = 1;
               else if ( args[i].equals("-dSingle") ) fdefault = 2;
               else if ( args[i].equals("-dLong") ) idefault = 1;
               else if ( args[i].equals("-dInt") ) idefault = 2;
               else if ( args[i].charAt(0) == '-' ) 
                        warning = "Invalid command line option!";
               if ( args[i].charAt(0) !='-' ) { 
                  inputFiles.add(args[i]);
               }
            }
            if ( warning != null )
               help = true;
            if ( !help ) {
               List<String> sourceFiles = new ArrayList<String>();
               for (String fileName : inputFiles) {
                  List<String> files = getFilenames(new File(fileName));
                     sourceFiles.addAll(files);
                }
                doFiles(sourceFiles);

                if ( x2 ) {
                   System.gc();
                   if ( verbose ) System.out.println("waiting for 1st pass");
                   if ( threaded ) while ( !firstPassDone ) { } 
                   if ( verbose ) System.out.println("2nd pass");
                      doFiles(sourceFiles);
                }
            } else {
               if ( warning != null ) 
                  System.err.println(warning);
               System.out.println("Usage: java HpcLuaTool [OPTION] <directory or file name(s)>");
               System.out.println("Parses and treats a hpclua directory or file(s).\n");
               System.out.println("Command line options:");
               System.out.println("-prof: presents profiling info");
               System.out.println("-ptree: prints the parse tree");
               System.out.println("-SLL: use SLL prediction");
               System.out.println("-diag: identifies additional grammar problems");
               System.out.println("-bail: imediatelly cancels parsing on errors");
               System.out.println("-2x: performs a second compile pass");
               System.out.println("-threaded: threaded runtime execution");
               System.out.println("-verb: works more verbosely");;
               System.out.println("-stype: strips types from code");
               System.out.println("-typei: infer program types");
               System.out.println("-dFloat: Float is the default numerical type");
               System.out.println("-dInteger: Integer is the default numerical type");
               System.out.println("-dDouble: Double is the default floating-point type");
               System.out.println("-dSingle: Single is the default floating-point type");
               System.out.println("-dLong: Long is the default integer type");
               System.out.println("-dInt: Int is the default integer type");
               System.out.println("-pprint: pretty prints the parse tree");
               System.out.println("-help: this message");
            }
         } else {
            System.err.println("Usage: java HpcLuaTool [OPTIONS] <directory or file name(s)>");
            System.err.println("Try 'java HpcLuaTool -help' for more information.\n");
         }
        long stop = System.currentTimeMillis();
        if ( profile ) System.out.println("Total elapsed time " + (stop - start) + "ms.");
        System.gc();
      } catch(Exception e) {
         System.err.println("exception: "+e);
         e.printStackTrace(System.err);   
      }
   }

   public static void doFiles(List<String> files) throws Exception {
      long parserStart = System.currentTimeMillis();
      if ( threaded ) {
         barrier = new CyclicBarrier(3,new Runnable() {
            public void run() {
               report(); firstPassDone = true;
            }
         });
         int chunkSize = files.size() / 3;  // 10/3 = 3
         int p1 = chunkSize; // 0..3
         int p2 = 2 * chunkSize; // 4..6, then 7..10
         workers[0] = new Worker(files.subList(0,p1+1));
         workers[1] = new Worker(files.subList(p1+1,p2+1));
         workers[2] = new Worker(files.subList(p2+1,files.size()));
         new Thread(workers[0], "worker-"+windex++).start();
         new Thread(workers[1], "worker-"+windex++).start();
         new Thread(workers[2], "worker-"+windex++).start();
      }
      else {
         for (String f : files) {
            parseFile(f);
         }
         long parserStop = System.currentTimeMillis();
         if ( profile ) System.out.println("Total lexer+parser time " + (parserStop - parserStart) + "ms.");
      }
   }

   private static void report() {
      long time = 0;
      if ( workers!=null ) {
         for (Worker w : workers) {
            long wtime = w.parserStop - w.parserStart;
            time = Math.max(time,wtime);
            if ( profile ) System.out.println("worker time " + wtime + "ms.");
         }
      }
      if ( verbose ) {
         if ( profile ) System.out.println("Total lexer+parser time " + time + "ms.");
         System.out.println("finished parsing OK");
         System.out.println(LexerATNSimulator.match_calls+" lexer match calls");
      }
   }

   public static List<String> getFilenames(File f) throws Exception {
      List<String> files = new ArrayList<String>();
      getFilenames_(f, files);
      return files;
   }

   public static void getFilenames_(File f, List<String> files) throws Exception {
      if (f.isDirectory()) {
         String flist[] = f.list();
         for(int i=0; i < flist.length; i++) {
            getFilenames_(new File(f, flist[i]), files);
         }
      } else 
         if ( ((f.getName().length()>4) &&
               f.getName().substring(f.getName().length()-4).equals(".lua")) )
            files.add(f.getAbsolutePath());
   }

   public static void parseFile(String f) {
      File file = new File(f);
      if (!file.exists()) 
         System.err.println("File not found: " + f);
      else {
         if ( verbose ) System.out.println("File: " + f);
         try {
            CharStream codePointCharStream = CharStreams.fromFileName(f);
            HpcLuaLexer lexer =  new HpcLuaLexer(codePointCharStream);
            CommonTokenStream tokens = new CommonTokenStream(lexer);

            LuaAnalyser analyser = new LuaAnalyser(ndefault,fdefault,idefault);
            HpcLuaParser parser = new HpcLuaParser(tokens);
            if ( diag ) parser.addErrorListener(new DiagnosticErrorListener());
            if ( bail ) parser.setErrorHandler(new BailErrorStrategy());
            if ( SLL ) parser.getInterpreter().setPredictionMode(PredictionMode.SLL);

            parser.setBuildParseTree(true);
            ParseTree tree = parser.chunk();
   
            ParseTreeWalker walker = new ParseTreeWalker();
            if (typei) {
               DefPhase def = new DefPhase(analyser);
               walker.walk(def, tree);
               RefPhase ref = new RefPhase(analyser);
               walker.walk(ref, tree);
            }
      
            TreePrinterListener listener = 
               new TreePrinterListener(parser,
                  analyser.getSymbolTable());
            listener.setStrip(stype);
            listener.setTypeInfer(typei);
            if ( pprint )
               listener.setCode();
            else 
               listener.setTree();

            if ( typei )
               walker.walk(listener, tree);
            else 
               ParseTreeWalker.DEFAULT.walk(listener, tree);

            if ( pprint || printTree ) 
               System.out.print(listener.toString());
         } catch (IOException i) {
            System.err.println("file handling exception: "+i);
            i.printStackTrace();
         } catch (Exception e) {
            System.err.println("parser exception: "+e);
            e.printStackTrace();
         }
      }
   }
}
