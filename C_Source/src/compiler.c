#include <stdio.h>
#include <stdlib.h>

#include "common.h"
#include "compiler.h"
#include "scanner.h"

#ifdef DEBUG_PRINT_CODE 
#include "debug.h"
#endif

typedef struct {
	Token current;
	Token previous;
	bool hadError;
	bool panicMode;
} Parser;

typedef enum {
	PREC_NONE,
	PREC_ASSIGNMENT, // = += -= /= *= %=
	PREC_OR,         // or
	PREC_AND,        // and
	PREC_EQUALITY,   // == !=
	PREC_COMPARISON, // < > <= >=
	PREC_TERM,       // + -
	PREC_FACTOR,     // * /
	PREC_UNARY,      // ! - ++ --
	PREC_CALL,       // . () []
	PREC_PRIMARY
} Precedence;

typedef void (*ParseFn)();

typedef struct {
	ParseFn prefix;
	ParseFn infix;
	Precedence precedence;
} ParseRule;

Parser parser;
Chunk* compilingChunk;

static Chunk* currentChunk() {
	return compilingChunk;
}

static void errorAt(Token* token, const char* message) {
	if (parser.panicMode) return;
	parser.panicMode = true;

	fprintf(stderr, "[line %d] Error", token->line);

	if (token->type == TOKEN_EOF) {
		fprintf(stderr, " at end");
	}
	else if (token->type == TOKEN_ERROR) {
		// Nothing.
	}
	else {
		fprintf(stderr, " at '%.*s'", token->length, token->start);
	}

	fprintf(stderr, ": %s\n", message);
	parser.hadError = true;
}

static void error(const char* message) {
	errorAt(&parser.previous, message);
}

static void errorAtCurrent(const char* message) {
	errorAt(&parser.current, message);
}

static void advance() {
	parser.previous = parser.current;

	for (;;) {
		parser.current = scanToken();
		if (parser.current.type != TOKEN_ERROR) break;

		errorAtCurrent(parser.current.start);
	}
}

static void consume(TokenType type, const char* message) {
	if (parser.current.type == type) {
		advance();
		return;
	}

	errorAtCurrent(message);
}

static void emitByte(uint8_t byte) {
	writeChunk(currentChunk(), byte, parser.previous.line);
}

static void emitBytes(uint8_t byte1, uint8_t byte2) {
	emitByte(byte1);
	emitByte(byte2);
}

static void emitReturn() {
	emitByte(OP_RETURN);
}

static void emitConstant(Value value) {
	if (!writeConstant(currentChunk(), value, parser.previous.line)) {
		error("Too many constants in one chunk.");
	}
}

static void endCompiler() {
	emitReturn();
#ifdef DEBUG_PRINT_CODE 
	if (!parser.hadError) {
		disassembleChunk(currentChunk(), "code");
	}
#endif
}

static void expression();
static ParseRule* getRule(TokenType type);
static void parsePrecedence(Precedence precedence);

static void binary() {
	// Remember the operator 
	TokenType operatorType = parser.previous.type;

	// Compile the right operand 
	ParseRule* rule = getRule(operatorType);
	parsePrecedence((Precedence)(rule->precedence + 1));

	// Emit the operator instruction
	switch (operatorType) {
		case TOKEN_PLUS:    emitByte(OP_ADD); break;
		case TOKEN_MINUS:   emitByte(OP_SUBTRACT); break;
		case TOKEN_TIMES:   emitByte(OP_MULTIPLY); break;
		case TOKEN_DIV:     emitByte(OP_DIVIDE); break;
		default:
			return;
	}
}

static void grouping() {
	expression();
	consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression.");
}

static void number() {
	double value = strtod(parser.previous.start, nullptr);
	emitConstant(value);
}

static void unary() {
	TokenType operatorType = parser.previous.type;

	// Compile the operand
	parsePrecedence(PREC_UNARY);

	// Emit the operator instruction
	switch (operatorType) {
		case TOKEN_MINUS: emitByte(OP_NEGATE); break;
		default:
			return;
	}
}

ParseRule rules[] = {
	{ grouping,    nullptr,    PREC_NONE   },       // TOKEN_LEFT_PAREN      
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_RIGHT_PAREN     
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_LEFT_BRACE
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_RIGHT_BRACE     
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_LEFT_BRACKET
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_RIGHT_BRACKET  
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_COMMA           
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_DOT             
	{ unary,       binary,     PREC_TERM   },       // TOKEN_MINUS     
	{ nullptr,     binary,     PREC_TERM   },       // TOKEN_PLUS            
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_SEMICOLON      
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_QUESTION
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_COLON    
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_BANG            
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_BANG_EQUAL      
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_EQUAL        
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_EQUAL_EQUAL     
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_GREATER         
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_GREATER_EQUAL   
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_LESS            
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_LESS_EQUAL      
	{ nullptr,     binary,     PREC_FACTOR },       // TOKEN_DIV     
	{ nullptr,     binary,     PREC_FACTOR },       // INTEGER_DIV 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_DIV_EQUAL 
	{ nullptr,     binary,     PREC_FACTOR },       // TOKEN_MODULO 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_MODULO_EQUAL 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_PLUS_EQUAL  
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_MINUS_EQUAL  
	{ nullptr,     nullptr,    PREC_TERM   },       // TOKEN_PLUS_PLUS 
	{ nullptr,     nullptr,    PREC_TERM   },       // TOKEN_MINUS_MINUS 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_TIMES_EQUAL 
	{ nullptr,     binary,     PREC_FACTOR },       // TOKEN_TIMES 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_IDENTIFIER      
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_STRING          
	{ number,      nullptr,    PREC_NONE   },       // TOKEN_NUMBER          
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_AND             
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_CLASS           
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_ELSE            
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_FALSE           
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_FOR             
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_FUNCTION             
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_IF              
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_NULL             
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_OR              
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_PRINT           
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_RETURN          
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_SUPER           
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_THIS            
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_TRUE            
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_VAR             
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_WHILE     
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_BREAK 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_NEXT 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_STATIC
	{ nullptr,     nullptr,    PREC_NONE   },       // LEFT_ARROW 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_ERROR           
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_EOF             
};

static void parsePrecedence(Precedence precedence) {
	advance();
	ParseFn prefixRule = getRule(parser.previous.type)->prefix;
	if (prefixRule == nullptr) {
		error("Expect expression.");
		return;
	}

	prefixRule();

	while (precedence <= getRule(parser.current.type)->precedence) {
		advance();
		ParseFn infixRule = getRule(parser.previous.type)->infix;
		infixRule();
	}
}

static ParseRule* getRule(TokenType type) {
	return &rules[type];
}

void expression() {
	parsePrecedence(PREC_ASSIGNMENT);
}

bool compile(const char* source, Chunk* chunk) {
	initScanner(source);
	compilingChunk = chunk;

	parser.hadError = false;
	parser.panicMode = false;

	advance();
	expression();
	consume(TOKEN_EOF, "Expect end of expression.");
	endCompiler();

	return !parser.hadError;
}