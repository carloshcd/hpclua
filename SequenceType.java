/***
  SequenceType.java

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

public class SequenceType extends LuaType {

    private static final long serialVersionUID = 9L;
    
    List<LuaType> elements;   

    public SequenceType() {
        super(LuaType.tSEQUENCE);
        this.elements = new ArrayList<LuaType>();
    }
    
    public SequenceType(LuaType t) {
        super(LuaType.tSEQUENCE);
        this.elements = new ArrayList<LuaType>();
        this.elements.add(t);
    }
    
    public SequenceType(List<LuaType> s) {
        super(LuaType.tSEQUENCE);
        this.elements = s;
    }
    
    public List<LuaType> getElements() {
        return this.elements;
    }

    public int size() {
        return this.getElements().size();
    }

    @Override 
    public String toString() { 
        String result = "";
        if (this.getElements() == null) 
           new Throwable().printStackTrace();
        else if (this.size() == 0)
           return "(.)";
        else {
           int nel = this.size();
           for (int i=0;i<nel;i++) {
              if (!"".equals(result))
                 result = result + ",";
              result = result + this.getElements().get(i).fold();
           }
        }
        return "("+result+")";
    }

    @Override 
    public SequenceType expand(Scope s) { 
        if (this.getElements() == null) 
           new Throwable().printStackTrace();
        else { 
           int nel = this.size();
           for (int i=0;i<nel;i++) 
              this.getElements().set(i,this.getElements().get(i).expand(s));
        }
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
        SequenceType other = (SequenceType) t;
        if (this.size() != other.size())
           return false;
        else {
           int nel = this.size();
           for(int i=0;i<nel;i++)
              if (!(this.getElements().get(i).equals(
                       other.getElements().get(i))))
                 return false;
        }
        return true;
    }
   
    @Override
    public boolean subtype(LuaType t) {
       if (t.equals(LuaType._Any)) // TTop
          return true;
       if (t instanceof SequenceType) { // TSeq 
          SequenceType tp = (SequenceType) t;
          int nel1 = this.size();
          if (nel1 == 1 && this.getElements().get(0) instanceof SequenceType) 
             return this.getElements().get(0).subtype(t);
          else {
             int nel2 = tp.size();
             if (nel2 == 1 && tp.getElements().get(0) instanceof SequenceType) 
                return this.subtype(tp.getElements().get(0));
             else {
                for (int i=0;i<nel1;i++) {
                   LuaType elem1 = this.getElements().get(i);
                   LuaType elem2 = tp.getElements().get(i);
                   if (!elem1.subtype(elem2)) 
                      return false;
                }
                return true; 
             }
          }
       } else if (t instanceof UnionType) // TUSup
          return ((UnionType) t).TUSup(this);
       return false;
    }
    
    public boolean TSeq(LuaType t) { // Unary case
       List<LuaType> te = this.getElements();
       if (te != null) {
          int nel = te.size();
          if (nel > 0) {
             LuaType elem1 = this.getElements().get(0);
             return t.subtype(elem1);
          }
       }
       return false;
    }

    @Override
    public LuaType subst(LuaType v, LuaType t) { 
       int nel = this.size();
       for (int i=0;i<nel;i++) {
          LuaType obj = this.getElements().get(i);
          if (obj.equals(v))
             this.getElements().set(i,LuaType.copy(t));
          else
             obj.subst(v,t);
       }
       return this;
    }
         
    @Override
    public LuaType subst(Scope s) { 
       int nel = this.size();
       for (int i=0;i<nel;i++) {
          LuaType old = this.getElements().get(i);
          this.getElements().set(i,old.subst(s));
       }
       return this;
    }
         
    @Override 
    public List<VarType> freeTVars() {
       List<VarType> freeElemVars = new ArrayList<VarType>();
       for(LuaType elem : this.getElements()) { 
          List<VarType> e = elem.freeTVars();
          for(VarType v : e) {
             if (!freeElemVars.contains(v))
                freeElemVars.add(v);
          }
       }
       return freeElemVars;
    }
    
    @Override
    public Scope unifiesWith(Scope s, LuaType t, int verb) { 
       if ( verb > 1 )
          System.out.printf("Unifying SequenceType: %s/%s\n", this, t);
       if (t.equals(LuaType._Any))
          return s;
       else if (t instanceof VarType || 
                t instanceof UnionType)
          return t.unifiesWith(s,this,verb);
       else 
          if (t instanceof SequenceType) { // Seq
             List<LuaType> elems1 = this.getElements();
             int nel1 = elems1.size();
             List<LuaType> elems2 = ((SequenceType) t).getElements();
             int nel2 = elems2.size();
             Scope s1 = LuaType.unifiesLists(s,elems1,elems2,verb);
             if (s1 != null && verb > 1) 
                System.out.printf("Applying: Seq\n");
             return s1;
          } else
             return null;
    }

    @Override
    public LuaType select(Scope s, LuaType t, int verb) {
       LuaType result = LuaType._Bottom;
       if (t.subtype(LuaType._Number)) {
          List<LuaType> contents =  new ArrayList<LuaType>();
          int nel = this.size(); 
          for (int i=0;i<nel;i++) {
             LuaType v = this.getElements().get(i);
             contents.add(v);
          }
          result = new UnionType (new SequenceType (contents));
       }
       return result;
    }

    public void mergeWith(SequenceType t) {
       List<LuaType> newElems = t.getElements();
       if (newElems != null) {
          List<LuaType> oldElems = this.getElements();
          oldElems.addAll(newElems);
       }
    }

    @Override
    public String fold() {
       return this.toString();
    }

    public LuaType unpack() {
       LuaType result = this;
       while ((result instanceof SequenceType) &&
              (((SequenceType) result).getElements().size() == 1)) 
          result = ((SequenceType) result).getElements().get(0);
       return result;
    }
    
    @Override
    public boolean isNumericType() {
       for(LuaType elem : this.getElements()) { 
          if (!elem.isNumericType()) 
             return false;
       }
       return true;
    }
}
