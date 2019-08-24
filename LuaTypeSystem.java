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

    private Map<Integer,Type> types;
    private static int ndefault;
    private static int fdefault;
    private static int idefault;

    public LuaTypeSystem(int n, int f, int i) { 
       this.types = new HashMap<Integer,Type>(); 
        for (Map.Entry<Integer,LuaType> me : LuaType._BuiltInTypes.entrySet())
           this.defineType(me.getKey(), me.getValue());
        ndefault = n;
        fdefault = f;
        idefault = i;
    }

    public void defineType(Integer i, Type t) { types.put(i,t); }
    public Map<Integer,Type> getTypes() { return types; }
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

    protected static final String tVar1 = LuaLanguage.varPrefix + 
                                       (SymbolTable.firstGenName - 1);
    protected static final String tVar2 = LuaLanguage.varPrefix + 
                                       SymbolTable.firstGenName;
    protected static final VarType v1 = new VarType(tVar1);
    protected static final VarType v2 = new VarType(tVar2);
    public static final QuantifiedType _genericFunctionType = 
       new QuantifiedType(v1, new QuantifiedType(v2, new FunctionType(v1, v2)));
    
    public static final BuiltInSymbol _false = 
       new BuiltInSymbol("false",LuaType._Boolean);
    public static final BuiltInSymbol _true = 
       new BuiltInSymbol("true",LuaType._Boolean);
    public static final BuiltInSymbol _nil = 
       new BuiltInSymbol("nil",LuaType._Nil);
    public static final BuiltInSymbol _empty = 
       new BuiltInSymbol("{}",LuaType._Table);
        
    protected static final List<LuaType> _paramTwoVarTypes = 
        new ArrayList<LuaType>() {
        private static final long serialVersionUID = 1L; {
        add(v1); add(v2); }};
    protected static final LuaType _retBoolType = 
       LuaType._Boolean;
    protected static final QuantifiedType _compOpType = 
       (QuantifiedType) (new FunctionType (new SequenceType(_paramTwoVarTypes), 
                                              _retBoolType)).bindFree();
    
    public static final BuiltInSymbol _eq = 
       new BuiltInSymbol("==",_compOpType);
    public static final BuiltInSymbol _ieq = 
       new BuiltInSymbol("~=",_compOpType);
    public static final BuiltInSymbol _le = 
       new BuiltInSymbol("<=",_compOpType); 
    public static final BuiltInSymbol _ge = 
       new BuiltInSymbol(">=",_compOpType); 
    public static final BuiltInSymbol _lt = 
       new BuiltInSymbol("<",_compOpType);  
    public static final BuiltInSymbol _gt = 
       new BuiltInSymbol(">",_compOpType); 
      
    protected static final List<LuaType> _paramOneVarType = 
        new ArrayList<LuaType>() {
        private static final long serialVersionUID = 1L; {
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

    protected static final List<LuaType> _paramOneStrTypes = 
        new ArrayList<LuaType>() {
        private static final long serialVersionUID = 1L; {
        add(LuaType._String); }};
    protected static final LuaType _retIntType = LuaType._Int;
    public static final FunctionType _unStrOpType = 
       new FunctionType(new SequenceType(_paramOneStrTypes), _retIntType);

    public static final BuiltInSymbol _len = 
       new BuiltInSymbol("#",_unStrOpType);

    protected static final List<LuaType> _StrNumTypes = 
        new ArrayList<LuaType>() {
        private static final long serialVersionUID = 1L; {
        add(LuaType._String); add(LuaType._Number); }};
    protected static final UnionType _paramUnStrNumTypes  = 
        new UnionType(new SequenceType(_StrNumTypes));

    protected static final List<LuaType> _paramTwoStrTypes = 
        new ArrayList<LuaType>() {
        private static final long serialVersionUID = 1L; {
        add(_paramUnStrNumTypes); add(_paramUnStrNumTypes); }};
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
        private static final long serialVersionUID = 1L; {
        add(_paramUnStrNumTypes); add(_paramUnStrNumTypes); }};
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
    
    public static final List<Symbol> _BuiltInSymbols = 
        new ArrayList<Symbol>() {
        private static final long serialVersionUID = 1L; {

        add(_false); 
        add(_true); 
        add(_nil); 
        add(_empty); 

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
    }};
}
