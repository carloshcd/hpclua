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
import java.util.ArrayList;

public class FunctionType extends LuaType {

    private static final long serialVersionUID = 1L;

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
           this.retTypes = new SequenceType(r);        
        this.setName(this.toString());
    }
    
    public SequenceType getParamTypes() {
        return paramTypes;
    }

    public SequenceType getRetTypes() {
       return retTypes;
    }
   
    @Override 
    public String toString() { 
       String result = "";
       if (paramTypes.getElements().size() == 1) 
          result = paramTypes.getElements().get(0).toString();
       else
          result = paramTypes.toString();
       result = result + " -> ";
       if (retTypes.getElements().size() == 1)
          result = result + retTypes.getElements().get(0).toString();
       else
          result = result + retTypes.toString();
       return result; 
    }
    
    @Override 
    public FunctionType expand(Scope s) { 
       paramTypes = paramTypes.expand(s);
       retTypes = retTypes.expand(s);
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
        return (this.paramTypes.equals(other.getParamTypes())) &&
               (this.retTypes.equals(other.getRetTypes()));
    }

    @Override
    public boolean subtype(LuaType t) {
       if (t.equals(LuaType._Any)) // TTop
          return true; 
       if (t instanceof FunctionType) { // TFunc
          FunctionType tp = (FunctionType) t;
          return (tp.getParamTypes().subtype((this.getParamTypes())) &&
                  this.getRetTypes().subtype(tp.getRetTypes()));
       } else if (t instanceof UnionType) // TUSup
          return ((UnionType) t).TUSup(this);
       return false;
    }
    
    @Override
    public LuaType subst(LuaType v, LuaType t) { 
       LuaType obj = this.getParamTypes();
       if (obj.equals(v))
          this.paramTypes = (SequenceType) LuaType.copy(t);
       else
          obj.subst(v,t);
       obj = this.getRetTypes();
       if (obj.equals(v))
          this.retTypes = (SequenceType) LuaType.copy(t);
       else
          obj.subst(v,t);
       return this;
    }
    
    
    @Override
    public LuaType subst(Scope s) { 
       SequenceType params = (SequenceType) this.getParamTypes().subst(s);
       SequenceType rets = (SequenceType) this.getRetTypes().subst(s);
       return new FunctionType(params, rets);
    }
    
    @Override 
    public List<VarType> freeTVars() {
       List<VarType> freeParamVars = this.paramTypes.freeTVars(); 
       List<VarType> freeRetVars = this.retTypes.freeTVars();
       for(VarType v : freeRetVars) { 
          if (!freeParamVars.contains(v))
             freeParamVars.add(v);
       }
       return freeParamVars;
    }
    
    @Override
    public Scope unifiesWith (Scope s, LuaType t) {
       if (t.equals(LuaType._Any) || 
           t instanceof VarType || 
           t instanceof UnionType)
          return t.unifiesWith(s, this);
       else 
          if (t instanceof FunctionType) {
             List<LuaType> l1 = this.getParamTypes().getElements();
             List<LuaType> l2 = ((FunctionType) t).getParamTypes().getElements();
             Scope s1 = LuaType.unifiesLists(s, l1, l2, false);
             l1 = this.getRetTypes().getElements();
             l2 = ((FunctionType) t).getRetTypes().getElements();
             Scope s2 = LuaType.unifiesLists(s1, l1, l2, false);
             // s1.mergeWith(s2);
             return s2;
          } else
             return null;
    }
    
    public LuaType bindFree() {
       List<VarType> freeTVars = this.freeTVars();
       LuaType result = this;
       for(VarType v : freeTVars) { // should reverse freeTVars before binding?
          result = new QuantifiedType(v, result);
       }
       return result;
    }
}
