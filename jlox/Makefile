default: build run

build: 
	javac -g -d "./out" -cp ./src ./src/*.java

run: 
	@ java -cp ./out com.craftinginterpreters.lox.Lox

compile_tooling:
	javac -g -d "./out" -cp ./tools ./tools/*.java

generate_ast:
	java -cp ./out com.craftinginterpreters.tool.GenerateAst "./src"
