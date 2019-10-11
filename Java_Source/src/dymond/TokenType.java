package dymond;

public enum TokenType {
	// Single character tokens
	LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
	COMMA, DOT, MINUS, PLUS, SEMICOLON, STAR, SINGLE_LINE_COMMENT,
	QUESTION, COLON, LEFT_BRACKET, RIGHT_BRACKET,
	
	// One or two character tokens
	BANG, BANG_EQUAL,
	EQUAL, EQUAL_EQUAL,
	GREATER, GREATER_EQUAL,
	LESS, LESS_EQUAL,
	DIV, INTEGER_DIV,
	MODULO, MODULO_EQUAL,
	PLUS_EQUAL, MINUS_EQUAL,
	PLUS_PLUS, MINUS_MINUS,
	TIMES_EQUAL, DIV_EQUAL,
	
	// Literals
	IDENTIFIER, STRING, NUMBER,
	
	// Keywords
	AND, CLASS, ELSE, FALSE, FUNC, FOR, IF, NULL, OR,
	RETURN, SUPER, THIS, TRUE, WHILE, VAR, BREAK, NEXT,
	STATIC, LEFT_ARROW,
	
	EOF
}
