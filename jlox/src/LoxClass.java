package com.craftinginterpreters.lox;

import java.util.List;
import java.util.Map;

class LoxClass extends LoxInstance implements LoxCallable {
	final String name;
	final LoxClass superclass;
	private final Map<String, LoxFunction> methods;

	public LoxClass(LoxClass metaclass, String name, LoxClass superclass, Map<String, LoxFunction> methods) {
		super(metaclass);

		this.superclass = superclass;
		this.name = name;
		this.methods = methods;
	}

	public LoxFunction findMethod(String name) {
		if (methods.containsKey(name)) {
			return methods.get(name);
		}

		if (superclass != null) {
			return superclass.findMethod(name);
		}

		return null;
	}

	@Override
	public Object call(Interpreter interpreter, List<Object> arguments) {
		LoxInstance instance = new LoxInstance(this);

		LoxFunction initializer = findMethod("init");

		if (initializer != null) {
			initializer.bind(instance).call(interpreter, arguments);
		}

		return instance;
	}

	@Override
	public int arity() {
		LoxFunction initializer = findMethod("init");

		if (initializer == null) {
			return 0;
		}

		return initializer.arity();
	}

	@Override
	public String toString() {
		return "<class " + name + ">";
	}
}
