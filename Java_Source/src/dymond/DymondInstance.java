package dymond;

import java.util.HashMap;
import java.util.Map;

public class DymondInstance {
	private DymondClass klass;
	private final boolean isStatic;
	private final Map<String, Object> fields = new HashMap<>();
	
	public DymondInstance(DymondClass klass) {
		this.klass = klass;
		isStatic = false;
	}
	
	public DymondInstance() {
		this.klass = (DymondClass) this;
		isStatic = true;
	}
	
	@Override
	public String toString() {
		return klass.name + " instance";
	}
	
	public Object get(Token name) {
		if (fields.containsKey(name.lexeme)) {
			return fields.get(name.lexeme);
		}
		
		DymondFunction staticMethod = klass.findStaticMethod(this, name.lexeme);
		DymondFunction regMethod = klass.findMethod(this, name.lexeme);
		
		if(!isStatic) {
			if (regMethod != null) return regMethod;
			
			if(staticMethod != null) throw new RuntimeError(name, "Cannot call static method '" + name.lexeme + "' non-statically. You must call it from the class itself.");
			
			throw new RuntimeError(name, "Undefined method '" + name.lexeme + "'.");
		} else {
			if (staticMethod != null) return staticMethod;
			
			if (regMethod != null) throw new RuntimeError(name, "Cannot call non-static method '" + name.lexeme + "' statically. You must create an instance and call it from the instance.");
			
			throw new RuntimeError(name, "Undefined static method '" + name.lexeme + "'.");
		}
	}
	
	public Object get(Token name, boolean check) {
		if (fields.containsKey(name.lexeme)) {
			return fields.get(name.lexeme);
		}
		
		DymondFunction method = klass.findMethod(this, name.lexeme);
		if (method != null) return method;
		
		return null;
	}
	
	public void set(Token name, Object value) {
		fields.put(name.lexeme, value);
	}
}
