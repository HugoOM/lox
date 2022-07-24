package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {
	static boolean hadError = false;
	static boolean hadRuntimeError = false;
	private static final Interpreter interpreter = new Interpreter();

	public static void main(String[] args) throws IOException {
		/*
			testRpnPrinter();
			testAstPrinter();
		*/
		System.out.println();

		if (args.length > 1) {
			System.out.println("Usage: jlox [script]");
			System.exit(64);
		} else if (args.length ==  1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
		
	}

	private static void testAstPrinter() {
		Expr expression = new Expr.Binary(
			new Expr.Unary(
				new Token(TokenType.MINUS, "-", null, 1),
				new Expr.Literal(123)
			),
			new Token(TokenType.STAR, "*", null, 1),
			new Expr.Grouping(
				new Expr.Literal(45.67)
			)
		);

		System.out.println("Testing AST Printer: " + new AstPrinter().print(expression));
	}

	private static void testRpnPrinter() {
		Expr expression = new Expr.Binary(
			new Expr.Grouping(
				new Expr.Binary(
					new Expr.Literal(1),
					new Token(TokenType.PLUS, "+", null, 1),
					new Expr.Literal(2)
				)
			),
			new Token(TokenType.STAR, "*", null, 1),
			new Expr.Grouping(
				new Expr.Binary(
					new Expr.Literal(4),
					new Token(TokenType.MINUS, "-", null, 1),
					new Expr.Literal(3)
				)
			)
		);

		System.out.println("Testing RPN Printer: " + new RpnPrinter().print(expression));
	}


	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));
		if (hadError) System.exit(65);
		if (hadRuntimeError) System.exit(70);
	}

	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);

		while (true) {
			System.out.print("jlox_repl> ");
			String line = reader.readLine();
			if (line == null) break;
			run(line);

			// Reset the error flag in REPL mode as to not brick the execution
			hadError = false;
			hadRuntimeError = false;
		}
	}

	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens);
		List<Stmt> statements = parser.parse();

		// Hack until we get error recovery / parser synchronization.
		if (hadError) {
			return;
		}

		/*
			//For now, just print the tokens.
			System.out.println("Lexical Tokens:");
			for (Token token : tokens) {
				System.out.println("	" + token);
			}

			
			System.out.println("Syntax Tree:");
			System.out.println("	" + new AstPrinter().print(expression));

			System.out.println("Interpreted Results:");
			System.out.print("	");
		*/

		interpreter.interpret(statements);
		System.out.println();
	}

	static void error(int line, String message) {
		report(line, "", message);
	}

	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}

	static void error(Token token, String message) {
		if (token.type == TokenType.EOF) {
			report(token.line, " at end", message);
		} else {
			report(token.line, " at '" + token.lexeme + "'", message);
		}
	}

	static void runtimeError(RuntimeError error) {
		System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
		hadRuntimeError = true;
	}
}
