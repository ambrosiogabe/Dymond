#pragma once

#include "common.h"
#include "value.h"

typedef enum {
	OP_CONSTANT,
	OP_CONSTANT_LONG,
	OP_ADD,
	OP_SUBTRACT,
	OP_MULTIPLY,
	OP_DIVIDE,
	OP_NEGATE,
	OP_RETURN,
} OpCode;

typedef struct {
	int line;
	uint8_t numInstructions;
} LineEncoding;

// The Chunk is a dynamic array of bytes
typedef struct {
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
void writeConstant(Chunk* chunk, Value value, int line);
int addConstant(Chunk* chunk, Value value);
int getLine(Chunk* chunk, int instructionNumber);
int hasLine(Chunk* chunk, int line);
void freeChunk(Chunk* chunk);