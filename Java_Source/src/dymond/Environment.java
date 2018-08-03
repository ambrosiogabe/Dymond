package dymond;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Random;

public class Environment {
	private final Map<String, Object> values = new HashMap<>();
	public final Environment enclosing;
	Scanner reader = new Scanner(System.in);
	
	public Environment() {
		enclosing = null;
	}
	
	public Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}
	
	public void define(String name, Object value) {
		values.put(name, value);
	}
	
	public Object getAt(int distance, String name) {
		return ancestor(distance).values.get(name);
	}
	
	public Environment ancestor(int distance) {
		Environment environment = this;
		for (int i=0; i < distance; i++) {
			environment = environment.enclosing;
		}
		return environment;
	}
	
	public void assignAt(int distance, Token name, Object value) {
		ancestor(distance).values.put(name.lexeme,  value);
	}
	
	public Object get(Token name) {
		if (values.containsKey(name.lexeme)) {
			return values.get(name.lexeme);
		}
		
		if (enclosing != null) return enclosing.get(name);
		
		throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
	}
	
	public boolean check(String name) {
		if (values.containsKey(name)) {
			return true;
		}
		return false;
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
			public int minArity() { return 0; }
			@Override
			public int maxArity() { return 0; }
			
			@Override
			public Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
				return (double)System.currentTimeMillis() / 1000.0;
			}
			
			@Override
			public Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr) {
				return null;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
		
		this.define("print", new DymondCallable() {
			@Override 
			public int minArity() { return 1; }
			@Override
			public int maxArity() { return 1; }
			
			@Override 
			public Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
				Object text = arguments.get(0);

				System.out.println(stringify(text));
				
				return null;
			}
			
			@Override
			public Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr) {
				return null;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
		
		this.define("toString", new DymondCallable() {
			@Override
			public int minArity() { return 1; }
			@Override
			public int maxArity() { return 1; }
			
			@Override 
			public Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
				Object obj = arguments.get(0);
				
				if(!(obj instanceof Double)) {
					Dymond.error(expr.paren.line, "toString(double) only applicable for type number.", expr.paren.lineText, expr.paren.column);
					return null;
				}
				
				return obj.toString();
			}
			
			@Override
			public Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr) {
				return null;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
		
		this.define("input", new DymondCallable() {
			@Override
			public int minArity() { return 1; }
			@Override
			public int maxArity() { return 1; }
			
			@Override
			public Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
				Object text = arguments.get(0);
				
				if(!(text instanceof String)) {
					Dymond.error(expr.paren.line, "input(String) is only applicable for type string.", expr.paren.lineText, expr.paren.column);
					return null;
				}
				
				System.out.print((String)text);
				Object userIn = reader.nextLine();
				
				return userIn;
			}
			
			@Override
			public Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr) {
				return null;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
		
		this.define("toNumber", new DymondCallable() {
			@Override
			public int minArity() { return 1; }
			@Override
			public int maxArity() { return 1; }
			
			@Override
			public Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
				Object text = arguments.get(0);
				
				if (!(text instanceof String)) {
					Dymond.error(expr.paren.line, "toNumber(String) is only applicable for type string.", expr.paren.lineText, expr.paren.column);
					return null;
				}
				
				if (!(isNumeric((String)text))) {
					Dymond.error(expr.paren.line, "toNumber(String) requires that the parameter is a number.", expr.paren.lineText, expr.paren.column);
					return null;
				}
				
				return Double.parseDouble((String)text);
			}
			
			@Override
			public Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr) {
				return null;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
		
		this.define("isNumber", new DymondCallable() {
			@Override
			public int minArity() { return 1; }
			@Override
			public int maxArity() { return 1; }
			
			@Override 
			public Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
				Object text = arguments.get(0);
				
				if (!(text instanceof String)) {
					return false;
				}
				
				return isNumeric((String)text);
			}
			
			@Override
			public Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr) {
				return null;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
		
		this.define("sqrt", new DymondCallable() {
			@Override
			public int minArity() { return 1; }
			@Override
			public int maxArity() { return 1; }
			
			@Override 
			public Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
				Object text = arguments.get(0);
				
				if (!(text instanceof Double)) {
					Dymond.error(expr.paren.line, "sqrt(Double) is only applicable for type double", expr.paren.lineText, expr.paren.column);
					return null;
				}
				
				return Math.sqrt((Double)text);
			}
			
			@Override
			public Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr) {
				return null;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
		
		this.define("randomInt", new DymondCallable() {
			@Override
			public int minArity() { return 2; }
			@Override
			public int maxArity() { return 2; }
			
			@Override 
			public Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
				Object numOne = arguments.get(0);
				Object numTwo = arguments.get(1);
				
				if (!(numOne instanceof Double && numTwo instanceof Double)) {
					Dymond.error(expr.paren.line, "randomInt(Double, Double) is only applicable for types of double", expr.paren.lineText, expr.paren.column);
					return null;
				}
				
				int beginning = ((Double)numOne).intValue();
				int end = ((Double)numTwo).intValue();
				
				if(end <= beginning) {
					Dymond.error(expr.paren.line, "randomInt(Double, Double) does not support negative ranges or ranges equal to 0", expr.paren.lineText, expr.paren.column);
					return null;
				}
				
				int range = end - beginning;
				Random rand = new Random();
				double n = rand.nextInt(range + 1) + beginning;
				
				return n;
			}
			
			@Override
			public Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr) {
				return null;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
		
		this.define("typeof", new DymondCallable() {
			@Override
			public int minArity() { return 1; }
			@Override
			public int maxArity() { return 1; }
			
			@Override 
			public Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
				Object obj = arguments.get(0);
				return obj.getClass();
			}
			
			@Override
			public Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr) {
				return null;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
		
		this.define("len", new DymondCallable() {
			@Override
			public int minArity() { return 1; }
			@Override
			public int maxArity() { return 1; }
			
			@Override 
			public Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
				Object obj = arguments.get(0);
				
				if (obj instanceof String) {
					double len = ((String)obj).length();
					return len;
				}
				
				return 0;
			}
			
			@Override
			public Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr) {
				return null;
			}
			
			@Override
			public String toString() { return "<native fn>"; }
		});
	}
	
	private String stringify(Object object) {
		if (object == null) return "Null";
		
		if (object instanceof Double) {
			String text = object.toString();
			if (text.endsWith(".0")) {
				text = text.substring(0, text.length() - 2);
			}
			return text;
		}
		
		return object.toString();
	}
	
	private boolean isNumeric(String str) {
		try {
			double d = Double.parseDouble(str);
		} catch (NumberFormatException | NullPointerException nfe) {
			return false;
		}
		return true;
	}
}
