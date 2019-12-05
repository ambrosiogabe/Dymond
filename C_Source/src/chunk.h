#pragma once

#include "common.h"
#include "value.h"

typedef enum
{
	OP_CONSTANT,
	OP_CONSTANT_LONG,
	OP_NULL,
	OP_TRUE,
	OP_FALSE,
	OP_POP,
	OP_EQUAL,
	OP_GREATER,
	OP_LESS,
	OP_ADD,
	OP_SUBTRACT,
	OP_MULTIPLY,
	OP_DIVIDE,
	OP_NOT,
	OP_NEGATE,
	OP_INT_DIVIDE,
	OP_MODULO,
	OP_PRINT,
	OP_DEFINE_GLOBAL,
	OP_DEFINE_GLOBAL_LONG,
	OP_GET_GLOBAL,
	OP_GET_GLOBAL_LONG,
	OP_SET_GLOBAL,
	OP_SET_GLOBAL_LONG,
	OP_RETURN,
} OpCode;

typedef struct
{
	int line;
	uint8_t numInstructions;
} LineEncoding;

// The Chunk is a dynamic array of bytes
typedef struct
{
	int count;
	int capacity;
	uint8_t* code;
	int lineCount;
	int lineCapacity;
	LineEncoding* lineEncoding;
	ValueArray constants;
} Chunk;

void initChunk(Chunk* chunk);
void writeChunk(Chunk* chunk, uint8_t byte, int line);
int writeConstant(Chunk* chunk, Value value, int line);
int addConstant(Chunk* chunk, Value value);
int getLine(Chunk* chunk, int instructionNumber);
int hasLine(Chunk* chunk, int line);
void freeChunk(Chunk* chunk);