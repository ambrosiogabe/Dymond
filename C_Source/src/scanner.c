#include <stdio.h>
#include <string.h>

#include "common.h"
#include "scanner.h"

typedef struct {
	const char* start;
	const char* current;
	int line;
} Scanner;

Scanner scanner;

void initScanner(const char* source) {
	scanner.start = source;
	scanner.current = source;
	scanner.line = 1;
}

static bool isAlpha(char c) {
	return (c >= 'a' && c <= 'z') ||
		   (c >= 'A' && c <= 'Z') ||
		    c == '_';
}

static bool isDigit(char c) {
	return c >= '0' && c <= '9';
}

static bool isAtEnd() {
	return *scanner.current == '\0';
}

static char advance() {
	scanner.current++;
	return scanner.current[-1];
}

static char peek() {
	return *scanner.current;
}

static char peekNext() {
	if (isAtEnd()) return '\0';
	return scanner.current[1];
}

static char peekNextNext() {
	if (isAtEnd()) return '\0';
	if (peek() == '\0') return '\0';
	return scanner.current[2];
}

static bool match(char expected) {
	if (isAtEnd()) return false;
	if (*scanner.current != expected) return false;

	scanner.current++;
	return true;
}

static Token makeToken(TokenType type) {
	Token token;
	token.type = type;
	token.start = scanner.start;
	token.length = (int)(scanner.current - scanner.start);
	token.line = scanner.line;

	return token;
}

static Token errorToken(const char* message) {
	Token token;
	token.type = TOKEN_ERROR;
	token.start = message;
	token.length = (int)strlen(message);
	token.line = scanner.line;

	return token;
}

static void skipWhitespace() {
	for (;;) {
		char c = peek();
		switch (c) {
			case ' ':
			case '\r':
			case '\t':
				advance();
				break;
			
			case '\n':
				scanner.line++;
				advance();
				break;

			case '#':
				while (peek() != '\n' && !isAtEnd()) advance();
				break;

			case '/':
				if (peekNext() == '*') {
					while (!isAtEnd() && peek() != '*' && peekNext() != '/') {
						if (*scanner.current == '\n') {
							scanner.line++;
						}
						advance();
					}
					if (!isAtEnd()) advance(); // We check if it's not at the line just in case there's an unclosed
					if (!isAtEnd()) advance(); // multiline comment at the end of the file
					break;
				}
				else {
					return;
				}
				break;

			default:
				return;
		}
	}
}

static TokenType checkKeyword(int start, int length, const char* rest, TokenType type) {
	printf(rest);
	if ((int)(scanner.current - scanner.start) == (start + length) && memcmp(scanner.start + start, rest, length) == 0) {
		return type;
	}

	return TOKEN_IDENTIFIER;
}

static TokenType identifierType() {
	switch (scanner.start[0]) {
		case 'a': return checkKeyword(1, 2, "nd", TOKEN_AND);
		case 'b': return checkKeyword(1, 4, "reak", TOKEN_BREAK);
		case 'c': return checkKeyword(1, 4, "lass", TOKEN_CLASS);
		case 'e': return checkKeyword(1, 3, "lse", TOKEN_ELSE);
		case 'i': return checkKeyword(1, 1, "f", TOKEN_IF);
		case 'o': return checkKeyword(1, 4, "rint", TOKEN_PRINT);
		case 'r': return checkKeyword(1, 5, "eturn", TOKEN_RETURN);
		case 'v': return checkKeyword(1, 2, "ar", TOKEN_VAR);
		case 'w': return checkKeyword(1, 4, "hile", TOKEN_WHILE);
		case 's': 
		case 'n': 
			if (scanner.current - scanner.start > 1) {
				switch (scanner.start[1]) {
					case 'u': return checkKeyword(2, 2, "ll", TOKEN_NULL);
					case 'e': return checkKeyword(2, 2, "xt", TOKEN_NEXT);
				}
			}
			break;
			if (scanner.current - scanner.start > 1) {
				switch (scanner.start[1]) {
					case 'u': return checkKeyword(2, 3, "per", TOKEN_SUPER);
					case 't': return checkKeyword(2, 4, "atic", TOKEN_STATIC);
				}
			}
			break;
		case 'f':
			if (scanner.current - scanner.start > 1) {
				switch (scanner.start[1]) {
					case 'a': return checkKeyword(2, 3, "lse", TOKEN_FALSE);
					case 'o': return checkKeyword(2, 1, "r", TOKEN_FOR);
					case 'u': return checkKeyword(2, 6, "nction", TOKEN_FUNCTION);
				}
			}
			break;
		case 't':
			if (scanner.current - scanner.start > 1) {
				switch (scanner.start[1]) {
					case 'h': return checkKeyword(2, 2, "is", TOKEN_THIS);
					case 'r': return checkKeyword(2, 2, "ue", TOKEN_TRUE);
				}
			}
			break;
	}

	return TOKEN_IDENTIFIER;
}

static Token identifier() {
	while (isAlpha(peek()) || isDigit(peek())) advance();

	return makeToken(identifierType());
}

static Token string() {
	while (peek() != '"' && !isAtEnd()) {
		if (peek() == '\\' && peekNext() == '"') advance();
		if (peek() == '\n') scanner.line++;
		advance();
	}

	if (isAtEnd()) return errorToken("Unterminated string.");

	// The closing quote
	advance();
	return makeToken(TOKEN_STRING);
}

static Token number() {
	while (isDigit(peek())) advance();

	bool hasE = false;
	if (peek() == '.' && (isDigit(peekNext()) || (peekNext() == 'e' && isDigit(peekNextNext()))
		|| (peekNext() == 'E' && isDigit(peekNextNext())))) {
		advance();

		while (isDigit(peek())) {
			advance();
		}

		if ((peek() == 'e' || peek() == 'E') && (isDigit(peekNext()) ||
			((peekNext() == '-' && isDigit(peekNextNext())) || (peekNext() == '+' && isDigit(peekNextNext()))))) {
			advance();
			while (isDigit(peek())) advance();

			if ((peek() == '-' || peek() == '+') && isDigit(peekNext())) {
				advance();
				while (isDigit(peek())) advance();
			}

			if (peek() == '.') {
				return errorToken("Unexpected number literal.");
			}
		}
	}

	if ((peek() == 'e' || peek() == 'E') && (isDigit(peekNext()) ||
		((peekNext() == '-' && isDigit(peekNextNext())) || (peekNext() == '+' && isDigit(peekNextNext()))))) {
		advance();
		while (isDigit(peek())) advance();

		if ((peek() == '-' || peek() == '+') && isDigit(peekNext())) {
			advance();
			while (isDigit(peek())) advance();
		}

		if (peek() == '.') {
			return errorToken("Unexpected number literal.");
		}
	}

	return makeToken(TOKEN_NUMBER);
}

Token scanToken() {
	skipWhitespace();

	scanner.start = scanner.current;

	if (isAtEnd()) return makeToken(TOKEN_EOF);

	char c = advance();

	if (isAlpha(c)) return identifier();
	if (isDigit(c)) return number();

	switch (c) {
		case '(': return makeToken(TOKEN_LEFT_PAREN);
		case ')': return makeToken(TOKEN_RIGHT_PAREN);
		case '{': return makeToken(TOKEN_LEFT_BRACE);
		case '}': return makeToken(TOKEN_RIGHT_BRACE);
		case '[': return makeToken(TOKEN_LEFT_BRACKET);
		case ']': return makeToken(TOKEN_RIGHT_BRACKET);
		case ';': return makeToken(TOKEN_SEMICOLON);
		case ':': return makeToken(TOKEN_COLON);
		case ',': return makeToken(TOKEN_COMMA);
		case '?': return makeToken(TOKEN_QUESTION);
		case '.': return makeToken(TOKEN_DOT);
		case '/': 
			return makeToken(match('/') ? TOKEN_INTEGER_DIV : match('=') ? TOKEN_DIV_EQUAL : TOKEN_DIV);
		case '<':
			return makeToken(match('=') ? TOKEN_LESS_EQUAL : match('-') ? TOKEN_LEFT_ARROW : TOKEN_LESS);
		case '-': 
			return makeToken(match('=') ? TOKEN_MINUS_EQUAL : match('-') ? TOKEN_MINUS_MINUS : TOKEN_MINUS);
		case '+': 
			return makeToken(match('=') ? TOKEN_PLUS_EQUAL : match('+') ? TOKEN_PLUS_PLUS : TOKEN_PLUS);
		case '*': 
			return makeToken(match('=') ? TOKEN_TIMES_EQUAL : TOKEN_TIMES);
		case '%':
			return makeToken(match('=') ? TOKEN_MODULO_EQUAL : TOKEN_MODULO);
		case '!':
			return makeToken(match('=') ? TOKEN_BANG_EQUAL : TOKEN_BANG);
		case '=':
			return makeToken(match('=') ? TOKEN_EQUAL_EQUAL : TOKEN_EQUAL);
		case '>':
			return makeToken(match('=') ? TOKEN_GREATER_EQUAL : TOKEN_GREATER);
		case '"': return string();
	}

	return errorToken("Unexpected character.");
}