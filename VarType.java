/***
  VarType.java

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
import java.lang.String;
import java.util.List;
import java.util.ArrayList;

public class VarType extends LuaType {

    private static final long serialVersionUID = 1L;
    public static final String _unknownName = "";
    
    String varName;   
 
    public VarType() {
        super(LuaType.tVAR);
        this.varName = _unknownName;
        this.setName(varName);
    }
 
    public VarType(String n) {
        super(LuaType.tVAR);
        this.varName = n;
        this.setName(varName);
    }
    
    public String getVarName() {
        return varName;
    }
   
    @Override 
    public String toString() { 
        return varName;
    }
    
    @Override
    public LuaType expand(Scope s) {
       LuaType result = _Unknown;
       if (!name.equals(_unknownName)) {
          Symbol var = s.resolveName(this.getVarName());
          if (var == null)
             result = LuaType._Bottom;
          else 
             result = (LuaType) var.getType();
       }
       if (result.equals(_Unknown))
          return this;
       else
          return result.expand(s);
    }
    
    @Override
    public boolean equals(Object t) {
        if (t == this)
            return true;
        if (t == null) 
            return false; 
        if (!getClass().equals(t.getClass()))
            return false;
        VarType other = (VarType) t;
        return (this.varName.equals(other.getVarName()));
    }

    @Override
    public boolean subtype(LuaType t) {
       if (t.equals(LuaType._Any)) // TTop
          return true;
       LuaType type = (LuaType) this.getType();
       if (type != null) // TVar
          return type.subtype(t);
       else if (this.equals(t)) // TVRefl
          return true;
       else if (t instanceof UnionType) // TUSup 
          return ((UnionType) t).TUSup(this);
       return false;
    }
    
    @Override
    public LuaType subst(LuaType v, LuaType t) { 
       if (this.equals(v))
          return LuaType.copy(t);
       else
          return this;
    }
    
    @Override
    public LuaType subst(Scope s) {
       String name = this.getVarName();
       Symbol symb = s.resolveName(name);
       if (symb != null) { 
          LuaType type = (LuaType) symb.getType();
          if (type.equals(_Unknown))
             return LuaType.copy(this);
          else  
             if (type instanceof VarType)
                return type.subst(s);
             else
                return LuaType.copy(type);
       } else 
          return LuaType.copy(this);
    }
    
    @Override 
    public List<VarType> freeTVars() {
       List<VarType> freeTVars = new ArrayList<VarType>(); 
       freeTVars.add(this);
       return freeTVars;
    }
   
    @Override
    public Scope unifiesWith (Scope s, LuaType t) {
       if (t.equals(LuaType._Any))
          return s;
       boolean reverse = false;
       String name = this.getVarName();
       Symbol symb = s.resolveName(name);
       LuaType type = _Unknown;
       if (symb != null) {
          type = (LuaType) symb.getType();
          if (t instanceof VarType) {
             int line = symb.getLine();
             String nameA = ((VarType) t).getVarName();
             Symbol symbA = s.resolveName(nameA);
             if (symbA != null) {
                LuaType typeA = (LuaType) symb.getType();
                int lineA = symbA.getLine();
                if (VariableSymbol.isGen(name)) {
                   if (!VariableSymbol.isGen(nameA) ||
                       (lineA > line && !typeA.equals(_Unknown)) ||
                       (lineA == line && name.compareTo(nameA)<0 && 
                                         !typeA.equals(_Unknown)))
                       reverse = true;
                }
             } else
                new Throwable().printStackTrace();
          } 
       }
       if (reverse) 
          return t.unifiesWith(s, this); 
       else 
          if (!type.equals(_Unknown))
             return type.unifiesWith(s, t.subst(s));
          else
             return LuaType.extend(s, this, t.subst(s));
    }

    public boolean toBeResolved(Scope s) {
       boolean result = false;
       Symbol var = s.resolveName(this.getVarName());
       if (var != null) {
          LuaType type = (LuaType) var.getType();
          result = type.equals(_Unknown);
       }
       return result;
    }

    public static final VarType _Unknown = new VarType();
}
