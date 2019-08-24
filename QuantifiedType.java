/***
  QuantifiedType.java

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

public class QuantifiedType extends LuaType {

    private static final long serialVersionUID = 1L;
    
    VarType tvar;
    LuaType texpr;
 
    public QuantifiedType(VarType v, LuaType e) {
        super(LuaType.tQUANT);
        this.tvar = v;
        this.texpr = e;
    }
    
    public VarType getTVar() {
        return tvar;
    }
    
    public LuaType getTExpr() {
        return texpr;
    }
   
    @Override 
    public String toString() { 
        return "V "+tvar.toString()+"."+texpr.toString();
    }
    
    @Override 
    public QuantifiedType expand(Scope s) { 
       texpr = texpr.expand(s);
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
        QuantifiedType other = (QuantifiedType) t;
        return (this.texpr.equals(
                   other.getTExpr().subst(other.getTVar(),this.tvar)));
    }

    @Override
    public boolean subtype(LuaType t) {
       if (t.equals(LuaType._Any)) // TTop
          return true;
       if (t instanceof QuantifiedType) { // TForall
          LuaType type = ((QuantifiedType) t).getTExpr();
          if (type != null) 
             return this.getTExpr().subtype(type);
       } else if (t instanceof UnionType) // TUSup
          return ((UnionType) t).TUSup(this);
       return false;
    }
    
    @Override
    public LuaType subst(LuaType v, LuaType t) { 
       LuaType obj = this.getTVar();
       if (!obj.equals(v)) 
          this.getTExpr().subst(v,t);
       return this;
    }
    
    @Override
    public LuaType subst(Scope s) { 
       LuaType ret = LuaType.copy(this);
       VarType var = this.getTVar();
       Symbol symb = s.resolveName(var.getVarName());
       if ((symb != null) && (symb.getName() != var.getVarName())) {
          LuaType quant = this.getTExpr().subst(s);
          ret = new QuantifiedType ((VarType) LuaType.copy(tvar), quant);
       } 
       return ret;
    }
    
    @Override 
    public List<VarType> freeTVars() {
       List<VarType> freeElemVars = new ArrayList<VarType>();
       List<VarType> e = this.texpr.freeTVars();
       for(VarType v : e) {
          if (!this.tvar.equals(v))
             freeElemVars.add(v);
       }
       return freeElemVars;
    }
    
    @Override
    public Scope unifiesWith (Scope s, LuaType t) {
       if (t.equals(LuaType._Any) || 
           t instanceof VarType ||
           t instanceof UnionType)
          return t.unifiesWith(s, this);
       else 
          if (freeTVars(s).contains(this.getTVar()))
             return null;
          else 
             return this.getTExpr().unifiesWith(s,t);
    }
}
