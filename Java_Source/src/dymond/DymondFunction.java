package dymond;

import java.util.List;

public class DymondFunction implements DymondCallable {
	private final Stmt.Function declaration;
	private final Environment closure;
	
	public DymondFunction(Stmt.Function declaration, Environment closure) {
		this.declaration = declaration;
		this.closure = closure;
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
