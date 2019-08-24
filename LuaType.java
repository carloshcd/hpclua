/***
  LuaType.java

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
import java.util.Map;
import java.util.HashMap;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.Serializable;

public class LuaType extends Symbol implements Serializable, Type {

    private static final long serialVersionUID = 1L;
    
    int typeIndex;
 
    public static final int tBOTTOM = 0; 
    public static final int tNIL = 1;
    public static final int tBOOLEAN = 2;
    public static final int tSTRING = 3;
    public static final int tNUMBER = 4;
    public static final int tINT = 5;
    public static final int tLONG = 6;
    public static final int tSINGLE = 7;
    public static final int tDOUBLE = 8;
    public static final int tVAR = 9;
    public static final int tTHREAD = 10;
    public static final int tUSERDATA = 11;
    public static final int tTABLE = 12;
    public static final int tFUNCTION = 13;
    public static final int tUNION = 14;
    public static final int tSEQUENCE = 15;
    public static final int tANY = 16;
    public static final int tQUANT = 17;
    public static final int tOTHER = 18;

    public LuaType(int i) {
        super();
        this.typeIndex = i;
    }

    public LuaType(String n, int i) {
        super(n);
        this.typeIndex = i;
    }

    public int getTypeIndex() { 
        return typeIndex; 
    }
    
    public String toString() { 
        return getName();
    }
    
    public LuaType expand(Scope s) {
       return this;
    }
    
    public boolean equals(Object t) {
        if (t == this)
            return true;
        if (t == null) 
            return false; 
        if (!getClass().equals(t.getClass()))
            return false;
        LuaType other = (LuaType) t;
        return (this.typeIndex == other.getTypeIndex());
    }
    
    public int hashCode() {
       return tOTHER + this.typeIndex;
    }

    public boolean isBasicTypeTag() {
       switch (typeIndex) {
          case tNIL:
          case tBOOLEAN:
          case tSTRING:
          case tNUMBER:
          case tANY:
             return true;
          default:
             return false;
       }
    }
    
    public boolean isNumTypeTag() {
       switch (typeIndex) {
          case tINT:
          case tLONG:
          case tSINGLE:
          case tDOUBLE:
             return true;
          default:
             return false;
       }
    }

    public boolean isStructTypeTag() {
       switch (typeIndex) {
          case tTHREAD:
          case tUSERDATA:
          case tANY:
             return true;
          default:
             return false;
       }
    }
     
    public boolean isAbbrvTypeTag() {
       return (this.equals(_Bottom)   ||
               this.equals(_Integer)  ||
               this.equals(_Float)    ||
               this.equals(_Table)    || 
               this.equals(_Function) || 
               this.equals(_Object));
    }
    
    public boolean isTTag() {
       return (isBasicTypeTag()  || 
               isNumTypeTag()    ||
               isStructTypeTag() || 
               isAbbrvTypeTag());
    }
    
    public boolean subtype(LuaType t) {
       if (t.equals(_Any))                // TTop
          return true;
       else if (this.isTTag() && 
                this.equals(t))          // TCRefl
          return true;
       else if (this.equals(_Int) &&     // TInt
                t.equals(_Number))
          return true;
       else if (this.equals(_Long) &&    // TLong
                t.equals(_Number))
          return true;
       else if (this.equals(_Single) &&  // TSingle
                t.equals(_Number))
          return true; 
       else if (this.equals(_Double) &&  // TDouble
                t.equals(_Number))
          return true;
       else if (this.equals(_Int) &&     // TIL
                t.equals(_Long))
          return true;
       else if (this.equals(_Single) &&  // TSD
                t.equals(_Double))
          return true;
       else if (t instanceof UnionType)  // TUSup
          return ((UnionType) t).TUSup(this);
       return false;       
    }
    
    public LuaType subst(LuaType v, LuaType t) { 
      if (this.equals(v)) 
         return copy(t);
      else
         return this;
    }
    
    public LuaType subst(Scope s) {
       return copy(this);
    }
    
    public List<VarType> freeTVars() {
       return new ArrayList<VarType>();
    }
    
    public static List<VarType> freeTVars(Scope s) {
       List<VarType> freeTVars = new ArrayList<VarType>();
       Map<String,Symbol> m = s.getSymbols();
       for(Map.Entry<String, Symbol> me : m.entrySet()) {
          LuaType type = (LuaType) me.getValue().getType();
          List<VarType> ftv = type.freeTVars();
          freeTVars.addAll(ftv);
       }
       return freeTVars;
    }

    public static LuaType copy(LuaType orig) {
        LuaType obj = null;
        try {
        
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(orig);
            out.flush();
            out.close();

            ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bos.toByteArray()));
            obj = (LuaType) in.readObject();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        catch(ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return obj;
    }
    
    public static Scope delta(Scope s, VarType n, LuaType t) {
       String name = n.getVarName();
       Symbol symb = s.resolveName(name); 
       if (symb != null) {
          int line = symb.getLine();
          symb.getScope().defineName(new VariableSymbol(name, t, line));
       } else 
          s = null;
       return s;
    }      

    public static Scope extend(Scope s, VarType n, LuaType t) {
       Symbol symb = s.resolveName(n.getVarName());
       if (symb != null && symb.getType().equals(n))
          return s;
       else {
          List<VarType> tvars = t.freeTVars();
          if (tvars != null && tvars.contains(n))
             return null;
          else 
             return delta(s, n, t);
       }
    }

    public Scope unifiesWith (Scope s, LuaType t) {
       if (this.equals(_Any) || t.equals(_Any)) 
          return s;
       else
          if (t instanceof VarType || t instanceof UnionType)
             return t.unifiesWith(s, this);
          else 
             if (t.subtype(this) || this.subtype(t))
               return s;
             else 
                return null;
    }
    
    public static Scope unifiesLists (Scope s, List<LuaType> l1, List<LuaType> l2, boolean samesize) {
       Scope result = s; 
       LuaType obj1, obj2;
       if (samesize && l1.size() != l2.size())
          return null;
       for(int i=0;i<l1.size();i++) {
          obj1 = l1.get(i);
          if (i >= l2.size())
             obj2 = _Nil;
          else
             obj2 = l2.get(i);
          Scope s1 = obj1.unifiesWith(result, obj2);
          if (s1 != null)  
             result = s1;
          else
             return null;
       }
       return result;
    }
    
    public static LuaType expand(Scope s, String name) {
        boolean stop = false;
        LuaType type = LuaType._Bottom;
        while (!stop) {
           Symbol var = s.resolveName(name);
           if (var == null)
              stop = true;
           else {
              type = (LuaType) var.getType();
              if (type instanceof VarType && !type.equals(VarType._Unknown))
                 name = ((VarType) type).getVarName();
              else
                 stop = true;
           }
        }
        if (type.equals(VarType._Unknown))
           type = LuaType._Bottom;
        return type;
    }
    
    public static final LuaType _Nil =
        new LuaType("Nil", tNIL);
    public static final LuaType _Boolean =
        new LuaType("Boolean", tBOOLEAN);
    public static final LuaType _String =
        new LuaType("String", tSTRING);
    public static final LuaType _Number =
        new LuaType("Number", tNUMBER);
    public static final LuaType _Int =
        new LuaType("Int", tINT);
    public static final LuaType _Long =
        new LuaType("Long", tLONG);
    public static final LuaType _Single =
        new LuaType("Single", tSINGLE);
    public static final LuaType _Double =
        new LuaType("Double", tDOUBLE);
    public static final LuaType _Var =
        new LuaType("TVar", tVAR);
    public static final LuaType _Thread =
        new LuaType("Thread", tTHREAD);
    public static final LuaType _UserData =
        new LuaType("UserData", tUSERDATA);
    public static final LuaType _Any =
        new LuaType("Any", tANY);

    public static final LuaType _Union =
        new LuaType("Union", tUNION);
    public static final LuaType _Sequence =
        new LuaType("Sequence", tSEQUENCE);
    public static final LuaType _Quantified =
        new LuaType("Quantified", tQUANT);
        
    public static final UnionType _Bottom = 
        new UnionType (); 
    public static final TableType _Table = 
        new TableType (); 
    public static final FunctionType _Function = 
       new FunctionType (_Bottom, _Any);
    
    protected static final List<LuaType> _elemInteger = 
       new ArrayList<LuaType>() {
        private static final long serialVersionUID = 1L; {
        add(_Int); add(_Long); }};
    public static final UnionType _Integer = 
       new UnionType(new SequenceType (_elemInteger));
    
    protected static final List<LuaType> _elemFloat = 
       new ArrayList<LuaType>() {
        private static final long serialVersionUID = 1L; {
        add(_Single); add(_Double); }};
    public static final UnionType _Float = 
       new UnionType(new SequenceType(_elemFloat));
          
    protected static final List<LuaType> _elemObject =
       new ArrayList<LuaType>() {        
        private static final long serialVersionUID = 1L; {
        add(_Thread); add(_UserData); 
        add(_Table); add(_Function);}};
    public static final UnionType _Object = 
       new UnionType(new SequenceType(_elemObject));
          
    public static final Map<Integer,LuaType> _BuiltInTypes = 
        new HashMap<Integer,LuaType>() {
        private static final long serialVersionUID = 1L; {

        put(tANY, _Any); 
 
        put(tQUANT,_Quantified);
        put(tUNION,_Union);
        put(tFUNCTION,_Function);
        put(tTABLE,_Table);
        put(tQUANT,_Quantified);
        put(tSEQUENCE,_Sequence);
        put(tUSERDATA,_UserData);
        put(tTHREAD,_Thread);
        put(tVAR,_Var);

        put(tDOUBLE,_Double);
        put(tSINGLE,_Single);
        put(tLONG,_Long);
        put(tINT,_Int);
    
        put(tNUMBER,_Number);
        put(tSTRING,_String);
        put(tBOOLEAN,_Boolean); 
        put(tNIL,_Nil); 

        put(tBOTTOM,_Bottom); 
    }};
    
    public static boolean isInt(Long n) {
       return ((Integer.MIN_VALUE <= n) && (n <= Integer.MAX_VALUE));
    }
               
    public static boolean isSingle(Double n) {
       return ((Float.MIN_VALUE <= n) && (n <= Float.MAX_VALUE));
    }
}
