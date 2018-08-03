package dymond;

import java.util.List;
import java.util.Map;

public class DymondClass extends DymondInstance implements DymondCallable {
	public final String name;
	private final Map<String, DymondFunction> methods;
	private final Map<String, DymondFunction> staticMethods;
	public final DymondClass superclass;
	
	public DymondClass(String name, Map<String, DymondFunction> methods) {
		super();
		this.name = name;
		this.methods = methods;
		this.staticMethods = null;
		this.superclass = null;
	}
	
	public DymondClass(String name, Map<String, DymondFunction> methods, Map<String, DymondFunction> staticMethods, DymondClass superclass) {
		super();
		this.name = name;
		this.methods = methods;
		this.staticMethods = staticMethods;
		this.superclass = superclass;
	}
	
	public DymondFunction findMethod(DymondInstance instance, String name) {
		if (methods.containsKey(name)) {
			return methods.get(name).bind(instance);
		}
		
		if (superclass != null) {
			return superclass.findMethod(instance, name);
		}

		return null;
	}
	
	public DymondFunction findStaticMethod(DymondInstance instance, String name) {
		if (staticMethods == null) return null;
		if (staticMethods.containsKey(name)) {
			return staticMethods.get(name).bind(instance);
		}
		
		if (superclass != null) {
			return superclass.findStaticMethod(instance, name);
		}
		
		return null;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override 
	public Object call(Interpreter interpreter, List<Expr> arguments, Expr.Call expr) {
		DymondInstance instance = new DymondInstance(this);
		DymondFunction initializer = methods.get("init");
		if (initializer != null) {
			initializer.bind(instance).call(interpreter, arguments, expr);
		}
		
		return instance;
	}
	
	@Override 
	public int minArity() {
		DymondFunction initializer = methods.get("init");
		if (initializer == null) return 0;
		return initializer.minArity();
	}
	
	@Override
	public int maxArity() {
		DymondFunction initializer = methods.get("init");
		if (initializer == null) return 0;
		return initializer.maxArity();
	}
	
	@Override 
	public Object call1(Interpreter interpreter, List<Object> arguments, Expr.Call expr) {
		return null;
	}
}
