package lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lexer.TokenType.*;

public class Scanner {
	  private static final Map<String, TokenType> keywords;

	  static {
	    keywords = new HashMap<>();
	    keywords.put("and",    AND);
	    keywords.put("class",  CLASS);
	    keywords.put("else",   ELSE);
	    keywords.put("false",  FALSE);
	    keywords.put("for",    FOR);
	    keywords.put("func",   FUNC);
	    keywords.put("if",     IF);
	    keywords.put("nil",    NIL);
	    keywords.put("or",     OR);
	    keywords.put("print",  PRINT);
	    keywords.put("return", RETURN);
	    keywords.put("super",  SUPER);
	    keywords.put("this",   THIS);
	    keywords.put("true",   TRUE);
	    keywords.put("var",    VAR);
	    keywords.put("while",  WHILE);
	  }
	  
	private final String source;
	private final List<Token> tokens = new ArrayList<>();
	
	private int start = 0;
	private int current = 0;
	private int line = 1;
	
	// This will be used for error handling, it will contain each entire line
	private String currentLine = "";
	private int currentColumn = -1;
	
	public Scanner(String source) {
		this.source = source;
	}
	
	public List<Token> scanTokens() {
		while (!isAtEnd()) {
			start = current;
			scanToken();
		}
		
		tokens.add(new Token(EOF, "", null, line, "", -1));
		return tokens;
	}
	
	private void scanToken() {
		char c = advance();
		switch (c) {
			case '(': addToken(LEFT_PAREN); break;
			case ')': addToken(RIGHT_PAREN); break;
			case '{': addToken(LEFT_BRACE); break;
			case '}': addToken(RIGHT_BRACE); break;
			case ',': addToken(COMMA); break;
			case '.': 
				if(isDigit(peek())) {
					number();
					break;
				}
				addToken(DOT); 
				break;
			case '-': addToken(MINUS); break;
			case '+': addToken(PLUS); break;
			case ';': addToken(SEMICOLON); break;
			case '*': addToken(STAR); break;
			case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
			case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL);break;
			case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
			case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;
			case '/': 
				if (match('/')) {
					addToken(INTEGER_DIV);
					break;
				} else if (match('*')) {
					while(peek() != '*' && peekNext() != '/') {
						if(source.charAt(current) == '\n') {
							line++;
							currentLine = "";
							currentColumn = -1;
						}
						advance();
					}
					advance();
					advance();
					break;
				}
				addToken(match('/') ? INTEGER_DIV : DIV); break;
			case '#': while(peek() != '\n' && !isAtEnd()) advance(); break;
			case ' ':
			case '\r':
			case '\t':
				// Ignore whitespace
				break;
			case '\n':
				line++;
				currentLine = "";
				currentColumn = -1;
				break;
			case '"': string(); break;
			default:
				if (isDigit(c)) {
					number();
				} else if (isAlpha(c)) {
					identifier();
				} else {
					Dymond.error(line,  "Unexpected character.", currentLine, currentColumn);
				}
				break;
		}
	}
	
	private void identifier() {
		while(isAlphaNumeric(peek())) advance();
		
		String text = source.substring(start, current);
		
		TokenType type = keywords.get(text);
		if (type == null) type = IDENTIFIER;
		addToken(type);
		
		addToken(IDENTIFIER);
	}
	
	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			if(peek() == '\n') {
				line++;
				currentLine = "";
				currentColumn = -1;
			}
			advance();
		}
		
		if(isAtEnd()) {
			Dymond.error(line,  "Unterminated String.", currentLine, currentColumn);
			return;
		}
		
		advance();
		
		String value = source.substring(start + 1, current - 1);
		addToken(STRING, value);
	}
	
	private void number() {
		while (isDigit(peek())) advance();
		
		boolean hasE = false;
		if (peek() == '.' && (isDigit(peekNext()) || (peekNext() == 'e' && isDigit(peekNextNext()))
				|| (peekNext() == 'E' && isDigit(peekNextNext())))) {
			advance();
			
			while (isDigit(peek())) { 
				advance();
			}
			
			if ( (peek() == 'e' || peek() == 'E') && (isDigit(peekNext()) || 
					( (peekNext() == '-' && isDigit(peekNextNext())) || (peekNext() == '+' && isDigit(peekNextNext()))))) {
				advance();
				while (isDigit(peek())) advance();
				
				if( (peek() == '-' || peek() == '+') && isDigit(peekNext())) {
					advance();
					while (isDigit(peek())) advance();
				}
				
				if(peek() == '.') {
					Dymond.error(line, "Unexpected number literal.", currentLine, currentColumn);
				}
			}
		}
		
		if ( (peek() == 'e' || peek() == 'E') && (isDigit(peekNext()) || 
			( (peekNext() == '-' && isDigit(peekNextNext())) || (peekNext() == '+' && isDigit(peekNextNext()))))) {
			advance();
			while (isDigit(peek())) advance();
			
			if( (peek() == '-' || peek() == '+') && isDigit(peekNext())) {
				advance();
				while (isDigit(peek())) advance();
			}
			
			if(peek() == '.') {
				Dymond.error(line, "Unexpected number literal.", currentLine, currentColumn);
			}
		}
		
		addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
	}
	
	private char advance() {
		current++;
		currentColumn++;
		currentLine += source.charAt(current - 1);
		return source.charAt(current - 1);
	}
	
	private char peek() {
		if(isAtEnd()) return '\0';
		return source.charAt(current);
	}
	
	private char peekNext() {
		if(current + 1 >= source.length()) return '\0';
		return source.charAt(current + 1);
	}
	
	private char peekNextNext() {
		if(current + 2 >= source.length()) return '\0';
		return source.charAt(current + 2);
	}
	
	private void addToken(TokenType type) {
		addToken(type, null);
	}
	
	private boolean match(char expected) {
		if(isAtEnd()) return false;
		if(source.charAt(current) != expected) return false;
		
		current++;
		currentLine += source.charAt(current - 1);
		currentColumn++;
		return true;
	}
	
	private void addToken(TokenType type, Object literal) {
		String text = source.substring(start, current);
		tokens.add(new Token(type, text, literal, line, currentLine, currentColumn));
	}
	
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}
	
	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') ||
				(c >= 'A' && c <= 'Z') ||
				c == '_';
	}
	
	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}
	
	private boolean isAtEnd() {
		return current >= source.length();
	}
}
