package com.craftinginterpreters.lox;

import java.lang.Class;

class Interpreter implements Expr.Visitor<Object> {
	public void interpret (Expr expression) {
		try {
			Object value = evaluate(expression);
			System.out.println(stringify(value));
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
