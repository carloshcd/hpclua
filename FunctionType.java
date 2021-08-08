/***
  FunctionType.java

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
import java.util.List;

public class FunctionType extends LuaType {

    private static final long serialVersionUID = 4L;

    SequenceType paramTypes;
    SequenceType retTypes;   
 
    public FunctionType(LuaType a, LuaType r) {
        super(LuaType.tFUNCTION);
        if (a instanceof SequenceType)  
           this.paramTypes = (SequenceType) a;
        else
           this.paramTypes = new SequenceType (a);
        if (r instanceof SequenceType) 
           this.retTypes = (SequenceType) r;
        else 
           this.retTypes = new SequenceType (r);        
    }
    
    public SequenceType getParamTypes() {
        return paramTypes;
    }

    public SequenceType getRetTypes() {
       return retTypes;
    }
   
    public void setParamTypes(SequenceType p) {
       this.paramTypes = p;
    }

    public void setRetTypes(SequenceType r) {
       this.retTypes = r;
    }

    @Override 
    public String toString() { 
       String result = "(";
       if (this.getParamTypes().getElements().size() == 1) 
          result = result + this.getParamTypes().getElements().get(0).fold();
       else
          result = result + this.getParamTypes().fold();
       result = result + " -> ";
       if (this.getRetTypes().getElements().size() == 1)
          result = result + this.getRetTypes().getElements().get(0).fold();
       else
          result = result + this.getRetTypes().fold();
       return result + ")"; 
    }
    
    @Override 
    public FunctionType expand(Scope s) { 
       this.setParamTypes(this.getParamTypes().expand(s));
       this.setRetTypes(this.getRetTypes().expand(s));
       return this;
    }
    
    @Override
    public boolean equals(Object t) {
        if (t == this)
            return true;
        if (t == null) 
            return false; 
        if (!getClass().equals(t.getClass()))
            return false;
        FunctionType other = (FunctionType) t;
        return (this.getParamTypes().equals(other.getParamTypes())) &&
               (this.getRetTypes().equals(other.getRetTypes()));
    }

    @Override
    public boolean subtype(LuaType t) {
       if (t.equals(LuaType._Any)) // TTop
          return true; 
       if (t instanceof FunctionType) { // TFunc
          // System.out.printf("Compare %s with %s\n", this, t);
          FunctionType tp = (FunctionType) t;
          return (tp.getParamTypes().subtype((this.getParamTypes()))) && 
                 (this.getRetTypes().subtype(tp.getRetTypes()));
       } else 
          if (t instanceof SequenceType) // TSeq
             return ((SequenceType) t).TSeq(this);
          else
             if (t instanceof UnionType) // TUSup
                return ((UnionType) t).TUSup(this);
       return false;
    }
    
    @Override
    public LuaType subst(LuaType v, LuaType t) { 
       LuaType obj = this.getParamTypes();
       if (obj.equals(v))
          this.setParamTypes((SequenceType) LuaType.copy(t));
       else
          obj.subst(v,t);
       obj = this.getRetTypes();
       if (obj.equals(v))
          this.setRetTypes((SequenceType) LuaType.copy(t));
       else
          obj.subst(v,t);
       return this;
    }
    
    @Override
    public LuaType subst(Scope s) { 
       this.setParamTypes((SequenceType) this.getParamTypes().subst(s));
       this.setRetTypes((SequenceType) this.getRetTypes().subst(s));
       return this;
    }
    
    @Override 
    public List<VarType> freeTVars() {
       List<VarType> freeParamVars = this.getParamTypes().freeTVars(); 
       List<VarType> freeRetVars = this.getRetTypes().freeTVars();
       for(VarType v : freeRetVars) { 
          if (!freeParamVars.contains(v))
             freeParamVars.add(v);
       }
       return freeParamVars;
    }
    
    @Override
    public Scope unifiesWith (Scope s, LuaType t, int verb) {
       if ( verb > 1 ) 
          System.out.printf("Unifying FunctionType: %s/%s\n", this, t);
       if (t.equals(LuaType._Any))
          return s;  
       else if (t instanceof VarType || 
                t instanceof UnionType)
          return t.unifiesWith(s,this,verb);
       else 
          if (t instanceof FunctionType) { // FAbs 
             List<LuaType> l1 = this.getParamTypes().getElements();
             List<LuaType> l2 = ((FunctionType) t).getParamTypes().
                                   getElements();
             Scope s1 = LuaType.unifiesLists(s,l1,l2,verb);
             if (s1 != null) {
                l1 = this.getRetTypes().getElements();
                l2 = ((FunctionType) t).getRetTypes().getElements();
                Scope s2 = LuaType.unifiesLists(s1,l1,l2,verb);
                if (s2 != null && verb > 1) 
                   System.out.printf("Applying: FAbs\n");
                return s2;
             } else
                return null;
          } else
             return null;
    }
    
    public LuaType bindFree() {
       List<VarType> freeTVars = this.freeTVars();
       LuaType result = this;
       for(VarType v : freeTVars) 
          result = new QuantifiedType(v, result);
       return result;
    }

    @Override
    public String fold() {
       if (this.equals(LuaType._Function))
          return "Function";
       else
          return this.toString();
    }

    @Override
    public boolean isNumericType() {
       return this.getParamTypes().isNumericType() && 
              this.getRetTypes().isNumericType();
    }
}
