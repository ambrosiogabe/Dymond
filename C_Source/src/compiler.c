#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "common.h"
#include "compiler.h"
#include "scanner.h"

#ifdef DEBUG_PRINT_CODE 
#include "debug.h"
#endif

typedef struct
{
	Token current;
	Token previous;
	bool hadError;
	bool panicMode;
} Parser;

typedef enum
{
	PREC_NONE,
	PREC_ASSIGNMENT, // = += -= /= *= %=
	PREC_OR,         // or
	PREC_AND,        // and
	PREC_EQUALITY,   // == !=
	PREC_COMPARISON, // < > <= >=
	PREC_TERM,       // + -
	PREC_FACTOR,     // * /
	PREC_UNARY,      // ! - prefix ++ prefix --
	PREC_POST_UNARY, // ++ --
	PREC_CALL,       // . () [] 
	PREC_PRIMARY
} Precedence;

typedef void (*ParseFn)(bool canAssign);

typedef struct
{
	ParseFn prefix;
	ParseFn infix;
	Precedence precedence;
} ParseRule;

typedef struct
{
	Token name;
	int depth;
} Local;

typedef struct Compiler
{
	Local locals[UINT8_COUNT];
	int localCount;
	int scopeDepth;
} Compiler;

Parser parser;
Compiler* current = nullptr;
Chunk* compilingChunk;

static Chunk* currentChunk()
{
	return compilingChunk;
}

static void errorAt(Token* token, const char* message)
{
	if (parser.panicMode) return;
	parser.panicMode = true;

	fprintf(stderr, "[line %d] Error", token->line);

	if (token->type == TOKEN_EOF)
	{
		fprintf(stderr, " at end");
	}
	else if (token->type == TOKEN_ERROR)
	{
		// Nothing.
	}
	else
	{
		fprintf(stderr, " at '%.*s'", token->length, token->start);
	}

	fprintf(stderr, ": %s\n", message);
	parser.hadError = true;
}

static void error(const char* message)
{
	errorAt(&parser.previous, message);
}

static void errorAtCurrent(const char* message)
{
	errorAt(&parser.current, message);
}

static void advance()
{
	parser.previous = parser.current;

	for (;;)
	{
		parser.current = scanToken();
		if (parser.current.type != TOKEN_ERROR) break;

		errorAtCurrent(parser.current.start);
	}
}

static void consume(TokenType type, const char* message)
{
	if (parser.current.type == type)
	{
		advance();
		return;
	}

	errorAtCurrent(message);
}

static bool check(TokenType type)
{
	return parser.current.type == type;
}

static bool match(TokenType type)
{
	if (!check(type)) return false;
	advance();
	return true;
}

static void emitByte(uint8_t byte)
{
	writeChunk(currentChunk(), byte, parser.previous.line);
}

static void emitBytes(uint8_t byte1, uint8_t byte2)
{
	emitByte(byte1);
	emitByte(byte2);
}

static void emitLongBytes(uint8_t byte1, uint16_t constant)
{
	uint8_t lowerHalf = constant & 0xFF;
	uint8_t upperHalf = (constant & 0xFF00) >> 8;
	emitByte(byte1);
	emitByte(upperHalf);
	emitByte(lowerHalf);
}

static void emitReturn()
{
	emitByte(OP_RETURN);
}

static uint16_t makeConstant(Value value)
{
	int constant = addConstant(currentChunk(), value);
	if (constant > UINT16_MAX)
	{
		error("Too many constants in one chunk.");
		return 0;
	}

	return (uint16_t)constant;
}

static void emitConstant(Value value)
{
	writeConstant(currentChunk(), value, parser.previous.line);
}

static void initCompiler(Compiler* compiler)
{
	compiler->localCount = 0;
	compiler->scopeDepth = 0;
	current = compiler;
}

static void endCompiler()
{
	emitReturn();
#ifdef DEBUG_PRINT_CODE 
	if (!parser.hadError)
	{
		disassembleChunk(currentChunk(), "code");
	}
#endif
}

static void beginScope()
{
	current->scopeDepth++;
}

static void endScope()
{
	// TODO implement OP_POPN where it pops all locals at once
	current->scopeDepth--;

	while (current->localCount > 0 && current->locals[current->localCount - 1].depth > current->scopeDepth)
	{
		emitByte(OP_POP);
		current->localCount--;
	}
}

static void expression();
static void statement();
static void declaration();
static ParseRule* getRule(TokenType type);
static void parsePrecedence(Precedence precedence);

static uint16_t identifierConstant(Token* name)
{
	return makeConstant(OBJ_VAL(copyString(name->start, name->length)));
}

static bool identifiersEqual(Token* a, Token* b)
{
	if (a->length != b->length) return false;
	return memcmp(a->start, b->start, a->length) == 0;
}

static int resolveLocal(Compiler* compiler, Token* name)
{
	for (int i = compiler->localCount - 1; i >= 0; i--)
	{
		Local* local = &compiler->locals[0];
		if (identifiersEqual(name, &local->name))
		{
			if (local->depth == -1)
			{
				error("Cannot read local variable in its onwn initializer.");
			}
			return i;
		}
	}

	return -1;
}

static void addLocal(Token name)
{
	if (current->localCount == UINT8_COUNT)
	{
		error("Too many local variables in function.");
		return;
	}

	Local* local = &current->locals[current->localCount++];
	local->name = name;
	local->depth = -1;
}

static void declareVariable()
{
	// Global variables are implicitly declared
	if (current->scopeDepth == 0) return;

	Token* name = &parser.previous;

	for (int i = current->localCount - 1; i >= 0; i--)
	{
		Local* local = &current->locals[i];
		if (local->depth != -1 && local->depth < current->scopeDepth)
		{
			break;
		}

		if (identifiersEqual(name, &local->name))
		{
			error("Redefinition of variable in the same scope: '%s'", local->name);
		}
	}

	addLocal(*name);
}

static uint16_t parseVariable(const char* errorMessage)
{
	consume(TOKEN_IDENTIFIER, errorMessage);

	declareVariable();
	if (current->scopeDepth > 0) return 0;

	return identifierConstant(&parser.previous);
}

static void markInitialized()
{
	current->locals[current->localCount - 1].depth = current->scopeDepth;
}

static void defineVariable(uint16_t global)
{
	printf("Current depth: %d\n", current->scopeDepth);
	if (current->scopeDepth > 0)
	{
		markInitialized();
		return;
	}

	if (global <= UINT8_MAX)
	{
		emitBytes(OP_DEFINE_GLOBAL, (uint8_t)global);
	}
	else if (global < UINT16_MAX)
	{
		emitLongBytes(OP_DEFINE_GLOBAL_LONG, global);
	}
	else
	{
		error("Too many constants in one chunk.");
	}
}

static void binary(bool canAssign)
{
	// Remember the operator 
	TokenType operatorType = parser.previous.type;

	// Compile the right operand 
	ParseRule* rule = getRule(operatorType);
	parsePrecedence((Precedence)(rule->precedence + 1));

	// Emit the operator instruction
	switch (operatorType)
	{
	case TOKEN_BANG_EQUAL:    emitBytes(OP_EQUAL, OP_NOT); break;
	case TOKEN_EQUAL_EQUAL:   emitByte(OP_EQUAL); break;
	case TOKEN_GREATER:       emitByte(OP_GREATER); break;
	case TOKEN_GREATER_EQUAL: emitByte(OP_LESS, OP_NOT); break;
	case TOKEN_LESS:          emitByte(OP_LESS); break;
	case TOKEN_LESS_EQUAL:    emitByte(OP_GREATER, OP_NOT); break;
	case TOKEN_PLUS:          emitByte(OP_ADD); break;
	case TOKEN_MINUS:         emitByte(OP_SUBTRACT); break;
	case TOKEN_TIMES:         emitByte(OP_MULTIPLY); break;
	case TOKEN_DIV:           emitByte(OP_DIVIDE); break;
	case TOKEN_INTEGER_DIV:   emitByte(OP_INT_DIVIDE); break;
	case TOKEN_MODULO:        emitByte(OP_MODULO); break;
	default:
		return;
	}
}

static void grouping(bool canAssign)
{
	expression();
	consume(TOKEN_RIGHT_PAREN, "Expect ')' after expression.");
}

static void number(bool canAssign)
{
	double value = strtod(parser.previous.start, nullptr);
	emitConstant(NUMBER_VAL(value));
}


// Add support for \n characters and stuff then add translate characters here
static void string(bool canAssign)
{
	emitConstant(OBJ_VAL(copyString(parser.previous.start + 1, parser.previous.length - 2)));
}

static void namedVariable(Token name, bool canAssign)
{
	uint8_t getOp, setOp;
	int arg = resolveLocal(current, &name);
	if (arg != -1)
	{
		getOp = OP_GET_LOCAL;
		setOp = OP_SET_LOCAL;
	}
	else
	{
		arg = identifierConstant(&name);
		getOp = OP_GET_GLOBAL;
		setOp = OP_SET_GLOBAL;
	}

	if (canAssign && match(TOKEN_EQUAL))
	{
		expression();
		if (arg <= UINT8_MAX)
			emitBytes(setOp, (uint8_t)arg);
		else
			emitLongBytes(OP_SET_GLOBAL_LONG, arg);
	}
	else
	{
		if (arg <= UINT8_MAX)
			emitBytes(getOp, (uint8_t)arg);
		else
			emitLongBytes(OP_GET_GLOBAL_LONG, arg);
	}
}

static void variable(bool canAssign)
{
	namedVariable(parser.previous, canAssign);
}

static void unary(bool canAssign)
{
	TokenType operatorType = parser.previous.type;

	// Compile the operand
	parsePrecedence(PREC_UNARY);

	// Emit the operator instruction
	switch (operatorType)
	{
	case TOKEN_MINUS:
		emitByte(OP_NEGATE);
		break;
	case TOKEN_PLUS_PLUS:
		emitConstant(NUMBER_VAL(1));
		emitByte(OP_ADD);
		break;
	case TOKEN_MINUS_MINUS:
		emitConstant(NUMBER_VAL(1));
		emitByte(OP_SUBTRACT);
		break;
	case TOKEN_BANG:
		emitByte(OP_NOT);
		break;
	default:
		return;
	}
}

static void postUnary(bool canAssign)
{
	TokenType operatorType = parser.previous.type;

	// We already have the operand on the stack
	switch (operatorType)
	{
	case TOKEN_PLUS_PLUS:
		emitConstant(NUMBER_VAL(1));
		emitByte(OP_ADD);
		break;
	case TOKEN_MINUS_MINUS:
		emitConstant(NUMBER_VAL(1));
		emitByte(OP_SUBTRACT);
		break;
	default:
		return;
	}
}

static void literal(bool canAssign)
{
	switch (parser.previous.type)
	{
	case TOKEN_FALSE: emitByte(OP_FALSE); break;
	case TOKEN_NULL: emitByte(OP_NULL); break;
	case TOKEN_TRUE: emitByte(OP_TRUE); break;
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
	{ unary,       nullptr,    PREC_NONE   },       // TOKEN_BANG            
	{ nullptr,     binary,     PREC_EQUALITY },     // TOKEN_BANG_EQUAL      
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_EQUAL        
	{ nullptr,     binary,     PREC_EQUALITY   },   // TOKEN_EQUAL_EQUAL     
	{ nullptr,     binary,     PREC_COMPARISON },   // TOKEN_GREATER         
	{ nullptr,     binary,     PREC_COMPARISON },   // TOKEN_GREATER_EQUAL   
	{ nullptr,     binary,     PREC_COMPARISON },   // TOKEN_LESS            
	{ nullptr,     binary,     PREC_COMPARISON },   // TOKEN_LESS_EQUAL      
	{ nullptr,     binary,     PREC_FACTOR },       // TOKEN_DIV     
	{ nullptr,     binary,     PREC_FACTOR },       // INTEGER_DIV 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_DIV_EQUAL 
	{ nullptr,     binary,     PREC_FACTOR },       // TOKEN_MODULO 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_MODULO_EQUAL 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_PLUS_EQUAL  
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_MINUS_EQUAL  
	{ unary,       postUnary,  PREC_UNARY  },       // TOKEN_PLUS_PLUS 
	{ unary,       postUnary,  PREC_UNARY  },       // TOKEN_MINUS_MINUS 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_TIMES_EQUAL 
	{ nullptr,     binary,     PREC_FACTOR },       // TOKEN_TIMES 
	{ variable,     nullptr,   PREC_NONE   },       // TOKEN_IDENTIFIER      
	{ string,      nullptr,    PREC_NONE   },       // TOKEN_STRING          
	{ number,      nullptr,    PREC_NONE   },       // TOKEN_NUMBER          
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_AND             
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_CLASS           
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_ELSE            
	{ literal,     nullptr,    PREC_NONE   },       // TOKEN_FALSE           
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_FOR             
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_FUNCTION             
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_IF              
	{ literal,     nullptr,    PREC_NONE   },       // TOKEN_NULL             
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_OR              
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_PRINT           
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_RETURN          
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_SUPER           
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_THIS            
	{ literal,     nullptr,    PREC_NONE   },       // TOKEN_TRUE            
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_VAR             
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_WHILE     
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_BREAK 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_NEXT 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_STATIC
	{ nullptr,     nullptr,    PREC_NONE   },       // LEFT_ARROW 
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_ERROR           
	{ nullptr,     nullptr,    PREC_NONE   },       // TOKEN_EOF             
};

static void parsePrecedence(Precedence precedence)
{
	advance();
	ParseFn prefixRule = getRule(parser.previous.type)->prefix;
	if (prefixRule == nullptr)
	{
		error("Expect expression.");
		return;
	}

	bool canAssign = precedence <= PREC_ASSIGNMENT;
	prefixRule(canAssign);

	while (precedence <= getRule(parser.current.type)->precedence)
	{
		advance();
		ParseFn infixRule = getRule(parser.previous.type)->infix;
		infixRule(canAssign);
	}

	if (canAssign && match(TOKEN_EQUAL))
	{
		error("Invalid assignment target.");
	}
}

static ParseRule* getRule(TokenType type)
{
	return &rules[type];
}

void expression()
{
	parsePrecedence(PREC_ASSIGNMENT);
}

static void block()
{
	while (!check(TOKEN_RIGHT_BRACE) && !check(TOKEN_EOF))
	{
		declaration();
	}

	consume(TOKEN_RIGHT_BRACE, "Expect '}' after block.");
}

static void varDeclaration()
{
	uint16_t global = parseVariable("Expect a variable name.");

	if (match(TOKEN_EQUAL))
	{
		expression();
	}
	else
	{
		emitByte(OP_NULL);
	}
	consume(TOKEN_SEMICOLON, "Expect ';' after variable declaration.");

	defineVariable(global);
}

static void expressionStatement()
{
	expression();
	consume(TOKEN_SEMICOLON, "Expect ';' after expression.");
	emitByte(OP_POP);
}

static void printStatement()
{
	expression();
	consume(TOKEN_SEMICOLON, "Expect ';' after value.");
	emitByte(OP_PRINT);
}

static void synchronize()
{
	parser.panicMode = false;

	while (parser.current.type != TOKEN_EOF)
	{
		if (parser.previous.type == TOKEN_SEMICOLON) return;

		switch (parser.current.type)
		{
		case TOKEN_CLASS:
		case TOKEN_FUNCTION:
		case TOKEN_VAR:
		case TOKEN_FOR:
		case TOKEN_IF:
		case TOKEN_WHILE:
		case TOKEN_PRINT:
		case TOKEN_RETURN:
			return;

		default:
			// do nothing
			;
		}

		advance();
	}
}

static void declaration()
{
	if (match(TOKEN_VAR))
	{
		varDeclaration();
	}
	else
	{
		statement();
	}

	if (parser.panicMode) synchronize();
}

static void statement()
{
	if (match(TOKEN_PRINT))
	{
		printStatement();
	}
	else if (match(TOKEN_LEFT_BRACE))
	{
		beginScope();
		block();
		endScope();
	}
	else
	{
		expressionStatement();
	}
}

bool compile(const char* source, Chunk* chunk)
{
	initScanner(source);
	Compiler compiler;
	initCompiler(&compiler);
	compilingChunk = chunk;

	parser.hadError = false;
	parser.panicMode = false;

	advance();

	while (!match(TOKEN_EOF))
	{
		declaration();
	}

	endCompiler();

	return !parser.hadError;
}