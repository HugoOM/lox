CLASSES := src/Lox.java \
	   src/Scanner.java \
	   src/Token.java \
	   src/TokenType.java

default: build run

build: 
	javac -g -d "./out" -cp ./src $(CLASSES)

run: 
	@ java -cp ./out com.craftinginterpreters.lox.Lox

