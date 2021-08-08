/***
  LuaTypeSystem.java

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
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class LuaTypeSystem implements TypeSystem { 

    private static final long serialVersionUID = 15L;

    private Map<Integer,Type> types;
    private static int ndefault;
    private static int fdefault;
    private static int idefault;
    private static int strictness;

    public LuaTypeSystem(int n, int f, int i, int s) { 
       this.types = new HashMap<Integer,Type>(); 
        for (Map.Entry<Integer,LuaType> me : LuaType._BuiltInTypes.entrySet())
           this.defineType(me.getKey(), me.getValue());
        ndefault = n;
        fdefault = f;
        idefault = i;
        strictness = s;
    }

    public void defineType(Integer i, Type t) { 
       types.put(i,t); 
    }

    public Map<Integer,Type> getTypes() { 
       return types; 
    }

    public int getTypeIndex(Type t) { 
       for (int i=0;i<types.size();i++) 
          if (t.equals(types.get(i))) 
            return i;
      return 0;
    }
    
    public static int getIDefault() {
       return idefault;
    }

    public static int getFDefault() {
       return fdefault;
    }

    public static int getNDefault() {
       return ndefault;
    }

    public static int getStrictness() {
       return strictness;
    }

    protected static final String tVar0 = LuaLanguage.varPrefix + 
                                       (SymbolTable.firstGenName - 2);
    protected static final String tVar1 = LuaLanguage.varPrefix + 
                                       (SymbolTable.firstGenName - 1);
    protected static final String tVar2 = LuaLanguage.varPrefix + 
                                       (SymbolTable.firstGenName);
                                       
    protected static final VarType v0 = new VarType(tVar0);
    protected static final VarType v1 = new VarType(tVar1);
    protected static final VarType v2 = new VarType(tVar2);
    
    public static final BuiltInSymbol _false = 
       new BuiltInSymbol("false",LuaType._Boolean);
    public static final BuiltInSymbol _true = 
       new BuiltInSymbol("true",LuaType._Boolean);
    public static final BuiltInSymbol _nil = 
       new BuiltInSymbol("nil",LuaType._Nil);
    public static final BuiltInSymbol _empty = 
       new BuiltInSymbol("{}",LuaType._Table);
    public static final BuiltInSymbol _arg = 
       new BuiltInSymbol("arg", new TableType(LuaType._Int,LuaType._Any));
        
    protected static final List<LuaType> _paramTwoVarTypes = 
        new ArrayList<LuaType>() {
           private static final long serialVersionUID = 1501L; {
              add(v0); add(v1); }};
    protected static final LuaType _retBoolType = 
       LuaType._Boolean;
    protected static final QuantifiedType _compOpType = 
       (QuantifiedType) (new FunctionType (new SequenceType(_paramTwoVarTypes), 
                                              _retBoolType)).bindFree();
    protected static final List<LuaType> _paramTwoEqVarTypes =
       new ArrayList<LuaType>() {
          private static final long serialVersionUID = 1502L; {
             add(v0); add(v0); }};
    protected static final QuantifiedType _compEqOpType =
       (QuantifiedType) (new FunctionType (
                            new SequenceType(_paramTwoEqVarTypes),
                                             _retBoolType)).bindFree();
 
    public static final BuiltInSymbol _eq = 
       new BuiltInSymbol("==",_compOpType);
    public static final BuiltInSymbol _ieq = 
       new BuiltInSymbol("~=",_compOpType);
    public static final BuiltInSymbol _le = 
       new BuiltInSymbol("<=",_compEqOpType); 
    public static final BuiltInSymbol _ge = 
       new BuiltInSymbol(">=",_compEqOpType); 
    public static final BuiltInSymbol _lt = 
       new BuiltInSymbol("<",_compEqOpType);  
    public static final BuiltInSymbol _gt = 
       new BuiltInSymbol(">",_compEqOpType); 
      
    protected static final List<LuaType> _paramOneVarType = 
        new ArrayList<LuaType>() {
           private static final long serialVersionUID = 1503L; {
              add(LuaType._Any); }};
    protected static final FunctionType _unBoolOpType = 
       new FunctionType (new SequenceType(_paramOneVarType), 
                                          _retBoolType);       
      
    public static final BuiltInSymbol _not = 
       new BuiltInSymbol("not",_unBoolOpType);

    protected static final LuaType _retVarType = 
        new UnionType(new SequenceType(_paramTwoVarTypes));
    public static final QuantifiedType _binBoolOpType = 
       (QuantifiedType) (new FunctionType (new SequenceType(_paramTwoVarTypes), 
                                              _retVarType)).bindFree();

    public static final BuiltInSymbol _and = 
       new BuiltInSymbol("and",_binBoolOpType);
    public static final BuiltInSymbol _or = 
       new BuiltInSymbol("or",_binBoolOpType);

    protected static final List<LuaType> _paramOneStruTypes = 
        new ArrayList<LuaType>() {
           private static final long serialVersionUID = 1504L; {
              add(LuaType._Table);
              add(LuaType._String); }};
    protected static final LuaType _retIntType = LuaType._Int;
    public static final FunctionType _unStruOpType = 
       new FunctionType(new UnionType(new SequenceType(_paramOneStruTypes)), 
                        _retIntType);

    public static final BuiltInSymbol _len = 
       new BuiltInSymbol("#",_unStruOpType);

    protected static final List<LuaType> _StrNumTypes = 
        new ArrayList<LuaType>() {
           private static final long serialVersionUID = 1505L; {
              add(LuaType._String); 
              add(LuaType._Number); }};
    protected static final UnionType _paramUnStrNumTypes  = 
        new UnionType(new SequenceType(_StrNumTypes));

    protected static final List<LuaType> _paramTwoStrTypes = 
        new ArrayList<LuaType>() {
           private static final long serialVersionUID = 1506L; {
              add(_paramUnStrNumTypes); 
              add(_paramUnStrNumTypes); }};
    protected static final LuaType _retStrType = LuaType._String;
    public static final FunctionType _binStrOpType = 
       new FunctionType(new SequenceType(_paramTwoStrTypes), _retStrType);
        
    public static final BuiltInSymbol _concat = 
       new BuiltInSymbol("..",_binStrOpType);

    protected static final LuaType _retNumType = 
       ndefault==0?LuaType._Number:
       (ndefault==1?(fdefault==0?LuaType._Float:(
                     fdefault==1?LuaType._Double:LuaType._Single)):
                    (idefault==0?LuaType._Integer:(
                     idefault==1?LuaType._Long:LuaType._Int))); 
    
    public static final FunctionType _unNumOpType = 
       new FunctionType(new SequenceType(_paramUnStrNumTypes),_retNumType);

    public static final BuiltInSymbol _unm = 
       new BuiltInSymbol("-",_unNumOpType);

    protected static final List<LuaType> _BinStrNumTypes =
        new ArrayList<LuaType>() {
           private static final long serialVersionUID = 1507L; {
              add(_paramUnStrNumTypes); 
              add(_paramUnStrNumTypes); }};
    protected static final SequenceType _paramBinStrNumTypes = 
       new SequenceType(_BinStrNumTypes);
    public static final FunctionType _binNumOpType = 
       new FunctionType(_paramBinStrNumTypes, _retNumType);

    public static final BuiltInSymbol _add = 
       new BuiltInSymbol("+",_binNumOpType);
    public static final BuiltInSymbol _sub = 
       new BuiltInSymbol("-",_binNumOpType);
    public static final BuiltInSymbol _mul = 
       new BuiltInSymbol("*",_binNumOpType);
    public static final BuiltInSymbol _mod = 
       new BuiltInSymbol("%",_binNumOpType); 
    public static final BuiltInSymbol _idiv = 
       new BuiltInSymbol("//",_binNumOpType); 

    public static final BuiltInSymbol _div = 
       new BuiltInSymbol("/",_binNumOpType);
    public static final BuiltInSymbol _pow = 
       new BuiltInSymbol("^",_binNumOpType); 
       
    protected static final LuaType _retIntegerType = 
       LuaType._Integer;
    public static final FunctionType _unBitOpType = 
       new FunctionType(
          new SequenceType (_paramUnStrNumTypes),
                            _retIntegerType);

    public static final BuiltInSymbol _bnot = 
       new BuiltInSymbol("~", _unBitOpType);

    public static final FunctionType _binBitOpType = 
       new FunctionType(_paramBinStrNumTypes,_retIntegerType);

    public static final BuiltInSymbol _bxor = 
       new BuiltInSymbol("~", _binBitOpType);  
    public static final BuiltInSymbol _band = 
       new BuiltInSymbol("&", _binBitOpType); 
    public static final BuiltInSymbol _bor = 
       new BuiltInSymbol("|", _binBitOpType); 
    public static final BuiltInSymbol _shl = 
       new BuiltInSymbol("<<", _binBitOpType); 
    public static final BuiltInSymbol _shr = 
       new BuiltInSymbol(">>", _binBitOpType);

    public static final BuiltInSymbol _type =
       new BuiltInSymbol("type", 
          new FunctionType(LuaType._Any, LuaType._String));
    public static final BuiltInSymbol _print = 
       new BuiltInSymbol("print",
          new FunctionType(LuaType._Any, LuaType._Nil));
    public static final BuiltInSymbol _tonumber = 
       new BuiltInSymbol("tonumber",
          new FunctionType(LuaType._Any,
             new UnionType(new SequenceType(
                new ArrayList<LuaType>() {
                private static final long serialVersionUID = 1508L; {
                   add(LuaType._Nil); 
                   add(LuaType._Number); }}))));
    public static final BuiltInSymbol _tostring =
       new BuiltInSymbol("tostring", 
          new FunctionType(LuaType._Any, LuaType._String));
    public static final BuiltInSymbol _ipairs = 
       new BuiltInSymbol("ipairs", 
          (QuantifiedType)
          (new FunctionType(
             new TableType(LuaType._Int, v0),
             new SequenceType(
                new ArrayList<LuaType>() {
                private static final long serialVersionUID = 1509L; {
                   add(new FunctionType(
                          new SequenceType(
                             new ArrayList<LuaType>() {
                                private static final long 
                                   serialVersionUID = 1510L; {
                                      add(new TableType(LuaType._Int, v0));
                                      add(LuaType._Int); 
                                }}), 
                          new SequenceType (
                             new ArrayList<LuaType>() {
                                private static final long 
                                   serialVersionUID = 1511L; {
                                      add(LuaType._Int); 
                                      add(v0); 
                                }}))); 
                   add(new TableType(LuaType._Int, v0));
                   add(LuaType._Int); 
                }}))).bindFree()); 
   public static final BuiltInSymbol _pairs =
       new BuiltInSymbol("pairs",
          (QuantifiedType)
          (new FunctionType(
             new TableType(v0, v1),
             new SequenceType(
                new ArrayList<LuaType>() {
                private static final long serialVersionUID = 1512L; {
                   add(new FunctionType(
                          new SequenceType(
                             new ArrayList<LuaType>() {
                                private static final long 
                                   serialVersionUID = 1513L; {
                                      add(new TableType(v0, v1));
                                      add(v0); 
                                }}), 
                          new SequenceType (
                             new ArrayList<LuaType>() {
                                private static final long 
                                   serialVersionUID = 1514L; {
                                      add(v0); 
                                      add(v1); 
                                }})));
                   add(new TableType(v0, v1));
                   add(LuaType._Nil); 
                }}))).bindFree());
   public static final BuiltInSymbol _next =
       new BuiltInSymbol("next",
          (QuantifiedType)
          (new FunctionType(
             new SequenceType(
                new ArrayList<LuaType>() {
                   private static final long serialVersionUID = 1515L; {
                      add(new TableType(v0, v1));
                      add(new UnionType(new SequenceType(
                         new ArrayList<LuaType>() {
                           private static final long serialVersionUID = 1516L;{
                              add(v0); 
                              add(LuaType._Nil);
                           }})));
                   }}), 
             new SequenceType (
                new ArrayList<LuaType>() {
                   private static final long serialVersionUID = 1517L; {
                      add(v0); 
                      add(v1); 
                   }}))).bindFree());
    public static final List<Symbol> _BuiltInSymbols = 
        new ArrayList<Symbol>() {
        private static final long serialVersionUID = 1599L; {

        add(_nil);
        add(_false); 
        add(_true); 
        add(_empty); 

        add(_arg);

        add(_eq);
        add(_ieq);
        add(_le); 
        add(_ge); 
        add(_lt);  
        add(_gt); 
        
        add(_not);

        add(_and);
        add(_or);

        add(_len);

        add(_concat);

        add(_unm);

        add(_add);
        add(_sub);
        add(_mul);
        add(_div);
        add(_pow); 
        add(_mod); 
        add(_idiv); 

        add(_bnot);

        add(_bxor);  
        add(_band); 
        add(_bor); 
        add(_shl); 
        add(_shr);

        add(_type);
        add(_print);
        add(_tonumber);
        add(_tostring);
        add(_ipairs);
        add(_pairs);
        add(_next);
    }};

    public static final LuaType _mapType =
          (new FunctionType(
             new SequenceType(
                new ArrayList<LuaType>() {
                   private static final long serialVersionUID = 1516L; {
                   add(new FunctionType(v0, 
                      new UnionType(new SequenceType( 
                         new ArrayList<LuaType>() {
                            private static final long 
                               serialVersionUID = 1517L; {
                                  add(v1);
                                  add(LuaType._Nil);
                         }}))
                   ));
                   add(new TableType(v2, v0));
                }}),
             LuaType._Table)).bindFree();
}
