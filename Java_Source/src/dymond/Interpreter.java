package dymond;

import static dymond.TokenType.*;
import java.util.List;
import java.util.Map;

import dymond.Expr.Ternary;
import java.util.ArrayList;
import java.util.HashMap;


public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	final Environment globals = new Environment();
	private Environment environment = globals;
	private final Map<Expr, Integer> locals = new HashMap<>();
	private boolean repl;
	
	public Interpreter() {
		globals.defineNativeFunctions();
	}
	
	public void interpret(List<Stmt> statements, boolean repl) {
		try {
			this.repl = repl;
			
			for(Stmt statement : statements) {
				try {
					execute(statement);
				} catch(BreakError err) {
					throw new RuntimeError(err.token, "Break statement must be inside a loop.");
				} catch(Next err) {
					throw new RuntimeError(err.token, "Next statement must be inside a for-loop.");
				}
			}
		} catch (RuntimeError error) {
			Dymond.runtimeError(error);
		}
	}
	
	@Override
	public Object visitSuperExpr(Expr.Super expr) {
		int distance = locals.get(expr);
		DymondClass superclass = (DymondClass)environment.getAt(distance, "super");
		
		// "this" is always one level nearer than "super"'s environment
		DymondInstance object = (DymondInstance)environment.getAt(distance - 1,  "this");
		
		DymondFunction method = superclass.findMethod(object,  expr.method.lexeme);
		DymondFunction staticMethod = superclass.findStaticMethod(object, expr.method.lexeme);
		if(method == null && staticMethod == null) {
			throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
		}
		
		if(staticMethod == null) return method;
		return staticMethod;
	}
	
	@Override
	public Object visitThisExpr(Expr.This expr) {
		return lookUpVariable(expr.keyword, expr);
	}
	
	@Override
	public Object visitGetExpr(Expr.Get expr) {
		Object object = evaluate(expr.object);
		if (object instanceof DymondInstance) {
			return ((DymondInstance) object).get(expr.name);
		}
		
		throw new RuntimeError(expr.name, "Only instances have properties.");
	}
	
	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		Object superclass = null;
		if (stmt.superclass != null) {
			superclass = evaluate(stmt.superclass);
			if (!(superclass instanceof DymondClass)) {
				throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
			}
		}
		environment.define(stmt.name.lexeme, null);
		
		if (stmt.superclass != null) {
			environment = new Environment(environment);
			environment.define("super", superclass);
		}
		
		Map<String, DymondFunction> methods = new HashMap<>();
		Map<String, DymondFunction> staticMethods = new HashMap<>();
		for (Stmt.Function method : stmt.methods) {
			DymondFunction function = new DymondFunction(method, environment, method.name.lexeme.equals("init"));
			methods.put(method.name.lexeme, function);
		}
		
		for (Stmt.Function staticMethod : stmt.staticMethods) {
			DymondFunction function = new DymondFunction(staticMethod, environment, false);
			staticMethods.put(staticMethod.name.lexeme, function);
		}
		
		DymondClass klass = new DymondClass(stmt.name.lexeme, methods, staticMethods, (DymondClass)superclass);
		
		if (superclass != null) environment = environment.enclosing;
		
		environment.assign(stmt.name,  klass);
		return null;
	}
	
	@Override 
	public Void visitReturnStmt(Stmt.Return stmt) {
		Object value = null;
		if (stmt.value != null) value = evaluate(stmt.value);
		
		throw new Return(value);
	}
	
	@Override
	public Void visitBreakStmt(Stmt.Break stmt) {
		throw new BreakError(stmt.keyword);
	}
	
	@Override 
	public Void visitNextStmt(Stmt.Next stmt) {
		throw new Next(stmt.keyword);
	}
	
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		DymondFunction function = new DymondFunction(stmt, environment, false);
		environment.define(stmt.name.lexeme,  function);
		return null;
	}
	
	@Override 
	public Object visitCallExpr(Expr.Call expr) {
		Object callee = evaluate(expr.callee);
		
		List<Object> arguments = new ArrayList<>();
		for (Expr argument : expr.arguments) {
			arguments.add(evaluate(argument));
		}
		
		if (!(callee instanceof DymondCallable)) {
			throw new RuntimeError(expr.paren, "Can only call functions and classes.");			
		}
		
		DymondCallable function = (DymondCallable)callee;
		if (arguments.size() != function.arity()) {
			throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments, but got " + arguments.size() + ".");
		}
		
		if(callee.toString().equals("<native fn>"))
			return function.call(this, arguments, expr);
		else
			return function.call(this,  arguments);
	}
	
	@Override 
	public Void visitWhileStmt(Stmt.While stmt) {
		while (isTruthy(evaluate(stmt.condition))) {
			try {
				execute(stmt.body);
			} catch(BreakError err) {
				break;
			}
		}
		
		return null;
	}
	
	@Override 
	public Void visitForStmt(Stmt.For stmt) {
		while (isTruthy(evaluate(stmt.condition))) {
			try {
				execute(stmt.body);
			} catch(BreakError err) {
				break;
			} catch(Next err) {
				execute(stmt.increment);
			}
		}
		
		return null;
	}
	
	@Override
	public Object visitLogicalExpr(Expr.Logical expr) {
		Object left = evaluate(expr.left);
		
		if (expr.operator.type == OR) {
			if (isTruthy(left)) return left;
		} else {
			if (!isTruthy(left)) return left;
		}
		
		return evaluate(expr.right);
	}
	
	@Override
	public Void visitIfStmt(Stmt.If stmt) {
		if (isTruthy(evaluate(stmt.condition))) {
			execute(stmt.thenBranch);
		} else if (stmt.elseBranch != null) {
			execute(stmt.elseBranch);
		}
		return null;
	}
	
	@Override 
	public Void visitBlockStmt(Stmt.Block stmt) {
		executeBlock(stmt.statements, new Environment(environment));
		
		return null;
	}
	
	public void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;
		try {
			this.environment = environment;
			
			for (Stmt statement : statements) {
				execute(statement);
			}
		} finally {
			this.environment = previous;
		}
	}
	
	@Override 
	public Object visitAssignExpr(Expr.Assign expr) {
		Integer distance = locals.get(expr);
		Object left = null;
		if (distance != null) left = environment.getAt(distance, expr.name.lexeme);
		else left = globals.get(expr.name);
		Object right = evaluate(expr.value);
		Object finalValue = null;
		
		if(left == null && expr.operator.type != EQUAL) {
			throw new RuntimeError(expr.name, "Undefined variable '" + expr.name.lexeme + "'."); 
		}
		
		switch(expr.operator.type) {
			case PLUS_EQUAL:
				if (left instanceof String && right instanceof String) finalValue = (String)left + (String)right;
				else if (left instanceof Double && right instanceof Double) finalValue = (Double)left + (Double)right;
				else if(left instanceof Double && right instanceof String || left instanceof String && right instanceof Double) {
					if (left instanceof Double) left = stringify(left);
					if (right instanceof Double) right = stringify(right);
					finalValue = (String)left + (String)right;
				} else throw new RuntimeError(expr.operator, "Operands may be comprised of numbers and strings only.");
				break;
			case MINUS_EQUAL:
				if (left instanceof Double && right instanceof Double) finalValue = (Double)left - (Double)right;
				else throw new RuntimeError(expr.operator, "Operands must be two numbers.");
				break;
			case MODULO_EQUAL:
				if (left instanceof Double && right instanceof Double) finalValue = (Double)left % (Double)right;
				else throw new RuntimeError(expr.operator, "Operands must be two numbers.");
				break;
			case TIMES_EQUAL:
				if (left instanceof Double && right instanceof Double) finalValue = (Double)left * (Double)right;
				else throw new RuntimeError(expr.operator, "Operands must be two numbers.");
				break;
		}
		
		if(finalValue == null && right != null) {
			finalValue = right;
		}
		
		if (distance != null) {
			environment.assignAt(distance, expr.name, finalValue);
		} else {
			globals.assign(expr.name, finalValue);
		}
		return finalValue;
	}
	
	@Override 
	public Object visitSetExpr(Expr.Set expr) {
		Object object = evaluate(expr.object);
		
		if (!(object instanceof DymondInstance)) {
			throw new RuntimeError(expr.name, "Only instances have fields.");
		}
		
		Object right = evaluate(expr.value);
		Object left = ((DymondInstance)object).get(expr.name, true);
		Object finalValue = null;
		
		if (left == null && expr.equals.type != EQUAL) {
			throw new RuntimeError(expr.name, "Undefined property '" + expr.name.lexeme + "'.");
		}
		
		switch(expr.equals.type) {
			case PLUS_EQUAL:
				if (left instanceof String && right instanceof String) finalValue = (String)left + (String)right;
				else if (left instanceof Double && right instanceof Double) finalValue = (Double)left + (Double)right;
				else if(left instanceof Double && right instanceof String || left instanceof String && right instanceof Double) {
					if (left instanceof Double) left = stringify(left);
					if (right instanceof Double) right = stringify(right);
					finalValue = (String)left + (String)right;
				} else throw new RuntimeError(expr.equals, "Operands may be comprised of numbers and strings only.");
				break;
			case MINUS_EQUAL:
				if (left instanceof Double && right instanceof Double) finalValue = (Double)left - (Double)right;
				else throw new RuntimeError(expr.equals, "Operands must be two numbers.");
				break;
			case MODULO_EQUAL:
				if (left instanceof Double && right instanceof Double) finalValue = (Double)left % (Double)right;
				else throw new RuntimeError(expr.equals, "Operands must be two numbers.");
				break;
			case TIMES_EQUAL:
				if (left instanceof Double && right instanceof Double) finalValue = (Double)left * (Double)right;
				else throw new RuntimeError(expr.equals, "Operands must be two numbers.");
				break;
		}
		
		if(finalValue == null && right != null) {
			finalValue = right;
		}
		
		((DymondInstance)object).set(expr.name, finalValue);
		return finalValue;
	}
	
	@Override
	public Object visitUnaryAssignExpr(Expr.UnaryAssign expr) {
		Integer distance = locals.get(expr);
		Object left = null;
		if (distance != null) left = environment.getAt(distance, expr.name.lexeme);
		else left = globals.get(expr.name);
		
		switch(expr.operator.type) {
			case PLUS_PLUS:
				checkNumberOperand(expr.operator, left);
				left = (double)left + 1;
				break;
			case MINUS_MINUS:
				checkNumberOperand(expr.operator, left);
				left = (double)left - 1;
				break;
		}
		
		if (distance != null) {
			environment.assignAt(distance, expr.name, left);
		} else {
			globals.assign(expr.name, left);
		}
		return left;
	}
	
	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if(stmt.initializer != null) {
			value = evaluate(stmt.initializer);
		}
		
		environment.define(stmt.name.lexeme, value);
		return null;
	}
	
	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return lookUpVariable(expr.name, expr);
	}
	
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		Object result = evaluate(stmt.expression);
		if(repl)
			System.out.println(stringify(result));
		return null;
	}
	
	@Override
	public Object visitLiteralExpr(Expr.Literal expr) {
		return expr.value;
	}
	
	@Override
	public Object visitGroupingExpr(Expr.Grouping expr) {
		return evaluate(expr.expression);
	}
	
	@Override
	public Object visitUnaryExpr(Expr.Unary expr) {
		Object right = evaluate(expr.right);
		
		switch (expr.operator.type) {
			case MINUS:
				checkNumberOperand(expr.operator, right);
				return -(double)right;
			case BANG:
				return !isTruthy(right);
		}
		
		return null;
	}
	
	@Override
	public Object visitBinaryExpr(Expr.Binary expr) {
		Object left = evaluate(expr.left);
		Object right = evaluate(expr.right);
		
		switch (expr.operator.type) {
			case MINUS:
				checkNumberOperands(expr.operator, left, right);
				return (double)left - (double)right;
			case DIV:
				checkNumberOperands(expr.operator, left, right);
				if ((double)right == 0) {
					throw new RuntimeError(expr.operator, "Division by zero error.");
				}
				
				return (double)left / (double)right;
			case STAR:
				checkNumberOperands(expr.operator, left, right);
				return (double)left * (double)right;
			case INTEGER_DIV:
				checkNumberOperands(expr.operator, left, right);
				return (double)( (int)( (double)left ) / (int)( (double)right )  );
			case GREATER:
				if (left instanceof Double && right instanceof Double) return (double)left > (double)right;
				if(left instanceof String && right instanceof String) {
					int val = ( ((String)left).toLowerCase() ).compareTo( ((String)right).toLowerCase() );
					return val > 0;
				}
				
				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			case GREATER_EQUAL:
				if (left instanceof Double && right instanceof Double) return (double)left >= (double)right;
				if(left instanceof String && right instanceof String) {
					int val = ( ((String)left).toLowerCase() ).compareTo( ((String)right).toLowerCase() );
					return val >= 0;
				}
				
				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			case LESS:
				if (left instanceof Double && right instanceof Double) return (double)left < (double)right;
				if(left instanceof String && right instanceof String) {
					int val = ( ((String)left).toLowerCase() ).compareTo( ((String)right).toLowerCase() );
					return val < 0;
				}
				
				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			case LESS_EQUAL:
				if (left instanceof Double && right instanceof Double) return (double)left <= (double)right;
				if(left instanceof String && right instanceof String) {
					int val = ( ((String)left).toLowerCase() ).compareTo( ((String)right).toLowerCase() );
					return val <= 0;
				}
				
				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
			case BANG_EQUAL:
				return !isEqual(left, right);
			case EQUAL_EQUAL:
				return isEqual(left, right);
			case MODULO:
				return (double)left % (double)right;
			case PLUS:
				if (left instanceof Double && right instanceof Double) return (double)left + (double)right;
				if (left instanceof String && right instanceof String) return (String)left + (String)right;				
				if(left instanceof Double && right instanceof String || left instanceof String && right instanceof Double) {
					if (left instanceof Double) left = stringify(left);
					if (right instanceof Double) right = stringify(right);
					return (String)left + (String)right;
				}
				
				throw new RuntimeError(expr.operator, "Operands may be comprised of numbers and strings only.");
		}
		
		return null;
	}
	
	@Override
	public Object visitTernaryExpr(Ternary expr) {
		// TODO Auto-generated method stub
		Object right = evaluate(expr.right);
		Object left = evaluate(expr.left);
		Object condition = evaluate(expr.condition);
		
		if(isTruthy(condition)) {
			return left;
		}
		return right;
	}
	
	private Object lookUpVariable(Token name, Expr expr) {
		Integer distance = locals.get(expr);
		if (distance != null) {
			return environment.getAt(distance, name.lexeme);
		} else {
			return globals.get(name);
		}
	}
	
	public void resolve(Expr expr, int depth) {
		locals.put(expr, depth);
	}
	
	private void execute(Stmt stmt) {
		stmt.accept(this);
	}
	
	private void checkNumberOperands(Token operator, Object...operands) {
		for(Object operand : operands) {
			if(operand instanceof Double) { continue; }
			throw new RuntimeError(operator, "Operand must be a number.");
		}
	}
	
	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double) return;
		throw new RuntimeError(operator, "Operand must be a number.");
	}
	
	private boolean isEqual(Object a, Object b) {
		if (a == null && b == null) return true;
		if (a == null) return false;
		if (b == null) return false;
		
		return a.equals(b);
	}
	
	private boolean isTruthy(Object object) {
		if (object == null) return false;
		if (object instanceof Boolean) return (boolean)object;
		return true;
	}
	
	public Object evaluate(Expr expr) {
		return expr.accept(this);
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

}
