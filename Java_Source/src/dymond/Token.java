package dymond;

public class Token {
	private final TokenType type;
	public final String lexeme;
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
	
	public Token(TokenType type, String lexeme, Object literal, int line) {
		this.type = type;
		this.lexeme = lexeme;
		this.literal = literal;
		this.line = line;
		this.column = -1;
		this.lineText = "";
	}
	
	public String toString() {
		return type + " " + lexeme + " " + literal;
	}
}
