package dymond;

import static dymond.TokenType.*;

import java.util.List;

import dymond.Expr.Ternary;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
	private Environment environment = new Environment();
	private boolean repl;
	
	public void interpret(List<Stmt> statements, boolean repl) {
		try {
			this.repl = repl;
			
			for(Stmt statement : statements) {
				execute(statement);
			}
		} catch (RuntimeError error) {
			Dymond.runtimeError(error);
		}
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
		Object left = environment.get(expr.name);
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
		
		environment.assign(expr.name, finalValue);
		return finalValue;
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
		return environment.get(expr.name);
	}
	
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		Object result = evaluate(stmt.expression);
		if(repl)
			System.out.println(stringify(result));
		return null;
	}
	
	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		Object value = evaluate(stmt.expression);
		System.out.println(stringify(value));
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
			case PLUS_PLUS:
				return right = (double)right + 1;
			case MINUS_MINUS:
				return right = (double)right - 1;
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
	
	private Object evaluate(Expr expr) {
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
