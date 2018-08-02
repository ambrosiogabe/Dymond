package dymond;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Environment {
	private final Map<String, Object> values = new HashMap<>();
	public final Environment enclosing;
	
	public Environment() {
		enclosing = null;
	}
	
	public Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}
	
	public void define(String name, Object value) {
		values.put(name, value);
	}
	
	public Object get(Token name) {
		if (values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}
		
		if (enclosing != null) return enclosing.get(name);
		
		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}
	
	public void assign(Token name, Object value) {
		if (values.containsKey(name.lexeme)) {
			values.put(name.lexeme, value);
			return;
		}
		
		if(enclosing != null) {
			enclosing.assign(name,  value);
			return;
		}
		
		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}
	
	
	public void defineNativeFunctions() {
		this.define("clock", new DymondCallable() {
			@Override
			public int arity() { return 0; }
			
			@Override
			public Object call(Interpreter interpreter, List<Object> arguments) {
				return (double)System.currentTimeMillis() / 1000.0;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
		
		this.define("print", new DymondCallable() {
			@Override 
			public int arity() { return 2; }
			
			@Override 
			public Object call(Interpreter interpreter, List<Object> arguments) {
				Object text = arguments.get(0);
				Object ending = arguments.get(1);

				System.out.print((String)text + (String)ending);
				
				return null;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
		
		this.define("print", new DymondCallable() {
			@Override 
			public int arity() { return 1; }
			
			@Override 
			public Object call(Interpreter interpreter, List<Object> arguments) {
				Object text = arguments.get(0);

				System.out.println((String)text);
				
				return null;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
		
		this.define("toString", new DymondCallable() {
			@Override
			public int arity() { return 1; }
			
			@Override 
			public Object call(Interpreter interpreter, List<Object> arguments) {
				Object obj = arguments.get(0);
				return obj.toString();
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
	}
}
