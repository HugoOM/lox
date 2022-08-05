package com.craftinginterpreters.lox;

import java.util.List;

interface LoxCallable {
	int arity();

	Object call(Interpreter interpret, List<Object> arguments);
}
