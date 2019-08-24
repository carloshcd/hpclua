/***
  TableType.java

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
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class TableType extends LuaType {

    private static final long serialVersionUID = 1L;

    Map<LuaType,LuaType> elements;   

    public TableType() {
       super(LuaType.tTABLE);
       this.elements = new HashMap<LuaType,LuaType>();
       this.setName(this.toString());
    }
 
    public TableType(LuaType k, LuaType v) {
       super(LuaType.tTABLE);
       this.elements = new HashMap<LuaType,LuaType>();
       this.elements.put(k,v);
       this.setName(this.toString());
    }
 
    public TableType(Map<LuaType,LuaType> s) {
        super(LuaType.tTABLE);
        this.elements = s;
        this.setName(this.toString());
    }
    
    public Map<LuaType,LuaType> getElements() {
        return elements;
    }

    public int size() {
       return elements.size();
    }

    @Override 
    public String toString() { 
        if (elements == null) 
           return "{.}";
        else {
           String result = ".";
           Map<LuaType,LuaType> m = this.elements;
           for (Map.Entry<LuaType,LuaType> me : m.entrySet()) {
               if (!result.equals(".")) 
                  result = result + ",";
               else
                  result = "";
               result = result + me.getKey().toString() + "|->" + me.getValue().toString(); 
           }
           return "{"+result+"}";
        }
    }
   
    @Override 
    public TableType expand(Scope s) { 
        if (elements == null) 
           new Throwable().printStackTrace();
        else {
           Map<LuaType,LuaType> m = this.elements;
           List<LuaType> remove = new ArrayList<LuaType>();
           for (Map.Entry<LuaType,LuaType> me : m.entrySet()) {
               LuaType newval = me.getKey().expand(s);
               if (!me.getKey().equals(newval))
                  remove.add(me.getKey());
               m.put(newval, me.getValue().expand(s));
           }
           for (LuaType k : remove)
              m.remove(k);
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
        TableType other = (TableType) t;
        if (this.size() != other.size())
           return false;
        else
           return (this.elements.equals(other.getElements()));
    }
    
    @Override
    public boolean subtype(LuaType t) {
       if (t.equals(LuaType._Any)) // TTop
          return true;
       if (t instanceof TableType) { // TTab
          Map<LuaType,LuaType> m = this.elements;
          Map<LuaType,LuaType> mt = ((TableType) t).getElements();
          for (Map.Entry<LuaType,LuaType> mte : mt.entrySet())
             for (Map.Entry<LuaType,LuaType> me : m.entrySet())
                if (me.getKey().equals(mte.getKey()))
                   if (!(me.getValue().subtype(mte.getValue())))
                      return false;
          return true;
      } else if (t instanceof UnionType) // TUSup
         return ((UnionType) t).TUSup(this);
      return false;
   }
   
    @Override
    public LuaType subst(LuaType v, LuaType t) { 
       Map<LuaType,LuaType> m = this.elements;
       List<LuaType> remove = new ArrayList<LuaType>();
       for (Map.Entry<LuaType,LuaType> me : m.entrySet()) {
          LuaType obj = me.getKey();
          if (obj.equals(v)) {
             remove.add(obj);
             m.put(LuaType.copy(t),me.getValue());
          } else
             obj.subst(v,t);
          obj = me.getValue();
          if (obj.equals(v)) 
             me.setValue(LuaType.copy(t));
          else
             obj.subst(v,t);
       }
       for (LuaType k : remove)
          m.remove(k);
       return this;
    }
   
    @Override
    public LuaType subst(Scope s) {
       Map<LuaType,LuaType> tab = new HashMap<LuaType,LuaType>();
       Map<LuaType,LuaType> m = this.elements;
       for (Map.Entry<LuaType,LuaType> me : m.entrySet()) 
          tab.put(me.getKey().subst(s), me.getValue().subst(s));
       return new TableType(tab);
    }
    
    @Override 
    public List<VarType> freeTVars() {
       List<VarType> freeTVars = new ArrayList<VarType>();
       Map<LuaType,LuaType> m = this.elements;
       for (Map.Entry<LuaType,LuaType> me : m.entrySet()) {
          for(VarType v : me.getKey().freeTVars()) {
             if (!freeTVars.contains(v))
                freeTVars.add(v);
          }
          for(VarType v : me.getValue().freeTVars()) {
             if (!freeTVars.contains(v))
                freeTVars.add(v);
          }
       }
       return freeTVars;
    }
    
    @Override
    public Scope unifiesWith (Scope s, LuaType t) { 
       if (t.equals(LuaType._Any) || 
           t instanceof VarType ||
           t instanceof UnionType)
          return t.unifiesWith(s, this);
       else // Tab 
          if (t instanceof TableType) {
             Map<LuaType,LuaType> m = this.elements;
             Map<LuaType,LuaType> mt = ((TableType) t).getElements();
             if (m.keySet().size() == 0 || mt.keySet().size() == 0)
                return s;
             else { 
                if (m.keySet().size() != mt.keySet().size())
                   return null;
                else {
                   Scope s1 = null;
                   for (Map.Entry<LuaType,LuaType> mte : mt.entrySet())
                      for (Map.Entry<LuaType,LuaType> me : m.entrySet())
                         if (me.getKey().equals(mte.getKey())) {
                            Scope s2 = me.getValue().unifiesWith(s, mte.getValue());
                            if (s2 != null) {
                               if (s1 == null)
                                  s1 = s;
                               s1.mergeWith(s2);
                            } else
                               return null;
                         }
                   return s1;     
                }
             }
         } else
            return null;
    }
    
    public TableType mergeWith(TableType t) {
       Map<LuaType,LuaType> map = new HashMap<LuaType,LuaType>(this.elements);
       Map<LuaType,LuaType> mt = t.getElements();
       for (Map.Entry<LuaType,LuaType> mte : mt.entrySet()) {
          LuaType k = mte.getKey();
          LuaType vn = mte.getValue();
          if (map.containsKey(k)) {
             LuaType vo = map.get(k);
             List<LuaType> l = new ArrayList<LuaType>();
             if (vo instanceof UnionType) {
                List<LuaType> o = ((UnionType) vo).getElements().getElements();
                l.addAll(o);
             } else 
                l.add(vo);
             l.add(vn);
             UnionType v = new UnionType(new SequenceType(l));
             map.put(k,v);
          } else 
             map.put(k, vn);
       }
       return new TableType(map);
    }
    
    public LuaType select(Scope s, LuaType t) {
       List<LuaType> result = new ArrayList<LuaType>();
       Map<LuaType,LuaType> mt = this.elements;
       for (Map.Entry<LuaType,LuaType> mte : mt.entrySet()) {
          LuaType k = mte.getKey();
          if (k.unifiesWith(s, t) != null)
             result.add(mte.getValue());
       }
       if (result.size() == 1) 
          return result.get(0);
       else 
          return new UnionType(new SequenceType (result));
    }
}
