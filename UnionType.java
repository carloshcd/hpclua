/***
  UnionType.java

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

public class UnionType extends LuaType {

    private static final long serialVersionUID = 1L;
    
    SequenceType elements;   
    
    public UnionType() {
       super(LuaType.tUNION);
       this.elements = new SequenceType();
       this.setName(this.toString());
    }

    public UnionType(LuaType t) {
       super(LuaType.tUNION);
       if (t instanceof SequenceType)
          this.elements = (SequenceType) t;
       else {
          List<LuaType> temp = new ArrayList<LuaType>();
          temp.add(t);
          this.elements = new SequenceType(temp);
       }
    }
     
    public SequenceType getElements() {
        return elements;
    }

    public int size() {
        return elements.size();
    }
   
    @Override 
    public String toString() {
       return "U "+elements.toString();
    }
 
    @Override 
    public UnionType expand(Scope s) { 
       elements = elements.expand(s);
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
        UnionType other = (UnionType) t;
        if (this.size() != other.size())
           return false;
        else 
           if (this.size() == 0)
              return true;
           else
              return (this.subtype(other) && other.subtype(this));
    }
    
    @Override
    public boolean subtype(LuaType t) {
       if (t.equals(LuaType._Any)) // TTop
          return true;
       else if (this.equals(LuaType._Bottom)) // TBot
          return true;
       
       // TUSub
       SequenceType te = this.elements;
       if (te != null) {
          int nel = te.size();
          if (nel == 0)
             return true;
          else {
             for (int i=0;i<nel;i++) {
                if (!(te.getElements().get(i).subtype(t)))
                   return false;
             }
             return true;
          }
       } else 
          return false;
    }
   
    public boolean TUSup(LuaType t) { // TUSup
       SequenceType te = this.getElements();
       if (te != null) {
          for (int i=0;i<te.size();i++) {
             if (t.subtype(te.getElements().get(i)))
                return true;
          }
          return false;
       } else
          return false;
    }
 
    @Override
    public LuaType subst(LuaType v, LuaType t) { 
       LuaType obj = this.getElements();
       if (obj.equals(v))
          this.elements = (SequenceType) LuaType.copy(t);
       else
          obj.subst(v,t);
       return this;
    }
    
    @Override
    public LuaType subst(Scope s) { 
       return new UnionType(this.getElements().subst(s));
    }
    
    @Override 
    public List<VarType> freeTVars() {
       return this.elements.freeTVars();
    }
    
    @Override
    public Scope unifiesWith (Scope s, LuaType t) { 
       if (t.equals(LuaType._Any) || t instanceof VarType)
          return t.unifiesWith(s, this);
       else
          if (t instanceof UnionType) {
             if (this.equals(LuaType._Bottom) &&
                 t.equals(LuaType._Bottom))
                return s;
             List<Scope> results = new ArrayList<Scope>();
             Scope s1;
             int base = 0;
             for(LuaType t1 : this.getElements().getElements()) {
                s1 = null;
                boolean tbrt1 = (t1 instanceof VarType && 
                                 ((VarType) t1).toBeResolved(s));
                boolean tbrt2 = false;
                SequenceType seq = ((UnionType) t).getElements();
                int i;
                for(i=base;i<seq.size();i++) {
                   LuaType t2 = seq.getElements().get(i);
                   tbrt2 = (t2 instanceof VarType &&
                            ((VarType) t2).toBeResolved(s));
                   Scope s2 = t1.unifiesWith(s, t2);
                   if (s2 != null) {
                      if (s1 == null)
                         s1 = s;
                      s1.mergeWith(s2);
                      if (tbrt1 || tbrt2)
                         break;
                   }
                }
                if (s1 != null) {
                   results.add(s1);
                   if (tbrt1 || tbrt2)
                      base = i + 1;
                }
             }
             s1 = null;
             for(Scope r : results)
                if (s1 == null)
                   s1 = r;
                else
                   s1.mergeWith(r);
             return s1;
          } else {
             Scope s1 = null;
             for(LuaType t1 : this.getElements().getElements()) {
                Scope s2 = t1.unifiesWith(s, t);
                if (s2 != null) {
                   if (s1 == null)
                      s1 = s;
                   s1.mergeWith(s2);
                }
             }
             return s1;
          }
    }
    
    public void mergeWith(UnionType t) {
       List<LuaType> newElems = t.getElements().getElements();
       List<LuaType> oldElems = this.getElements().getElements();
       if (newElems != null)
          oldElems.addAll(newElems);
    }

    private String fold() {
       if (this.equals(_Bottom))
          return "Bottom";
       else if (this.equals(_Integer))
          return "Integer";
       else if (this.equals(_Float))
          return "Float";
       else if (this.equals(_Table))
          return "Table";
       else if (this.equals(_Function))
          return "Function";
       else if (this.equals(_Object))
          return "Object";
       else
          return this.toString();
    }
}
