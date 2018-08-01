package dymond;

import java.util.ArrayList;
import java.util.List;

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
	//            | ifStmt
	//            | printStmt
	//            | block
	private Stmt statement() {
		if (match(PRINT)) return printStatement();
		if (match(LEFT_BRACE)) return new Stmt.Block(block());
		if (match(IF)) return ifStatement();

		return expressionStatement();
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
	
	private Stmt printStatement() {
		Expr value = expression();
		
		if(!repl)
			consume(SEMICOLON, "Expect ';' after value.");
		else 
			match(SEMICOLON);
		return new Stmt.Print(value);
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
		Expr expr = ternary();
		
		if (match(EQUAL, PLUS_EQUAL, MINUS_EQUAL, MODULO_EQUAL, TIMES_EQUAL, DIV_EQUAL)) {
			Token equals = previous();
			Expr value = assignment();
			
			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable)expr).name;
				return new Expr.Assign(name,  value, equals);
			}
			
			error(equals, "Invalid assignment target.");
		}
		
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
			return new Expr.Unary(operator, right);
		}
		
		return postfix();
	}
	
	// postfix ++ --
	private Expr postfix() {
		Expr expr = primary();
		
		if (match(PLUS_PLUS, MINUS_MINUS)) {
			Token operator = previous();
			return new Expr.Unary(operator, expr);
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
		
		if (match(LEFT_PAREN)) {
			Expr expr = expression();
			consume(RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}
		
		throw error(peek(), "Expect expression.");
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