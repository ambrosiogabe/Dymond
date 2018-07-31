package dymond;

import java.util.List;

import static dymond.TokenType.*;

public class Parser {
	private static class ParseError extends RuntimeException {}
	
	private final List<Token> tokens;
	private int current = 0;
	
	public Parser(List<Token> tokens) {
		this.tokens = tokens;
	}
	
	public Expr parse() {
		try {
			return expression();
		} catch (ParseError error) {
			return null;
		}
	}
	
	
	private Expr expression() {
		return comma();
	}
	
	// comma -> ternary equality
	private Expr comma() {
		Expr expr = ternary();
		
		while (match(COMMA)) {
			expr = ternary();
		}
		
		return expr;
	}
	
	private Expr ternary() {
		Expr expr = equality();
		

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
		
		while (match(DIV, STAR, INTEGER_DIV)) {
			Token operator = previous();
			Expr right = unary();
			expr = new Expr.Binary(expr, operator, right);
		}
		
		return expr;
	}
	
	// unary -> ( "!" | "-" ) unary
	//       | primary ;
	private Expr unary() {
		if (match(BANG, MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}
		
		return primary();
	}
	
	// primary -> NUMBER | STRING | "False" | "True" | "Null" | "(" expression ")" ;
	private Expr primary() {
		if (match(FALSE)) return new Expr.Literal(false);
		if (match(TRUE)) return new Expr.Literal(true);
		if (match(NULL)) return new Expr.Literal(null);
		
		if (match(NUMBER, STRING)) {
			return new Expr.Literal(previous().literal);
		}
		
		if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}
		
		throw error(peek(), "Expect expression.");
	}
	
	
	
	
	
	
	
	private Token consume(TokenType type, String message) {
		if (check(type)) return advance();
		throw error(peek(), message);
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
				case VAR:
				case FOR:
				case IF:
				case WHILE:
				case PRINT:
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