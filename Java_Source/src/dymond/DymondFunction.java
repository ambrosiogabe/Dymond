package dymond;

import java.util.List;
import java.util.ArrayDeque;

public class DymondFunction implements DymondCallable {
	private final Stmt.Function declaration;
	private final Environment closure;
	private final boolean isInitializer;
	private final int minParamArgs;
	private final int maxParamArgs;
	
	public DymondFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
		this.declaration = declaration;
		this.closure = closure;
		this.isInitializer = isInitializer;
		
		int min = 0;
		for (Expr.Assign expr : declaration.parameters) {
			if (expr.operator == null) {
				min++;
			}
		}
		this.minParamArgs = min;
		this.maxParamArgs = declaration.parameters.size();
	}
	
	public DymondFunction bind(DymondInstance instance) {
		Environment environment = new Environment(closure);
		environment.define("this", instance);
		return new DymondFunction(declaration, environment, isInitializer);
	}
	
	@Override 
	public Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr) {
		Environment environment = new Environment(closure);
		ArrayDeque<Object> q = new ArrayDeque<>();
		
		for (int i = 0; i < arguments.size(); i++) {
			if (arguments.get(i) instanceof Expr.Assign) {
				Expr.Assign arg = (Expr.Assign)arguments.get(i);
				String name = arg.name.lexeme;
				Object val = interpreter.evaluate(arg);
				environment.define(name, val);
			} else {
				q.add(interpreter.evaluate(arguments.get(i)));
			}
		}
		
		
		for (int i = 0; i < declaration.parameters.size(); i++) {
			String name = declaration.parameters.get(i).name.lexeme;
			
			if (!environment.check(name)) {
				if(q.size() > 0) {
					Object value = q.removeFirst();
					environment.define(name, value);
					continue;
				} else {
					if (declaration.parameters.get(i).operator != null) {
						environment.define(name, interpreter.evaluate(declaration.parameters.get(i).value));
					} else {
						Dymond.runtimeError(new RuntimeError(expr.paren, "Missing required arguments."));
					}
				}
			}
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
	public Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
		return null;
	}
	
	@Override
	public int minArity() {
		return minParamArgs;
	}
	
	@Override
	public int maxArity() {
		return maxParamArgs;
	}
	
	@Override 
	public String toString() {
		return "<fn " + declaration.name.lexeme + ">";
	}
}
