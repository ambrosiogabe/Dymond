package lexer;

public class Token {
	private final TokenType type;
	private final String lexeme;
	private final Object literal;
	private final int line;
	private final int column;
	private final String lineText;
	
	public Token(TokenType type, String lexeme, Object literal, int line, String lineText, int column) {
		this.type = type;
		this.lexeme = lexeme;
		this.literal = literal;
		this.line = line;
		this.lineText = lineText;
		this.column = column;
	}
	
	public String toString() {
		return type + " " + lexeme + " " + literal;
	}
}
