package dymond;

import java.util.List;

public class DymondFunction implements DymondCallable {
	private final Stmt.Function declaration;
	private final Environment closure;
	private final boolean isInitializer;
	
	public DymondFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
		this.declaration = declaration;
		this.closure = closure;
		this.isInitializer = isInitializer;
	}
	
	public DymondFunction bind(DymondInstance instance) {
		Environment environment = new Environment(closure);
		environment.define("this", instance);
		return new DymondFunction(declaration, environment, isInitializer);
	}
	
	@Override 
	public Object call(Interpreter interpreter, List<Object> arguments) {
		Environment environment = new Environment(closure);
		for (int i = 0; i < declaration.parameters.size(); i++) {
			environment.define(declaration.parameters.get(i).lexeme, arguments.get(i));
		}
		
		try {
			interpreter.executeBlock(declaration.body, environment);
		} catch (Return returnValue) {
			return returnValue.value;
		}
		
		if (isInitializer) return closure.getAt(0, "this");
		return null;
	}
	
	@Override 
	public Object call(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
		return null;
	}
	
	@Override
	public int arity() {
		return declaration.parameters.size();
	}
	
	@Override 
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}
}
