package com.craftinginterpreters.lox;

import java.lang.Class;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	private Environment environment = new Environment();

	public void interpret (List<Stmt> statements) {
		try {
			for (Stmt statement : statements) {
				execute(statement);
			}
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}

	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.value;
	}

	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}

	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case BANG:
				return !isTruthy(right);
			case MINUS:
				checkOperandType(Double.class, expr.operator, right);
				return -(double)right;
		}

		// Unreachable.
		return null;
	}
	
	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return environment.get(expr.name);
	}

	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);

		switch (expr.operator.type) {
			case COMMA:
				return right;
			case BANG_EQUAL:
				return !isEqual(left, right);
			case EQUAL_EQUAL:
				return isEqual(left, right);
			case GREATER:
				checkOperandType(Double.class, expr.operator, left, right);
				return (double)left > (double)right;
			case GREATER_EQUAL:
				checkOperandType(Double.class, expr.operator, left, right);
				return (double)left >= (double)right;
			case LESS:
				checkOperandType(Double.class, expr.operator, left, right);
				return (double)left < (double)right;
			case LESS_EQUAL:
				checkOperandType(Double.class, expr.operator, left, right);
				return (double)left <= (double)right;
			case MINUS:
				checkOperandType(Double.class, expr.operator, left, right);
				return (double)left - (double)right;
			case PLUS:
				if (left instanceof Double && right instanceof Double) {
					return (double)left + (double)right;
				}

				if (left instanceof String && right instanceof String) {
					return (String)left + (String)right;
				} 

				if (left instanceof String && right instanceof Double) {
					return (String)left + stringify(right);
				}

				if (left instanceof Double && right instanceof String) {
					return stringify(left) + (String)right;
				}

				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			case SLASH:
				checkOperandType(Double.class, expr.operator, left, right);

				if ((double)right == 0.0) {
					throw new RuntimeError(expr.operator, "Cannot divide by zero");
				}

				return (double)left / (double)right;
			case STAR:
				checkOperandType(Double.class, expr.operator, left, right);
				return (double)left * (double)right;
		}

		// Unreachable.
		return null;
	}

	// Because of right-associativity, I need to evaluate both branches, 
	// 	with elseBranch first... Right?
	@Override
	public Object visitConditionalExpr(Expr.Conditional expr) {
		Object elseObj = evaluate(expr.elseBranch);
		Object thenObj = evaluate(expr.thenBranch);
		Object conditionObj = evaluate(expr.expression);


		if (isTruthy(conditionObj)) {
			return thenObj;
		} else {
			return elseObj;
		}
	}
	
	private void checkOperandType(Class concreteType, Token operator, Object... operands) {
		for (Object operand : operands) {
			if (!concreteType.isInstance(operand)) {
				throw new RuntimeError(operator, "Operand of incorrect type: " + operand + " should be of type " + concreteType.toString());	
			}
		}
	}
	
	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}

	private void execute(Stmt stmt) {
		stmt.accept(this);
	}

	private void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;

		try {
			this.environment = environment;

			for (Stmt statement: statements) {
				execute(statement);
			}
		} 
		finally {
			this.environment = previous;
		}
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		executeBlock(stmt.statements, new Environment(environment));
		return null;
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		evaluate(stmt.expression);
		return null;
	}

	@Override
	public Void visitPrintStmt (Stmt.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;

		if (stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}

		environment.define(stmt.name.lexeme, value);
		return null;
	}

	@Override
	public Object visitAssignExpr(Expr.Assign expr) {
		Object value = evaluate(expr.value);
		environment.assign(expr.name, value);
		return value;
	}

	private boolean isTruthy(Object object) {
		if (object == null) {
			return false;
		}

		if (object instanceof Boolean) {
			return (boolean)object;
		}

		return true;
	}

	private boolean isEqual(Object a, Object b) {
		if (a == null && b == null) {
			return true;
		}

		if (a == null) {
			return false;
		}

		return a.equals(b);
	}

	private String stringify(Object object) {
		if (object == null) {
			return "nil";
		}

		if (object instanceof Double) {
			String text = object.toString();

			if (text.endsWith(".0")) {
				text = text.substring(0, text.length() - 2);
			}

			return text;

		}

		return object.toString();
	}
}
