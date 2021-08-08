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

    private static final long serialVersionUID = 11L;

    Map<LuaType,LuaType> elements;   

    public TableType() {
       super(LuaType.tTABLE);
       this.elements = new HashMap<LuaType,LuaType>();
    }
 
    public TableType(LuaType k, LuaType v) {
       super(LuaType.tTABLE);
       this.elements = new HashMap<LuaType,LuaType>();
       this.elements.put(k,v);
    }
 
    public TableType(Map<LuaType,LuaType> s) {
       super(LuaType.tTABLE);
       this.elements = s;
    }
    
    public Map<LuaType,LuaType> getElements() {
       return elements;
    }

    public void setElements(Map<LuaType,LuaType> t) { 
       this.elements = t;
    }

    public int size() {
       return this.getElements().size();
    }

    @Override 
    public String toString() { 
        String result = "{.}";
        if (this.getElements() != null) {
           Set<Map.Entry<LuaType,LuaType>> mes = this.getElements().entrySet();
           for (Map.Entry<LuaType,LuaType> me : mes) {
               if ("{.}".equals(result)) 
                  result = "";
               else
                  result = result + ",";
               result = result + me.getKey().fold() + "|->" + 
                                 me.getValue().fold(); 
           }
           if (!("{.}".equals(result)))
              result = "{"+result+"}";
        }
        if ("{.}".equals(result))
           return "Table";
        else 
           return result;
    }
   
    @Override 
    public TableType expand(Scope s) { 
        if (this.getElements() == null) 
           new Throwable().printStackTrace();
        else {
           Map<LuaType,LuaType> result = new HashMap<LuaType,LuaType>();
           Map<LuaType,LuaType> m = this.getElements();
           Set<Map.Entry<LuaType,LuaType>> mes = m.entrySet();
           for (Map.Entry<LuaType,LuaType> me : mes) {
               LuaType ok = me.getKey();
               LuaType nk = ok.expand(s);
               result.put(nk, me.getValue().expand(s));
           }
           this.setElements(result);
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
           return (this.getElements().equals(other.getElements()));
    }

    @Override
    public boolean subtype(LuaType t) {
       if (t.equals(LuaType._Any)) // TTop
          return true;
       if (t instanceof TableType) { // TTab
          Map<LuaType,LuaType> m = this.getElements();
          Map<LuaType,LuaType> mt = ((TableType) t).getElements();
          for (Map.Entry<LuaType,LuaType> mte : mt.entrySet())
             for (Map.Entry<LuaType,LuaType> me : m.entrySet())
                if ((me.getKey().equals(mte.getKey())) &&
                    (!(me.getValue().subtype(mte.getValue()))))
                      return false;
          return true;
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
       Map<LuaType,LuaType> result = new HashMap<LuaType,LuaType>();
       Set<Map.Entry<LuaType,LuaType>> mes = this.getElements().entrySet();
       for (Map.Entry<LuaType,LuaType> me : mes) {
          LuaType ok = me.getKey();
          LuaType ov = me.getValue();
          LuaType nk;
          if (ok.equals(t))
             nk = LuaType.copy(t);
          else
             nk = ok.subst(v,t);
          LuaType nv;
          if (ov.equals(t))
             nv = LuaType.copy(t);
          else 
             nv = ov.subst(v,t);
          result.put(nk,nv);
       }
       this.setElements(result);
       return this;
    }
   
    @Override
    public LuaType subst(Scope s) {
       Map<LuaType,LuaType> result = new HashMap<LuaType,LuaType>();
       Set<Map.Entry<LuaType,LuaType>> mes = this.getElements().entrySet();
       for (Map.Entry<LuaType,LuaType> me : mes) {
          LuaType ok = me.getKey();
          LuaType ov = me.getValue();
          LuaType nk = ok.subst(s);
          LuaType nv = ov.subst(s);
          if (!ok.equals(nk))
             result.put(nk,nv);
          else 
             if (!ov.equals(nv))
                result.put(ok,nv);
             else
                result.put(ok,ov);
       }
       this.setElements(result);
       return this;
    }
    
    @Override 
    public List<VarType> freeTVars() {
       List<VarType> freeTVars = new ArrayList<VarType>();
       Set<Map.Entry<LuaType,LuaType>> mes = this.getElements().entrySet();
       for (Map.Entry<LuaType,LuaType> me : mes) {
          List<VarType> oftv = me.getKey().freeTVars();
          for(VarType v : oftv) {
             if (!freeTVars.contains(v))
                freeTVars.add(v);
          }
          oftv = me.getValue().freeTVars();
          for(VarType v : oftv) {
             if (!freeTVars.contains(v))
                freeTVars.add(v);
          }
       }
       return freeTVars;
    }
    
    @Override
    public Scope unifiesWith (Scope s, LuaType t, int verb) { 
       if ( verb > 1 ) 
          System.out.printf("Unifying TableType: %s/%s\n", this, t);
       if (t.equals(LuaType._Any))
          return s;
       else if (t instanceof VarType ||
                t instanceof UnionType)
          return t.unifiesWith(s,this,verb);
       else 
          if (t instanceof TableType) {
             Map<LuaType,LuaType> m = this.getElements();
             Map<LuaType,LuaType> mt = ((TableType) t).getElements();
             if (m.keySet().size() == 0 || mt.keySet().size() == 0) 
                return s;
             else { 
                if (m.keySet().size() != mt.keySet().size()) 
                   return null;
                else { // Tab
                   Scope s1 = null;
                   Set<Map.Entry<LuaType,LuaType>> mtes = mt.entrySet();
                   for (Map.Entry<LuaType,LuaType> mte : mtes) {
                      Set<Map.Entry<LuaType,LuaType>> mes = m.entrySet();
                      for (Map.Entry<LuaType,LuaType> me : mes) {
                         Scope s2 = me.getKey().unifiesWith(s,mte.getKey(), 
                                                            verb);
                         if (s2 != null) {
                            Scope s3 = me.getValue().unifiesWith(s2, 
                                             mte.getValue(),verb);
                            if (s3 != null) {
                               if (s1 == null)
                                  s1 = s;
                               s1.mergeWith(s3);
                            } else
                               return null;
                         }
                      }
                   }
                   if (s1 != null && verb > 1) 
                      System.out.printf("Applying: Tab\n");
                   return s1;     
                }
             }
         } else
            return null;
    }
    
    public TableType mergeWith(TableType t) {
       Map<LuaType,LuaType> map = new HashMap<LuaType,LuaType>(t.getElements());
       Set<Map.Entry<LuaType,LuaType>> mtes = this.getElements().entrySet();
       for (Map.Entry<LuaType,LuaType> mte : mtes) {
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
             if (!(l.contains(vn)))
                l.add(vn);
             LuaType v;
             if (l.size() > 1)
                v = new UnionType(new SequenceType(l));
             else
                v = l.get(0);
             map.put(k,v);
          } else 
             map.put(k, vn);
       }
       return new TableType(map);
    }
   
    @Override 
    public LuaType select(Scope s, LuaType t, int verb) {
       List<LuaType> result = new ArrayList<LuaType>();
       Set<Map.Entry<LuaType,LuaType>> mtes = this.getElements().entrySet();
       for (Map.Entry<LuaType,LuaType> mte : mtes) {
          LuaType k = mte.getKey();
          if (k.unifiesWith(s,t,verb) != null ||
              k.equals(t))
             result.add(mte.getValue());
       }
       if (result.size() == 1) 
          return result.get(0);
       else 
          return new UnionType(new SequenceType (result));
    }

    @Override
    public String fold() {
       if (this.equals(LuaType._Table))
          return "Table";
       else
          return this.toString();
    }
    
    @Override
    public boolean hasJustNumericKeys() {
       Set<Map.Entry<LuaType,LuaType>> mtes = this.getElements().entrySet();
       for (Map.Entry<LuaType,LuaType> mte : mtes) {
          LuaType k = mte.getKey();
          if (!k.isTTag() || !k.isNumericType())
             return false;
       }
       return true;
    }
}
