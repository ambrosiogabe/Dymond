package dymond;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import dymond.Expr.Ternary;
import dymond.Expr.UnaryAssign;
import dymond.Stmt.Break;
import dymond.Stmt.For;
import dymond.Stmt.Next;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
	private enum FunctionType {
		NONE,
		FUNCTION,
		INITIALIZER,
		METHOD
	}
	
	private enum ClassType {
		NONE,
		CLASS,
		SUBCLASS
	}
	
	private final Interpreter interpreter;
	private final Stack<Map<String, Boolean>> scopes = new Stack<>();
	private FunctionType currentFunction = FunctionType.NONE;
	private ClassType currentClass = ClassType.NONE;
	
	public Resolver(Interpreter interpreter) {
		this.interpreter = interpreter;
	}
	
	@Override
	public Void visitSuperExpr(Expr.Super expr) {
		if (currentClass == ClassType.NONE) {
			Dymond.error(expr.keyword.line, "Cannot use 'super' outside of a class.", expr.keyword.lineText, expr.keyword.column);
		} else if (currentClass != ClassType.SUBCLASS) {
			Dymond.error(expr.keyword.line, "Cannot use 'super' in a class with no superclass.", expr.keyword.lineText, expr.keyword.column);
		}
		resolveLocal(expr, expr.keyword);
		return null;
	}
	
	@Override
	public Void visitThisExpr(Expr.This expr) {
		if (currentClass == ClassType.NONE) {
			Dymond.error(expr.keyword.line, "Cannot use 'this' outside of a class.", expr.keyword.lineText, expr.keyword.column);
			return null;
		}
		
		resolveLocal(expr, expr.keyword);
		return null;
	}
	
	@Override 
	public Void visitSetExpr(Expr.Set expr) {
		resolve(expr.value);
		resolve(expr.object);
		return null;
	}
	
	@Override 
	public Void visitGetExpr(Expr.Get expr) {
		resolve(expr.object);
		return null;
	}
	
	@Override
	public Void visitClassStmt(Stmt.Class stmt) {
		ClassType enclosingClass = currentClass;
		currentClass = ClassType.CLASS;
		
		declare(stmt.name);
		
		if (stmt.superclass != null) {
			currentClass = ClassType.SUBCLASS;
			resolve(stmt.superclass);
		}
		
		define(stmt.name);
		
		if (stmt.superclass != null) {
			beginScope();
			scopes.peek().put("super",  true);
		}
		
		beginScope();
		scopes.peek().put("this", true);
		
		for (Stmt.Function method : stmt.methods) {
			FunctionType declaration = FunctionType.METHOD;
			if (method.name.lexeme.equals("init")) {
				declaration = FunctionType.INITIALIZER;
			}
			resolveFunction(method, declaration);
		}
		
		for (Stmt.Function staticMethod : stmt.staticMethods) {
			FunctionType declaration = FunctionType.METHOD;
			resolveFunction(staticMethod, declaration);
		}
		
		endScope();
		
		if (stmt.superclass != null) endScope();
		
		currentClass = enclosingClass;
		return null;
	}
	
	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		beginScope();
		resolve(stmt.statements);
		endScope();
		return null;
	}
	
	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		declare(stmt.name);
		if (stmt.initializer != null) {
			resolve(stmt.initializer);
		}
		define(stmt.name);
		return null;
	}
	
	@Override
	public Void visitVariableExpr(Expr.Variable expr) {
		if (!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme)== Boolean.FALSE ) {
			Dymond.error(expr.name.line, "Cannot read local variable in its own initializer.", expr.name.lineText, expr.name.column);
		}
		
		resolveLocal(expr, expr.name);
		return null;
	}
	
	@Override
	public Void visitAssignExpr(Expr.Assign expr) {
		resolve(expr.value);
		resolveLocal(expr, expr.name);
		return null;
	}
	
	@Override
	public Void visitUnaryAssignExpr(UnaryAssign expr) {
		resolveLocal(expr, expr.name);
		return null;
	}
	
	@Override
	public Void visitFunctionStmt(Stmt.Function stmt) {
		declare(stmt.name);
		define(stmt.name);
		
		resolveFunction(stmt, FunctionType.FUNCTION);
		return null;
	}
	
	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		resolve(stmt.expression);
		return null;
	}
	
	@Override 
	public Void visitIfStmt(Stmt.If stmt) {
		resolve(stmt.condition);
		resolve(stmt.thenBranch);
		if (stmt.elseBranch != null) resolve(stmt.elseBranch);
		return null;
	}
	
	@Override
	public Void visitReturnStmt(Stmt.Return stmt) {
		if (currentFunction == FunctionType.NONE) {
			Dymond.error(stmt.keyword.line, "Cannot return from top-level code.", stmt.keyword.lineText, stmt.keyword.column);
		}
		
		if (stmt.value != null) {
			if (currentFunction == FunctionType.INITIALIZER) {
				Dymond.error(stmt.keyword.line, "Cannot return a value from an initializer.", stmt.keyword.lineText, stmt.keyword.column);
			}
			resolve(stmt.value);
		}
		
		return null;
	}
	
	@Override 
	public Void visitWhileStmt(Stmt.While stmt) {
		resolve(stmt.condition);
		resolve(stmt.body);
		return null;
	}
	
	@Override
	public Void visitBinaryExpr(Expr.Binary expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}
	
	@Override
	public Void visitCallExpr(Expr.Call expr) {
		resolve(expr.callee);
		
		for (Expr argument : expr.arguments) {
			resolve(argument);
		}
		
		return null;
	}
	
	@Override
	public Void visitGroupingExpr(Expr.Grouping expr) {
		resolve(expr.expression);
		return null;
	}
	
	@Override
	public Void visitLiteralExpr(Expr.Literal expr) {
		return null;
	}
	
	@Override
	public Void visitLogicalExpr(Expr.Logical expr) {
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}
	
	@Override
	public Void visitUnaryExpr(Expr.Unary expr) {
		resolve(expr.right);
		return null;
	}
	
	@Override
	public Void visitBreakStmt(Break stmt) {
		return null;
	}

	@Override
	public Void visitNextStmt(Next stmt) {
		return null;
	}

	@Override
	public Void visitForStmt(For stmt) {
		resolve(stmt.condition);
		resolve(stmt.increment);
		resolve(stmt.body);
		return null;
	}

	@Override
	public Void visitTernaryExpr(Ternary expr) {
		resolve(expr.condition);
		resolve(expr.left);
		resolve(expr.right);
		return null;
	}

	private void resolveFunction(Stmt.Function function, FunctionType type) {
		FunctionType enclosingFunction = currentFunction;
		currentFunction = type;
		
		beginScope();
		for (Token param : function.parameters) {
			declare(param);
			define(param);
		}
		resolve(function.body);
		endScope();
		currentFunction = enclosingFunction;
	}
	
	private void resolveLocal(Expr expr, Token name) {
		for (int i = scopes.size() - 1; i >= 0; i--) {
			if (scopes.get(i).containsKey(name.lexeme)) {
				interpreter.resolve(expr, scopes.size() - 1 - i);
				return;
			}
		}
	}
	
	private void declare(Token name) {
		if (scopes.isEmpty()) return;
		
		Map<String, Boolean> scope = scopes.peek();
		if (scope.containsKey(name.lexeme)) {
			Dymond.error(name.line, "Variable with this name already declared in this scope.", name.lineText, name.column);
		}
		scope.put(name.lexeme,  false);
	}
	
	private void define(Token name) {
		if (scopes.isEmpty()) return;
		scopes.peek().put(name.lexeme, true);
	}
	
	private void beginScope() {
		scopes.push(new HashMap<String, Boolean>());
	}
	
	private void endScope() {
		scopes.pop();
	}
	
	public void resolve(List<Stmt> statements) {
		for (Stmt statement : statements) {
			resolve(statement);
		}
	}
	
	private void resolve(Stmt stmt) {
		stmt.accept(this);
	}
	
	private void resolve(Expr expr) {
		expr.accept(this);
	}
}
