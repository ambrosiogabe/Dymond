package dymond;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import static dymond.TokenType.*;

public class Parser {
	private static class ParseError extends RuntimeException {}
	
	private final List<Token> tokens;
	private int current = 0;
	private final boolean repl;
	
	public Parser(List<Token> tokens, boolean repl) {
		this.tokens = tokens;
		this.repl = repl;
	}
	
	// parse -> declaration* EOF
	public List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<>();
		try {
			while (!isAtEnd()) {
				statements.add(declaration());
			}
			
			return statements;
		} catch(ParseError error) {
			return statements;
		}
	}
	
	private Stmt declaration() {
		try {
			if (match(VAR)) return varDeclaration();
			
			return statement();
		} catch (ParseError error) {
			synchronize();
			return null;
		}
	}
	
	private Stmt varDeclaration() {
		Token name = consume(IDENTIFIER, "Expect a variable name.");
		
		Expr initializer = null;
		if (match(EQUAL)) {
			initializer = expression();
		}
		
		if(!repl)
			consume(SEMICOLON, "Expect ';' after variable declaration.");
		else
			match(SEMICOLON);
		return new Stmt.Var(name,  initializer);
	}
	
	// statement -> exprStmt
	//            | forStmt
	//            | ifStmt
	//            | whileStmt
	//            | funcStmt
	//            | block
	//            | returnStmt
	//            | classDecl
	private Stmt statement() {
		if (match(LEFT_BRACE)) return new Stmt.Block(block());
		if (match(IF)) return ifStatement();
		if (match(WHILE)) return whileStatement();
		if (match(FOR)) return forStatement();
		if (match(FUNC)) return function("function");
		if (match(RETURN)) return returnStatement();
		if (match(BREAK)) return breakStatement();
		if (match(NEXT)) return nextStatement();
		if (match(CLASS)) return classDeclaration();
		
		return expressionStatement();
	}
	
	private Stmt classDeclaration() {
		Token name = consume(IDENTIFIER, "Expect a class name.");
		consume(LEFT_BRACE, "Expect '{' before class body.");
		
		List<Stmt.Function> methods = new ArrayList<>();
		List<Stmt.Function> staticMethods = new ArrayList<>(); 
		while (!check(RIGHT_BRACE) && !isAtEnd()) {
			if (match(STATIC)) staticMethods.add(function("staticMethod"));
			else if (match(FUNC)) methods.add(function("method"));
		}
		
		consume(RIGHT_BRACE, "Expect '}' after class body.");
		
		return new Stmt.Class(name,  methods, staticMethods);
	}
	
	private Stmt returnStatement() {
		Token keyword = previous();
		Expr value = null;
		if (!check(SEMICOLON)) {
			value = expression();
		}
		
		consume(SEMICOLON, "Expect ';' after return value.");
		return new Stmt.Return(keyword, value);
	}
	
	private Stmt breakStatement() {
		Token keyword = previous();
		consume(SEMICOLON, "Expect ';' after break.");
		return new Stmt.Break(keyword);
	}
	
	private Stmt nextStatement() {
		Token keyword = previous();
		consume(SEMICOLON, "Expect ';' after next.");
		return new Stmt.Next(keyword);
	}
	
	private Stmt.Function function(String kind) {
		Token name = null;
		
		if (check(IDENTIFIER))
			name = consume(IDENTIFIER, "Expect " + kind + " name.");
		
		consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
		List<Token> parameters = new ArrayList<>();
		if (!check(RIGHT_PAREN)) {
			do {
				if (parameters.size() >= 32) {
					error(peek(), "Cannot have more than 32 parameters.");
				}
				
				parameters.add(consume(IDENTIFIER, "Expect a paramter name."));
			} while (match(COMMA));
		}
		consume(RIGHT_PAREN, "Expect ')' after parameters.");
		
		consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
		List<Stmt> body = block();
		return new Stmt.Function(name,  parameters, body);
	}
	
	private Stmt forStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'for'.");
		
		Stmt initializer;
		if (match(SEMICOLON)) {
			initializer = null;
		} else if (match(VAR)) {
			initializer = varDeclaration();
		} else {
			initializer = expressionStatement();
		}
		
		Expr condition = null;
		if (!check(SEMICOLON)) {
			condition = expression();
		}
		consume(SEMICOLON, "Expect ';' after loop condition.");
		
		Expr increment = null;
		if (!check(RIGHT_PAREN)) {
			increment = expression();
		}
		consume(RIGHT_PAREN, "Expect ')' after for clauses.");
		
		Stmt body = statement();
		
		if (increment != null) {
			body = new Stmt.Block(Arrays.asList(
					body,
					new Stmt.Expression(increment)
				));		
		}
		
		if (condition == null) condition = new Expr.Literal(true);
		body = new Stmt.For(condition, body, new Stmt.Expression(increment));
		
		if (initializer != null) {
			body = new Stmt.Block(Arrays.asList(initializer, body));
		}
		
		return body;
	}
	
	private Stmt whileStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'while'.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expect ')' after condition.");
		
		Stmt body = null;
		body = statement();
		
		return new Stmt.While(condition, body);
	}
	
	private Stmt ifStatement() {
		consume(LEFT_PAREN, "Expect '(' after 'if'.");
		Expr condition = expression();
		consume(RIGHT_PAREN, "Expect ')' after if condition.");
		
		Stmt thenBranch = statement();
		Stmt elseBranch = null;
		if (match(ELSE)) {
			elseBranch = statement();
		}
		
		return new Stmt.If(condition, thenBranch, elseBranch);
	}
	
	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<>();
		
		while (!check(RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}
		
		consume(RIGHT_BRACE, "Expect '}' after block.");
		return statements;
	}
	
	private Stmt expressionStatement() {
		Expr expr = expression();
		
		if(!repl)
			consume(SEMICOLON, "Expect ';' after value.");
		else
			match(SEMICOLON);
		return new Stmt.Expression(expr);
	}
	
	private Expr expression() {
		return comma();
	}
	
	// comma -> ternary equality
	private Expr comma() {
		Expr expr = assignment();
		
		while (match(COMMA)) {
			expr = assignment();
		}
		
		return expr;
	}
	
	// assignment -> identifier = assignment
	//             | ternary
	private Expr assignment() {
		Expr expr = anonFunction();
		
		if (match(EQUAL, PLUS_EQUAL, MINUS_EQUAL, MODULO_EQUAL, TIMES_EQUAL, DIV_EQUAL)) {
			Token equals = previous();
			Expr value = assignment();
			
			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable)expr).name;
				return new Expr.Assign(name,  value, equals);
			} else if (expr instanceof Expr.Get) {
				Expr.Get get = (Expr.Get)expr;
				return new Expr.Set(get.object, get.name, value, equals);
			}
			
			error(equals, "Invalid assignment target.");
		}
		
		return expr;
	}
	
	private Expr anonFunction() {
		Expr expr = ternary();
		
		/* TODO implement anonymus functions*/
		
		return expr;
	}
	
	private Expr ternary() {
		Expr expr = logic_or();
		
		if (match(QUESTION)) {
			Expr left = logic_or();
			
			consume(COLON, "Expected ':' a colon.");
			
			Expr right = expression();
			expr = new Expr.Ternary(left, right, expr);
		}
		
		return expr;
	}
	
	private Expr logic_or() {
		Expr expr = logic_and();
		
		while (match(OR)) {
			Token operator = previous();
			Expr right = logic_and();
			expr = new Expr.Logical(expr,  operator, right);
		}
		
		return expr;
	}
	
	private Expr logic_and() {
		Expr expr = equality();
		
		while (match(AND)) {
			Token operator = previous();
			Expr right = equality();
			expr = new Expr.Logical(expr,  operator, right);
		}
		
		return expr;
	}
	
	// equality -> comparison ( ("!=" | "==" ) comparison )* ;
	private Expr equality() {
		Expr expr = comparison();
		
		while (match(BANG_EQUAL, EQUAL_EQUAL)) {
			Token operator = previous();
			Expr right = comparison();

			expr = new Expr.Binary(expr,  operator,  right);
		}
		
		return expr;
	}
	
	// comparison -> addition ( ( ">" | ">=" | "<" | "<=" ) addition )* ;
	private Expr comparison() {
		Expr expr = addition();
		
		while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
			Token operator = previous();
			Expr right = addition();
			expr = new Expr.Binary(expr,  operator, right);
		}
		
		return expr;
	}
	
	private Expr addition() {
		Expr expr = multiplication();
		
		while (match(MINUS, PLUS)) {
			Token operator = previous();
			Expr right = addition();
			expr = new Expr.Binary(expr,  operator, right);
		}
		
		return expr;
	}
	
	private Expr multiplication() {
		Expr expr = unary();
		
		while (match(DIV, STAR, INTEGER_DIV, MODULO)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}
	
	// unary -> ( "!" | "-" ) unary
	//       | primary ;
	private Expr unary() {
		if (match(BANG, MINUS, PLUS_PLUS, MINUS_MINUS)) {
			Token operator = previous();
			Expr right = postfix();
			if (right instanceof Expr.Variable) {
				Token name = ((Expr.Variable)right).name;
				return new Expr.UnaryAssign(name, operator);
			} 
			return new Expr.Unary(operator, right);
		}
		
		return postfix();
	}
	
	// postfix ++ --
	private Expr postfix() {
		Expr expr = call();
		
		if (match(PLUS_PLUS, MINUS_MINUS)) {
			Token operator = previous();
			
			if(expr instanceof Expr.Variable) { 
				Token name = ((Expr.Variable)expr).name;
				return new Expr.UnaryAssign(name, operator);
			} 
			return new Expr.Unary(operator, expr);
		}
		
		return expr;
	}
	
	// call -> primary ( "(" arguments? )" )* ;
	private Expr call() {
		Expr expr = primary();
		
		while (true) {
			if (match(LEFT_PAREN)) {
				expr = finishCall(expr);
			} else if (match(DOT)) {
				Token name = consume(IDENTIFIER, "Expect property name after '.'.");
				expr = new Expr.Get(expr, name);
			} else {
				break;
			}
		}
		
		return expr;
	}
	
	// primary -> NUMBER | STRING | "False" | "True" | "Null" | "(" expression ")" ;
	private Expr primary() {
		if (match(FALSE)) return new Expr.Literal(false);
		if (match(TRUE)) return new Expr.Literal(true);
		if (match(NULL)) return new Expr.Literal(null);
		if (match(IDENTIFIER)) return new Expr.Variable(previous());
		if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal);
		if (match(THIS)) return new Expr.This(previous());
		
		if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}
		
		throw error(peek(), "Expect expression.");
	}
	
	
	
	
	
	private Expr finishCall(Expr callee) {
		List<Expr> arguments = new ArrayList<>();
		if (!check(RIGHT_PAREN)) {
			do {
				if (arguments.size() >= 32) {
					error(peek(), "Cannot have more than 32 arguments.");
				}
				arguments.add(assignment());
			} while (match(COMMA));
		}
		
		Token paren = consume(RIGHT_PAREN, "Expect ')' after arguments.");
		
		return new Expr.Call(callee, paren, arguments);
	}
	
	
	private Token consume(TokenType type, String message) {
		if (check(type)) return advance();
		throw error(previous(), message);
	}
	
	private ParseError error(Token token, String message) {
		Dymond.error(token.line, message, token.lineText, token.column);
		return new ParseError();
	}
	
	private void synchronize() {
		advance();
		
		while (!isAtEnd()) {
			if (previous().type == SEMICOLON) return;
			
			switch (peek().type) {
				case CLASS:
				case FUNC:
				case FOR:
				case IF:
				case WHILE:
				case RETURN:
					return;
			}
			
			advance();
		}
	}

	
	
	
	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if(check(type)) {
				advance();
				return true;
			}
		}
		
		return false;
	}
	
	private boolean check(TokenType tokenType) {
		if (isAtEnd()) return false;
		return peek().type == tokenType;
	}
	
	private Token advance() {
		if (!isAtEnd()) current++;
		return previous();
	}
	
	private boolean isAtEnd() {
		return peek().type == EOF;
	}
	
	private Token peek() {
		return tokens.get(current);
	}
	
	private Token previous() {
		return tokens.get(current - 1);
	}
}