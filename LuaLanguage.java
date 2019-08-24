/***
  LuaLanguage.java

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
import java.util.Collections;

public class LuaLanguage implements Language { 

    private String name; 
    private String extension;   
    private TypeSystem typeSystem;
    private GlobalScope builtInSymbols;

    public static final String varPrefix = 
       VariableSymbol.genPrefix + "v" + VariableSymbol.genPrefix;
    public static final String returnVarPrefix = 
       VariableSymbol.genPrefix + "ret" + VariableSymbol.genPrefix;
    
    public static final BaseScope _GlobalBuiltInScope = 
       new BaseScope(LuaTypeSystem._BuiltInSymbols);
    
    public LuaLanguage(int n, int f, int i) {
       this.name = "Lua"; 
       this.extension = "lua"; 
       this.typeSystem = new LuaTypeSystem(n, f, i);
       this.builtInSymbols = new GlobalScope(null);

        Map<Integer,Type> types = typeSystem.getTypes();
        for (int j = 0;j<types.size();j++) {
           LuaType t = (LuaType) types.get(j);
           builtInSymbols.defineName(t);
        }
        
        builtInSymbols.mergeWith(_GlobalBuiltInScope);
    }

    public String getName() { return name; }
    public String getExt() { return extension; }
    public TypeSystem getTypeSystem() { return typeSystem; }
    public GlobalScope getBuiltIns() { return builtInSymbols; }
}
