package lexer;

public enum TokenType {
	// Single character tokens
	LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
	COMMA, DOT, MINUS, PLUS, SEMICOLON, STAR, SINGLE_LINE_COMMENT,
	
	// One or two character tokens
	BANG, BANG_EQUAL,
	EQUAL, EQUAL_EQUAL,
	GREATER, GREATER_EQUAL,
	LESS, LESS_EQUAL,
	DIV, INTEGER_DIV,
	
	// Literals
	IDENTIFIER, STRING, NUMBER,
	
	// Keywords
	AND, CLASS, ELSE, FALSE, FUNC, FOR, IF, NIL, OR,
	PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,
	
	EOF
}
