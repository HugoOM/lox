package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

class Parser {
	private static class ParseError extends RuntimeException {}

	private final List<Token> tokens;
	private int current = 0;

	public Parser(List<Token> tokens) {
		this.tokens = tokens;
	}

	public List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();

		while (!isAtEnd()) {
			statements.add(declaration());
		}

		return statements;
	}

	private Stmt declaration() {
		try {
			if (match(TokenType.CLASS)) {
				return classDeclaration();
			}

			if (match(TokenType.FUN)) {
				return function("function");
			}

			if (match(TokenType.VAR)) {
				return varDeclaration();
			}

			return statement();
		}
		catch (ParseError error) {
			synchronize();
			return null;
		}
	}

	private Stmt classDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect class name.");
		consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");

		List<Stmt.Function> methods = new ArrayList<>();

		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			methods.add(function("method"));
		}

		consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");

		return new Stmt.Class(name, methods);
	}

	private Stmt varDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");

		Expr initializer = null;

		if (match(TokenType.EQUAL)) {
			initializer = expression();
		}

		consume (TokenType.SEMICOLON, "Expect ';' after variable declaration.");
		return new Stmt.Var(name, initializer);
	}

	private Stmt statement() {
		if (match(TokenType.FOR)) {
			return forStatement();
		}

		if (match(TokenType.IF)) {
			return ifStatement();
		}

		if (match(TokenType.PRINT)) {
			return printStatement();
		}

		if (match(TokenType.RETURN)) {
			return returnStatement();
		}

		if (match(TokenType.WHILE)) {
			return whileStatement();
		}

		if (match(TokenType.LEFT_BRACE)) {
			return new Stmt.Block(block());
		}

		return expressionStatement();
	}

	private Stmt forStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
		
		Stmt initializer;

		if (match(TokenType.SEMICOLON)) {
			initializer = null;
		} else if (match(TokenType.VAR)) {
			initializer = varDeclaration();
		} else {
			initializer = expressionStatement();
		}
		
		Expr condition = null;
		if (!check(TokenType.SEMICOLON)) {
			condition = expression();
		}
		consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");
		
		Expr increment = null;
		if (!check(TokenType.RIGHT_PAREN)) {
			increment = expression();
		}
		consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");

		Stmt body = statement();

		if (increment != null) {
			body = new Stmt.Block(
				Arrays.asList(
					body,
					new Stmt.Expression(increment)
				)
			);
		}

		if (condition == null) {
			condition = new Expr.Literal(true);
		}

		body = new Stmt.While(condition, body);

		if (initializer != null) {
			body = new Stmt.Block(Arrays.asList(initializer, body));
		}

		return body;
	}

	private Stmt ifStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

		Stmt thenBranch = statement();
		Stmt elseBranch = null;

		if (match(TokenType.ELSE)) {
			elseBranch = statement();
		}

		return new Stmt.If(condition, thenBranch, elseBranch);
	}

	private Stmt printStatement() {
		Expr value = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after value.");
		return new Stmt.Print(value);
	}

	private Stmt returnStatement() {
		Token keyword = previous();
		Expr value = null;

		if (!check(TokenType.SEMICOLON)) {
			value = expression();
		}

		consume(TokenType.SEMICOLON, "Expect ';' after return value.");

		return new Stmt.Return(keyword, value);
	}

	private Stmt whileStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");

		Stmt body = statement();

		return new Stmt.While(condition, body);
	}

	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after expression.");
		return new Stmt.Expression(expr);
	}

	private Stmt.Function function(String kind) {
		Token name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");

		consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
		List<Token> parameters = new ArrayList<>();

		if (!check(TokenType.RIGHT_PAREN)) {
			do {
				if (parameters.size() >= 255) {
					error(peek(), "Can't have more than 255 parameters.");
				}

				parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."));
			} while (match(TokenType.COMMA));
		}

		consume(TokenType.RIGHT_PAREN, "Expec ')' after parameters.");

		// Consumed since block() assumes the opening curly has already
		// 	been matched. This lets us contextualize the error message
		// 	that we're in a function declaration context.
		consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
		List<Stmt> body = block();
		return new Stmt.Function(name, parameters, body);
	}

	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<>();

		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}

		consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");

		return statements;
	}

	private Expr expression() {
		// return parseBinaryRule(this::comma);
		return parseBinaryRule(this::assignment);
	}

	private Expr assignment() {
		Expr expr = or();

		if (match(TokenType.EQUAL)) {
			Token equals = previous();
			Expr value = assignment();

			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable)expr).name;
				return new Expr.Assign(name, value);
			} else if (expr instanceof Expr.Get) {
				Expr.Get get = (Expr.Get)expr;
				return new Expr.Set(get.object, get.name, value);
			}

			error(equals, "Invalid assignment target.");
		}

		return expr;
	}

	private Expr or() {
		return parseBinaryRule(this::and, TokenType.OR);
	}

	private Expr and() {
		return parseBinaryRule(this::comma, TokenType.AND);
	}

	// Challenge #6.1, not sure what it implies to "drop" the operator token here ...
	private Expr comma() {
		return parseBinaryRule(this::ternary, TokenType.COMMA);
	}

	// Challenge #6.2
	private Expr ternary() {
		Expr expr = equality();

		if (match(TokenType.QUESTIONMARK)) {
			Expr thenBranch = expression();
			consume(TokenType.COLON, "Expect ':' after then branch of ternary expression.");
			Expr elseBranch = ternary();
			expr = new Expr.Conditional(expr, thenBranch, elseBranch);
		}

		return expr;
	}
	
	private Expr equality() {
		return parseBinaryRule(this::comparison, TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL);
	}

	private Expr comparison() {
		return parseBinaryRule(this::term, TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL);
	}

	private Expr term() {
		return parseBinaryRule(this::factor, TokenType.MINUS, TokenType.PLUS);
	}

	private Expr factor() {
		return parseBinaryRule(this::unary, TokenType.SLASH, TokenType.STAR);
	}

	private Expr unary() {
		if (match(TokenType.BANG, TokenType.MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}

		return call();
	}

	private Expr call() {
		Expr expr = primary();

		while (true) {
			if (match(TokenType.LEFT_PAREN)) {
				expr = finishCall(expr);
			} else if (match(TokenType.DOT)) {
				Token name = consume(TokenType.IDENTIFIER, "Expect property name after '.'");
				expr = new Expr.Get(expr, name);
			} else {
				break;
			}
		}

		return expr;
	}

	private Expr finishCall(Expr callee) {
		List<Expr> arguments = new ArrayList<>();

		if (!check(TokenType.RIGHT_PAREN)) {
			do {
				if (arguments.size() >= 255) {
					error(peek(), "Can't have more than 255 arguments.");
				}

				arguments.add(expression());
			} while (match(TokenType.COMMA));
		}

		Token paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");

		return new Expr.Call(callee, paren, arguments);
	}

	private Expr primary() {
		if (match(TokenType.FALSE)) {
			return new Expr.Literal(false);
		}
		if (match(TokenType.TRUE)) {
			return new Expr.Literal(true);
		}
		if (match(TokenType.NIL)) {
			return new Expr.Literal(null);
		}

		if (match(TokenType.NUMBER, TokenType.STRING)) {
			return new Expr.Literal(previous().literal);
		}

		if (match(TokenType.THIS)) {
			return new Expr.This(previous());
		}

		if (match(TokenType.IDENTIFIER)) {
			return new Expr.Variable(previous());
		}

		if (match(TokenType.LEFT_PAREN)) {
			Expr expr = expression();
			consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}

		// Error Productions
		checkForBinaryRuleError(this::comma, TokenType.COMMA);

		checkForBinaryRuleError(this::equality, TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL);

		checkForBinaryRuleError(this::comparison, TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL);

		checkForBinaryRuleError(this::term, TokenType.PLUS);

		checkForBinaryRuleError(this::factor, TokenType.SLASH, TokenType.STAR);

		// We are on a token that cannot start an expression ...
		throw error(peek(), "Expect expression.");
	}


	private void checkForBinaryRuleError(BinaryRuleParser op, TokenType... types) {
		if (match(types)) {
			ParseError err = error(previous(), "Missing left-hand operand.");
			op.parse();
			throw err;
		}
	}

	private Expr parseBinaryRule(BinaryRuleParser op, TokenType... types) {
		Expr expr = op.parse();

		while (match(types)) {
			Token operator = previous();
			Expr right = op.parse();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}

	private interface BinaryRuleParser {
		public Expr parse();
	}

	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}

		return false;
	}

	private boolean check(TokenType type) {
		if (isAtEnd()) {
			return false;
		}
		return peek().type == type;
	}

	private Token advance() {
		if (!isAtEnd()) {
			current += 1;
		}
		return previous();
	}

	private boolean isAtEnd() {
		return peek().type == TokenType.EOF;
	}

	private Token peek() {
		return tokens.get(current);
	}

	private Token previous() {
		return tokens.get(current - 1);
	}

	private Token consume(TokenType type, String message) {
		if (check(type)) {
			return advance();
		}

		throw error(peek(), message);
	}

	private ParseError error(Token token, String message) {
		Lox.error(token, message);
		return new ParseError();
	}

	private void synchronize() {
		advance();

		while (!isAtEnd()) {
			if (previous().type == TokenType.SEMICOLON) {
				return;
			}

			switch (peek().type) {
				case CLASS:
				case FUN:
				case VAR:
				case FOR:
				case IF:
				case WHILE:
				case PRINT:
				case RETURN:
					return;
			}

			advance();
		}
	}
}
