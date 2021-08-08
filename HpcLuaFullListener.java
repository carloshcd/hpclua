/***
  HpcLuaFullListener.java

  Excerpted from The ANTLR 4 runtime

  [The "BSD license"]
  Copyright (c) 2012 Terence Parr
  Portions Copyright (c) 2020 Carlos Henrique Cabral Duarte
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

/* Make DefPhase and RefPhase subclasses of this ooe */
/* Activate the reversal in entering and leaving Assign rules */

public class HpcLuaFullListener extends HpcLuaBaseListener {
   boolean reverseTraversal;
   boolean secondFirstTraversal;
   Scope currentScope;
   Scope alternativeScope;

   public HpcLuaFullListener() {
      super();
      this.reverseTraversal = false;
      this.secondFirstTraversal = false;
      currentScope = null;
      alternativeScope = null;
   }

   public void setReverseTraversal(Scope s) {
      this.reverseTraversal = true;
      this.secondFirstTraversal = false;
      this.alternativeScope = s;
   }

   public void setSecondFirstTraversal(Scope s) {
      this.reverseTraversal = false;
      this.secondFirstTraversal = true; 
      this.alternativeScope = s; 
   }

   public void resetReverseTraversal() {
      this.reverseTraversal = false;
   }

   public void resetSecondFirstTraversal() {
      this.secondFirstTraversal = false;
   }

   public boolean getReverseTraversal() {
      return this.reverseTraversal;
   }

   public boolean getSecondFirstTraversal() {
      return this.secondFirstTraversal;
   }

   public void setCurrentScope(Scope s) {
      this.currentScope = s;
   }   

   public Scope getCurrentScope() {
      return this.currentScope;
   }

   public Scope getAlternativeScope() {
      return this.alternativeScope;
   }
}
