package dymond;

import static dymond.TokenType.*;

import dymond.Expr.Ternary;

public class Interpreter implements Expr.Visitor<Object> {
	
	public void interpret(Expr expression) {
		try {
			Object value = evaluate(expression);
			System.out.println(stringify(value));
		} catch (RuntimeError error) {
			Dymond.runtimeError(error);
		}
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
				
				throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
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
