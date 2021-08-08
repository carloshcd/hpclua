/***
  HpcLua.g4

  BSD License
  Copyright (c) 2013, Kazunori Sakamoto
  Portions Rules Copyright (c) 2016, Alexander Alexeev
  Portions Rules Copyright (c) 2018, Carlos Henrique C. Duarte
  Changes, Actions and Return Values
     Copyright (c) 2018, Carlos Henrique C. Duarte
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

  This grammar file derived from:
    Lua 5.3 Reference Manual
    http://www.lua.org/manual/5.3/manual.html
    Lua 5.2 Reference Manual
    http://www.lua.org/manual/5.2/manual.html
    Lua 5.1 grammar written by Nicolai Mainiero
    http://www.antlr3.org/grammar/1178608849736/Lua.g

  Tested by Kazunori Sakamoto with Test suite for Lua 5.2 
  (http://www.lua.org/tests/5.2/)
  Tested by Alexander Alexeev with Test suite for Lua 5.3 
  (http://www.lua.org/tests/lua-5.3.2-tests.tar.gz)
  Extended, fixed and tested by Carlos Henrique C. Duarte 
  with Test suite for Lua 5.3 
  (http://www.lua.org/tests/lua-5.3.4-tests.tar.gz)
***/

grammar HpcLua;

@header {
import java.util.Map;
import java.util.HashMap;
}

/* Type System Definitions */

basicType returns [LuaType type]
    : 'Nil'       
    { $type = LuaType._Nil; }
    | 'Boolean'   
    { $type = LuaType._Boolean; }
    | 'String'    
    { $type = LuaType._String; }
    ;

numericType returns [LuaType type]
    : 'Int'       
    { $type = LuaType._Int; }
    | 'Long'      
    { $type = LuaType._Long; }
    | 'Single'    
    { $type = LuaType._Single; }
    | 'Double'    
    { $type = LuaType._Double; }
    ;

varType returns [LuaType type]
    : NAME        
    { $type = new VarType($NAME.text); }
    ;
    
tableType returns [LuaType type]
    : '{.}'                       
    { $type = new TableType(); }
    | '{' tableTypeSequence '}'   
    { $type = $tableTypeSequence.type; }
    ;

tableTypeSequence returns [TableType type]
    : (o1=objectType | (o2=objectType '|->' o3=objectType)) 
         (',' tableTypeSequence)?   
      { TableType t = null;
        if ($o1.text == null)
           t = new TableType ($o2.type,$o3.type);
        else
           t = new TableType (LuaType._Int, $o1.type);
        if ($tableTypeSequence.text != null) 
           t.mergeWith($tableTypeSequence.type);
        $type = t;
      }
    ;

// restructure grammar to avoid the need of parentheses
functType returns [LuaType type] 
    :  '(' o1=anyObjectType '->' o2=anyObjectType ')'    
	{   LuaType ao = $o1.type;
        if (!(ao instanceof SequenceType)) 
           ao = new SequenceType($o1.type);
        LuaType ro = $o2.type;
        if (!(ro instanceof SequenceType))
           ro = new SequenceType($o2.type);
        $type = new FunctionType((SequenceType) ao, (SequenceType) ro);}
    ;

sequenceTypeSequence returns [SequenceType type]
    :   o1=objectType ',' o2=objectType
        { List<LuaType> l = new ArrayList<LuaType>();
          l.add(0,$o1.type);
          l.add(1,$o2.type);
          $type = new SequenceType(l);
        }
    |   objectType (',' sequenceTypeSequence)?  
        { LuaType o = $objectType.type;
          if ($sequenceTypeSequence.text != null) { 
             SequenceType t = $sequenceTypeSequence.type;
             List<LuaType> l = t.getElements();
             if (l != null) {
                 l.add(0,o);
                 $type = t;
             } else
                 $type = new SequenceType(o);
          } else 
              $type = new SequenceType(o);
        }
    ;

sequenceType returns [SequenceType type]
    : '(.)'
    { List<LuaType> t = new ArrayList<LuaType>();
      $type = new SequenceType (t); }
    | '(' sequenceTypeSequence ')'    
    { $type = $sequenceTypeSequence.type; }
    ;

abbrvType returns [LuaType type] 
    : 'Bottom'    
    { $type = LuaType._Bottom; }
    | 'Integer'   
    { $type = LuaType._Integer; }
    | 'Float'     
    { $type = LuaType._Float; }
    | 'Number'
    { $type = LuaType._Number; }
    | 'Table'     
    { $type = LuaType._Table; }
    | 'Function'  
    { $type = LuaType._Function; }
    | 'Object'    
    { $type = LuaType._Object; }
    ;    

objectType returns [LuaType type]
    : basicType                   
    { $type = $basicType.type; }
    | numericType                 
    { $type = $numericType.type; }
    | varType                     
    { $type = $varType.type; }
    | 'Thread'                    
    { $type = LuaType._Thread; }
    | 'UserData'                  
    { $type = LuaType._UserData; }
    | tableType                   
    { $type = $tableType.type; }
    | functType                   
    { $type = $functType.type; }
    | 'U' sequenceType            
    { $type = new UnionType($sequenceType.type); }
    | 'V' varType '.' objectType  
    { $type = new QuantifiedType((VarType) $varType.type,$objectType.type); }
    | 'Any'                       
    { $type = LuaType._Any; }
    | abbrvType                   
    { $type = $abbrvType.type; }
    | '(' o=objectType ')'        
    { $type = $o.type; }
    ;
    
anyObjectType returns [LuaType type]
    : objectType
    { $type = $objectType.type; }
    | sequenceType
    { $type = $sequenceType.type; }
    ;
    
typeAscr returns [LuaType type]
    : ':.' objectType
    { $type = $objectType.type; }
    ;
    
anyTypeAscr returns [LuaType type]
    : ':.' anyObjectType 
    { $type = $anyObjectType.type; }
    ;

/* Typed statements and expressions */

chunk
    : block EOF
    ;

block
    : retStat?
    | stat block
    ;

stat
    : ';'                                                                        # SemiColonStat
    | varList '=' expList                                                        # GAsgnStat 
    | functionCall                                                               # FunctionCallStat
    | label                                                                      # LabelStat
    | 'break'                                                                    # BreakStat
    | 'goto' NAME                                                                # GotoStat
    | 'do' block 'end'                                                           # DoStat
    | 'while' exp 'do' block 'end'                                               # WhileStat
    | 'repeat' block 'until' exp                                                 # RepeatStat
    | 'if' exp 'then' block ('elseif' exp 'then' block)* ('else' block)? 'end'   # IfStat
    | 'for' varNameAndType '=' exp ',' exp (',' exp)? 'do' block 'end'           # ForNumStat
    | 'for' nameList 'in' expList 'do' block 'end'                               # ForGenStat
    | 'function' functName functBody                                             # FunctStat
    | 'local' 'function' varName functBody                                       # LocalFunctStat
    | 'local' nameList ('=' expList)?                                            # LAsgnStat
    ;
   
retStat
    : 'return' expList? ';'?
    ;
    
label
    : '::' NAME '::'
    ;
    
functName returns [LuaType type]
    : fn=varName ('.' varName)* (':' varName)?
    // the OO notation is not supported yet
    { $type = $fn.type; }
    ;
    
varList returns [SequenceType type]
    :  var (',' vl=varList)?
    { LuaType o = $var.type;
      if ($vl.text != null) { 
         SequenceType t = $vl.type;
         List<LuaType> l = t.getElements();
         if (l != null) {
            l.add(0,o);
            $type = t;
         } else
            $type = new SequenceType(o);
      } else 
         $type = new SequenceType(o);
    }
    ;

var returns [LuaType type] 
    : ( varNameAndType | '(' exp ')' vs=varSuffix ) varSuffix*
    { $type = LuaAnalyser.mkNewGenVarType(); }
    ;
    
varSuffix returns [LuaType type]
    : nameAndArgs* '[' exp ']' 
    { $type = $exp.type; } 
    | nameAndArgs* '.' varName
    { $type = LuaType._String; } 
    ;
    
nameList returns [SequenceType type]
    :  varNameAndType (',' nl=nameList)?
    { LuaType o = $varNameAndType.type;
      if ($nl.text != null) { 
         SequenceType t = $nl.type;
         List<LuaType> l = t.getElements();
         if (l != null) {
            l.add(0,o);
            $type = t;
         } else
            $type = new SequenceType(o);
      } else 
         $type = new SequenceType(o);
    }
    ;

expList returns [SequenceType type]
    : exp (',' el=expList)? 
    { LuaType o = $exp.type;
      if ($el.text != null) { 
         SequenceType t = $el.type;
         List<LuaType> l = t.getElements();
         if (l != null) {
            l.add(0,o);
            $type = t;
         } else
            $type = new SequenceType(o);
      } else 
         $type = new SequenceType(o);
    }
    ;

exp returns [LuaType type]
    : 'nil' 
    { $type = LuaType._Nil; // Nil
      LuaAnalyser.reportRule("Nil");
    } # NilExp 
    | 'false'
    { $type = LuaType._Boolean; // Bool
      LuaAnalyser.reportRule("Bool");
    } # FalseExp 
    | 'true'
    { $type = LuaType._Boolean; // Bool
      LuaAnalyser.reportRule("Bool");
    } # TrueExp 
    | number 
    { $type = $number.type;
    } # NumberExp
    | string
    { $type = LuaType._String; // Str
      LuaAnalyser.reportRule("Str");
    } # StringExp 
    | '...'
    { $type = LuaType._Bottom; /** not supported yet **/
    } # VarArgExp 
    | functDef
    { $type = $functDef.type;
    } # FunctDefExp
    | prefixExp 
    { $type = $prefixExp.type;
    } # PrefixExpExp
    | tableConstructor 
    { $type = $tableConstructor.type;
    } # TableContructorExp
    | <assoc=right> exp operatorPower exp
    { $type = LuaAnalyser.opReturnType($operatorPower.text); 
    } # PowerOpExp
    | operatorUnary exp
    { $type = LuaAnalyser.opReturnType($operatorUnary.text); 
    } # UnaryOpExp
    | exp operatorMulDivMod exp
    { $type = LuaAnalyser.opReturnType($operatorMulDivMod.text); 
    } # MulDivModOpExp
    | exp operatorAddSub exp
    { $type = LuaAnalyser.opReturnType($operatorAddSub.text); 
    } # AddSubOpExp
    | <assoc=right> exp operatorStrCat exp
    { $type = LuaAnalyser.opReturnType($operatorStrCat.text); 
    } # StrCatOpExp
    | exp operatorComparison exp
    { $type = LuaAnalyser.opReturnType($operatorComparison.text); 
    } # CompOpExp
    | exp operatorAnd exp
    { $type = LuaAnalyser.opReturnType($operatorAnd.text); 
    } # AndOpExp
    | exp operatorOr exp
    { $type = LuaAnalyser.opReturnType($operatorOr.text); 
    } # OrOpExp 
    | exp operatorBitwise exp
    { $type = LuaAnalyser.opReturnType($operatorBitwise.text); 
    } # BitOpExp
    | e=exp typeAscr
    { $type = $e.type; 
    } # TypeAscrExp 
    ;

prefixExp returns [LuaType type] 
    : varOrExp nameAndArgs*
    { $type = LuaAnalyser.mkNewGenVarType(); }
    ;

functionCall 
    : varOrExp nameAndArgs+
    ;    

varOrExp returns [LuaType type]
    : var 
    { $type = $var.type; } 
    | '(' exp ')'
    { $type = $exp.type; } 
    ;

nameAndArgs returns [LuaType type]
    : (':' varName)? args
    { if ($varName.text != null)
         $type = LuaType._Bottom;
      else
         $type = $args.type; } 
    // this OO notation is not supported yet
    ;
    
args returns [SequenceType type]
    : '(' expList? ')' 
    { if ($expList.text != null) 
         $type = $expList.type;
      else {
         $type = new SequenceType (LuaType._Bottom); // ESeq
         LuaAnalyser.reportRule("ESeq");
      }
    }
    | tableConstructor 
    { $type = new SequenceType ($tableConstructor.type); }
    | string
    { $type = new SequenceType (LuaType._String); }
    ;

functDef returns [LuaType type]
    : 'function' functBody
    { $type = LuaAnalyser.mkNewUnbGenFunctType(); }
    ;

functBody
    : '(' parList? ')' anyTypeAscr? block 'end'
    ;
    
parList returns [SequenceType type]
    : nameList (',' '...')? // '...' is not supported yet
    { $type = $nameList.type; }
    | '...'
    { $type = new SequenceType (LuaType._Bottom); } // ',,,' not supported yet
    ;

tableConstructor returns [TableType type]
    : '{' fieldList? '}'
    { if ($fieldList.text != null) 
         $type = $fieldList.type;
      else {
         $type = LuaType._Table; // ETab
         LuaAnalyser.reportRule("ETab");
      }
    }
    ;

fieldList returns [TableType type]
    : f1=field (fieldSep f2=fieldList?)?
    { TableType t = $f1.type;
      if ($f2.text != null)
         $type = t.mergeWith($f2.type);
      else
         $type = t;
    }
    ;

field returns [TableType type]
    : '[' e1=exp ']' '=' e2=exp 
    { $type = new TableType($e1.type, $e2.type); }
    | NAME '=' exp 
    { $type = new TableType(LuaType._String, $exp.type); }
    | exp 
    { $type = new TableType(LuaType._Int, $exp.type); }
    ;
    
number returns [LuaType type]
    : INTEGER 
    { if (LuaType.isInt(Long.valueOf($INTEGER.text))) { // Num
         $type = LuaTypeSystem.getNDefault()==0?LuaType._Number:
                 (LuaTypeSystem.getIDefault()==0?LuaType._Integer: // Integer
                     LuaTypeSystem.getIDefault()==1?LuaType._Long: // Long
                        LuaType._Int);
         LuaAnalyser.reportRule("Num | Long | Integer");
      } else {
         $type = LuaType._Long; // Long
         LuaAnalyser.reportRule("Long");
      }
    }
    | HEX 
    { if (LuaType.isInt(Long.decode($HEX.text))) { // Num 
         $type = LuaTypeSystem.getNDefault()==0?LuaType._Number:
                 (LuaTypeSystem.getIDefault()==0?LuaType._Integer: // Integer
                     LuaTypeSystem.getIDefault()==1?LuaType._Long: // Long
                        LuaType._Int);
         LuaAnalyser.reportRule("Num | Long | Integer");
    } else {
         $type = LuaType._Long; // Long
         LuaAnalyser.reportRule("Long");
      }
    }
    | FLOAT 
    { if (LuaType.isSingle(Double.valueOf($FLOAT.text))) { // Num
         $type = LuaTypeSystem.getFDefault()==0?LuaType._Float: // Float
                    (LuaTypeSystem.getFDefault()==1)?LuaType._Double: // Double
                       LuaType._Single;
         LuaAnalyser.reportRule("Num | Float | Double");
      } else {
         $type = LuaType._Double; // Double
         LuaAnalyser.reportRule("Double");
      }
    }
    | HEX_FLOAT
    { if (LuaType.isSingle(Double.valueOf($HEX_FLOAT.text))) { // Num
         $type = LuaTypeSystem.getFDefault()==0?LuaType._Float: // Float
                    LuaTypeSystem.getFDefault()==1?LuaType._Double: // Double
                       LuaType._Single; 
         LuaAnalyser.reportRule("Num | Float | Double");                 
      } else {
         $type = LuaType._Double; // Double
         LuaAnalyser.reportRule("Double");
      }
    }
    ;

varName returns [LuaType type] 
    : NAME
    { $type = LuaAnalyser.mkNewGenVarType(); }
    ;
    
varNameAndType returns [LuaType type]
    : varName typeAscr?
    { $type = $varName.type; }
    | '(' varName typeAscr ')'
    { $type = $varName.type; }
    ;

fieldSep
    : ',' | ';'
    ;

operatorOr 
	: 'or';

operatorAnd 
	: 'and';

operatorComparison 
	: '<' | '>' | '<=' | '>=' | '~=' | '==';

operatorStrCat
	: '..';

operatorAddSub
	: '+' | '-';

operatorMulDivMod
	: '*' | '/' | '%' | '//';

operatorBitwise
	: '&' | '|' | '~' | '<<' | '>>';

operatorUnary
    : 'not' | '#' | '-' | '~';

operatorPower
    : '^';

string 
    : NORMALSTRING 
    | CHARSTRING   
    | LONGSTRING   
    ;

/* Lexer */

NAME
    : [a-zA-Z_][a-zA-Z_0-9]*
    ;

NORMALSTRING
    : '"' ( EscapeSequence | ~('\\'|'"') )* '"' 
    ;

CHARSTRING
    : '\'' ( EscapeSequence | ~('\''|'\\') )* '\''
    ;

LONGSTRING
    : '[' NESTED_STR ']'
    ;

fragment
NESTED_STR
    : '=' NESTED_STR '='
    | '[' .*? ']'
    ;

INTEGER
    : Digit+
    ;

HEX
    : '0' [xX] HexDigit+
    ;

FLOAT
    : Digit+ '.' Digit* ExponentPart?
    | '.' Digit+ ExponentPart?
    | Digit+ ExponentPart
    ;

HEX_FLOAT
    : '0' [xX] HexDigit+ '.' HexDigit* HexExponentPart?
    | '0' [xX] '.' HexDigit+ HexExponentPart?
    | '0' [xX] HexDigit+ HexExponentPart
    ;

fragment
ExponentPart
    : [eE] [+-]? Digit+
    ;

fragment
HexExponentPart
    : [pP] [+-]? Digit+
    ;

fragment
EscapeSequence
    : '\\' [abfnrtvz"'\\]
    | '\\' '\r'? '\n'
    | DecimalEscape
    | HexEscape
    | UtfEscape
    ;
    
fragment
DecimalEscape
    : '\\' Digit
    | '\\' Digit Digit
    | '\\' [0-2] Digit Digit
    ;
    
fragment
HexEscape
    : '\\' 'x' HexDigit HexDigit
    ;

fragment
UtfEscape
    : '\\' 'u{' HexDigit+ '}'
    ;

fragment
Digit
    : [0-9]
    ;

fragment
HexDigit
    : [0-9a-fA-F]
    ;

COMMENT
    : '--[' NESTED_STR ']' -> channel(HIDDEN) // skip single comments
    ;
    
LINE_COMMENT
    : '--'
    (                                               // --
    | '[' '='*                                      // --[==
    | '[' '='* ~('='|'['|'\r'|'\n') ~('\r'|'\n')*   // --[==AA
    | ~('['|'\r'|'\n') ~('\r'|'\n')*                // --AAA
    ) ('\r\n'|'\r'|'\n'|EOF)
    -> channel(HIDDEN)
    ;
    
WS  
    : [ \t\u000C\r\n]+ -> skip // skip spaces
    ;

SHEBANG
    : '#' '!' ~('\n'|'\r')* -> channel(HIDDEN) // skip commented lines
    ;
