package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
	public final Environment enclosing;
	private final Map<String, Object> values = new HashMap<>();

	/**
	 * Global top-level environment's constructor 
	 */
	public Environment() {
		enclosing = null;
	}

	public Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}

	public Object get(Token name) {
		if (values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}

		if (enclosing == null) {
			throw new RuntimeError(name, "Undefined variable '" + name.lexeme +"'.");
		}

		return enclosing.get(name);
	}

	public Object getAt(int distance, String name) {
		return ancestor(distance).values.get(name);
	}

	public void assignAt(int distance, Token name, Object value) {
		ancestor(distance).values.put(name.lexeme, value);
	}

	public Environment ancestor(int distance) {
		Environment environment = this;

		for (int i = 0; i < distance; i++) {
			environment = environment.enclosing;
		}

		return environment;
	}

	public void define(String name, Object value) {
		values.put(name, value);
	}

	public void assign(Token name, Object value) {
		if (values.containsKey(name.lexeme)) {
			values.put(name.lexeme, value);
			return;
		}
		
		if (enclosing == null) {
			throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
		}

		enclosing.assign(name, value);
	}
}
