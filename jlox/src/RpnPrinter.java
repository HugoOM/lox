package com.craftinginterpreters.lox;

class RpnPrinter implements Expr.Visitor<String> {
	String print(Expr expr) {
		return expr.accept(this);
	}

	@Override
	public String visitBinaryExpr(Expr.Binary expr) {
		return toPostfix(expr.operator.lexeme, expr.left, expr.right);
	}

	@Override
	public String visitGroupingExpr(Expr.Grouping expr) {
		return this.visitBinaryExpr((Expr.Binary) expr.expression);
	}

	@Override
	public String visitLiteralExpr(Expr.Literal expr) {
		return expr.value.toString();
	}

	@Override
	public String visitUnaryExpr(Expr.Unary expr) {
		return toPostfix(expr.operator.lexeme, expr.right);
	}

	@Override
	public String visitConditionalExpr(Expr.Conditional expr) {
		return toPostfix("ternary", expr.expression, expr.thenBranch, expr.elseBranch);
	}

	@Override
	public String visitVariableExpr(Expr.Variable expr) {
		return expr.name.lexeme;
	}

	@Override
	public String visitAssignExpr(Expr.Assign expr) {
		// Placeholder - should really return the value as well but the RPN printer is unapplicable now ... 
		return expr.name.lexeme;
	}
	
	private String toPostfix(String name, Expr... exprs) {
		StringBuilder builder = new StringBuilder();

		for (Expr expr : exprs) {
			builder.append(" ");
			builder.append(expr.accept(this));
		}

		builder.append(" " + name);

		return builder.toString().trim();
	}
}	
