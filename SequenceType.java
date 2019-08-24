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

    private static final long serialVersionUID = 1L;
    
    List<LuaType> elements;   

    public SequenceType() {
        super(LuaType.tSEQUENCE);
        this.elements = new ArrayList<LuaType>();
        this.setName(this.toString());
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
        return elements;
    }

    public int size() {
        return elements.size();
    }

    @Override 
    public String toString() { 
        String result = "";
        if (elements == null) 
           new Throwable().printStackTrace();
        else if (this.size() == 0)
           return "(.)";
        else {
           for (int i=0;i<this.size();i++) {
              if (!result.equals(""))
                 result = result + ",";
              result = result + elements.get(i).toString();
           }
        }
        return "("+result+")";
    }

    @Override 
    public SequenceType expand(Scope s) { 
        if (elements == null) 
           new Throwable().printStackTrace();
        else 
           for (int i=0;i<this.size();i++) 
              elements.set(i,elements.get(i).expand(s));
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
        else
           for(int i=0;i<this.size();i++)
              if (!(this.getElements().get(i).equals(
                       other.getElements().get(i))))
                 return false;
        return true;
    }
    
    @Override
    public boolean subtype(LuaType t) {
       if (t.equals(LuaType._Any)) // TTop
          return true;
       if (t instanceof SequenceType) { // TSeq
          SequenceType tp = (SequenceType) t;
          for (int i=0;i<this.size();i++) {
             if (!(this.getElements().get(i).subtype(tp.getElements().get(i)))) 
                return false;
          }
       } else if (t instanceof UnionType) // TUSup
          return ((UnionType) t).TUSup(this);
       return true;
    }
    
    @Override
    public LuaType subst(LuaType v, LuaType t) { 
       for (int i=0;i<this.size();i++) {
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
       LuaType obj;
       List<LuaType> elems = new ArrayList<LuaType>();
       for (int i=0;i<this.size();i++) {
          obj = this.getElements().get(i).subst(s);
          elems.add(obj);
       }
       return new SequenceType(elems);
    }
         
 
    @Override 
    public List<VarType> freeTVars() {
       List<VarType> freeElemVars = new ArrayList<VarType>();
       for(LuaType elem : this.elements) { 
          List<VarType> e = elem.freeTVars();
          for(VarType v : e) {
             if (!freeElemVars.contains(v))
                freeElemVars.add(v);
          }
       }
       return freeElemVars;
    }
    
    @Override
    public Scope unifiesWith (Scope s, LuaType t) { 
       if (t.equals(LuaType._Any) || 
           t instanceof VarType || 
           t instanceof UnionType)
          return t.unifiesWith(s, this);
       else // Seq 
          if (t instanceof SequenceType) {
             List<LuaType> l1 = this.getElements();
             List<LuaType> l2 = ((SequenceType) t).getElements();
             Scope s1 = LuaType.unifiesLists(s, l1, l2, true);
             return s1;
          } else
             return null;
    }
}
