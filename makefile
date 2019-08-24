# Make file of all HpcLua tools

JAVAC = javac
ANTLR = java -jar /usr/local/lib/antlr-4.7.2-complete.jar

# compilation args
JFLAGS = -Xlint

# libraries used by the tests
all: grammar tool

grammar: HpcLua.class
	

HpcLua.class: HpcLua.java
	$(JAVAC) $(JFLAGS) HpcLua*.java

HpcLua.java: HpcLua.g4
	$(ANTLR) HpcLua.g4

HpcLuaTool.class: HpcLuaTool.java
	$(JAVAC) $(JFLAGS) HpcLuaTool.java TreePrinterListener.java

tool: HpcLuaTool.class
	

all: grammar tool
	
	
grammarclean: 
	rm HpcLuaBaseListener*.* HpcLuaLexer*.* HpcLuaListener*.* HpcLuaParser*.* HpcLua.interp HpcLua.tokens

toolclean:
	rm *.class

clean : grammarclean toolclean
	

