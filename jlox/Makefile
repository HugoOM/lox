default: build run

build:  generate_ast
	@ javac -g -d "./out" -cp ./src ./src/*.java
	@ echo "JLox Recompiled"

run: 
	@ java -cp ./out com.craftinginterpreters.lox.Lox $(script)

generate_ast:
	@ javac -g -d "./out" -cp ./tools ./tools/*.java
	@ java -cp ./out com.craftinginterpreters.tool.GenerateAst "./src"
	@ echo "AST Node Definitions (Re)Generated"

clean:
	@ rm -rf ./out
	@ rm ./src/Expr.java ./src/Stmt.java

