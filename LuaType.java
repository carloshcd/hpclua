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
import java.util.Set;

public class LuaType extends Symbol implements Serializable, Type {

    private static final long serialVersionUID = 7L;
    
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

    @Override
    public int getTypeIndex() { 
        return this.typeIndex; 
    }
   
    @Override 
    public String toString() {
       String name = "";
       switch (this.getTypeIndex()) {
          case tNIL: name = "Nil"; break;
          case tBOOLEAN: name = "Boolean"; break;
          case tSTRING: name = "String"; break;
          case tNUMBER: name = "Number"; break;
          case tINT: name = "Int"; break;
          case tLONG: name = "Long"; break;
          case tSINGLE: name = "Single"; break;
          case tDOUBLE: name = "Double"; break;
          case tVAR: name = "TVar"; break;
          case tTHREAD: name = "Thread"; break;
          case tUSERDATA: name = "UserData"; break;
          case tANY: name = "Any"; break;
          case tUNION: name = "Union"; break;
          case tSEQUENCE: name = "Sequence"; break;
          case tQUANT: name = "Quantified"; break;
       }
       return name;
    }
    
    public boolean equals(Object t) {
        if (t == this)
            return true;
        if (t == null) 
            return false; 
        if (!getClass().equals(t.getClass()))
            return false;
        LuaType other = (LuaType) t;
        return (this.getTypeIndex() == other.getTypeIndex());
    }
   
    @Override 
    public int hashCode() {
       return tOTHER + this.getTypeIndex();
    }

    public LuaType expand(Scope s) {
       return this;
    }

    public boolean isBasicTypeTag() {
       switch (this.getTypeIndex()) {
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
       switch (this.getTypeIndex()) {
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
       switch (this.getTypeIndex()) {
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
       if (t.equals(LuaType._Any)) // TTop
          return true;
       else if (this.isTTag() && 
                this.equals(t)) // TCRefl
          return true;
       else if (this.equals(_Int) && // TInt
                t.equals(_Number))
          return true;
       else if (this.equals(_Long) && // TLong
                t.equals(_Number))
          return true;
       else if (this.equals(_Single) && // TSingle
                t.equals(_Number))
          return true; 
       else if (this.equals(_Double) && // TDouble
                t.equals(_Number))
          return true;
       else if (this.equals(_Int) && // TIL
                t.equals(_Long))
          return true;
       else if (this.equals(_Single) && // TSD
                t.equals(_Double))
          return true;
       else if (t instanceof SequenceType) // TSeq
          return ((SequenceType) t).TSeq(this);
       else if (t instanceof UnionType) // TUSup
          return ((UnionType) t).TUSup(this);
       return false;       
    }
    
    public LuaType subst(LuaType v, LuaType t) { 
      if (this.equals(v)) 
         return LuaType.copy(t);
      else
         return this;
    }
    
    public LuaType subst(Scope s) {
       return this;
    }
    
    public List<VarType> freeTVars() {
       return new ArrayList<VarType>();
    }
    
    public static List<VarType> freeTVars(Scope s) {
       List<VarType> freeTVars = new ArrayList<VarType>();
       Set<Map.Entry<String,Symbol>> mes = s.getSymbols().entrySet();
       for(Map.Entry<String, Symbol> me : mes) {
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
            in.close();
            bos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return obj;
    }
    
    public static Scope delta(Scope s, VarType n, LuaType t, int verb) { 
       if ( verb > 1 ) 
          System.out.printf("Entering delta()\n");
       String name = n.getVarName();
       Symbol symb = s.resolveName(name); 
       if (symb != null) 
          symb.getScope().defineName(new VariableSymbol(name, t, 
                                            symb.getLine()), verb);
       return s;
    }      

    public static Scope extend(Scope s, VarType n, LuaType t, int verb) {
       if ( verb > 1 )
          System.out.printf("Entering extend()\n");
       Symbol symb = s.resolveName(n.getVarName());
       if (symb != null && symb.getType().equals(t)) { 
          System.out.printf("There\n");
          return s;
       } else {
          List<VarType> tvars = t.freeTVars();
          if (tvars != null && tvars.contains(n)) 
             return null;
          else 
             return delta(s, n, t, verb);
       }
    }

    public Scope unifiesWith(Scope s, LuaType t, int verb) {
       if ( verb > 1 )
          System.out.printf("Unifying LuaType: %s/%s\n", this, t);
       if (t.equals(LuaType._Any) || 
           this.equals(LuaType._Any)) 
          return s;
       else
          if (t instanceof VarType || t instanceof UnionType)
             return t.unifiesWith(s,this,verb);
          else 
             if (t.subtype(this) || this.subtype(t)) 
               return s;
             else 
                return null;
    }

    public static Scope unifiesLists(Scope s,
                                     List<LuaType> l1, List<LuaType> l2, 
                                     int verb) {
       if ( verb > 1 ) 
          System.out.printf("Unifying Lists: %s/%s\n", l1, l2);
       Scope result = s;
       LuaType obj1;
       LuaType obj2;
       int nel = l1.size();
       for(int i=0;i<nel;i++) {
          obj1 = l1.get(i);
          if (i >= l2.size())
             obj2 = _Nil;
          else
             obj2 = l2.get(i);
          if ( verb > 1 ) 
             System.out.printf("Unifying Itens [%d/%d]:\n", i+1, l1.size());
          Scope s1 = obj1.unifiesWith(result,obj2,verb);
          if (s1 == null) { 
             result = null;
             break;
          } else
             result = s1;
       }
       return result;
    }
    
    public static LuaType expand(Scope s, LuaType type) {
        LuaType obj = type;
        while (obj instanceof VarType) {
           Symbol symb = s.resolveName(((VarType) obj).getVarName());
           if (symb != null) {
              LuaType ntype = (LuaType) symb.getType();
              if (ntype == null || ntype.equals(VarType._Unknown))
                 break;
              else
                 obj = ntype;
           } else
              break;
        }
        return obj;
    }
    
    public static final LuaType _Nil =
        new LuaType(tNIL);
    public static final LuaType _Boolean =
        new LuaType(tBOOLEAN);
    public static final LuaType _String =
        new LuaType(tSTRING);
    public static final LuaType _Number =
        new LuaType(tNUMBER);
    public static final LuaType _Int =
        new LuaType(tINT);
    public static final LuaType _Long =
        new LuaType(tLONG);
    public static final LuaType _Single =
        new LuaType(tSINGLE);
    public static final LuaType _Double =
        new LuaType(tDOUBLE);
    public static final LuaType _Var =
        new LuaType(tVAR);
    public static final LuaType _Thread =
        new LuaType(tTHREAD);
    public static final LuaType _UserData =
        new LuaType(tUSERDATA);
    public static final LuaType _Any =
        new LuaType(tANY);
    public static final LuaType _Union =
        new LuaType(tUNION);
    public static final LuaType _Sequence =
        new LuaType(tSEQUENCE);
    public static final LuaType _Quantified =
        new LuaType(tQUANT);
        
    public static final UnionType _Bottom = 
        new UnionType (); 
    public static final TableType _Table = 
        new TableType (); 
    public static final FunctionType _Function = 
       new FunctionType (_Bottom, _Any);
    public static final SequenceType _Sequence1 = 
       new SequenceType (
          new ArrayList<LuaType>() {
             private static final long serialVersionUID = 701L; {
             add(_Any); add(_Sequence1); }});
    
    protected static final List<LuaType> _elemInteger = 
       new ArrayList<LuaType>() {
        private static final long serialVersionUID = 702L; {
        add(_Int); add(_Long); }};
    public static final UnionType _Integer = 
       new UnionType(new SequenceType (_elemInteger));
    
    protected static final List<LuaType> _elemFloat = 
       new ArrayList<LuaType>() {
        private static final long serialVersionUID = 703L; {
        add(_Single); add(_Double); }};
    public static final UnionType _Float = 
       new UnionType(new SequenceType(_elemFloat));
          
    protected static final List<LuaType> _elemObject =
       new ArrayList<LuaType>() {        
        private static final long serialVersionUID = 704L; {
        add(_Thread); add(_UserData); 
        add(_Table); add(_Function);}};
    public static final UnionType _Object = 
       new UnionType(new SequenceType(_elemObject));
          
    public static final Map<Integer,LuaType> _BuiltInTypes = 
        new HashMap<Integer,LuaType>() {
        private static final long serialVersionUID = 705L; {

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

    public LuaType select(Scope s, LuaType t, int vetb) {
       if (this.equals(_String) && t.subtype(_Number))
          return _Int;
       else
          return _Bottom;
    } 

    public static boolean isSelectable(LuaType t) {
        return (t instanceof TableType ||
                t instanceof SequenceType ||
                (t instanceof LuaType && 
                 t.equals(LuaType._String)));
    }

    public String fold() {
       return this.toString();
    }
    
    public boolean isBasicNumericType() {
       return this.isNumTypeTag() || 
              this.equals(_Number);
    }
    
    public boolean isNumericType() {
       return this.isBasicNumericType();
    }
    
    public boolean hasJustNumericKeys() {
       return false;
    }
    
    public LuaType mostGeneralNumericType() {
       if (this.isBasicNumericType())
          return this;
       else
          return null;
    }
}
