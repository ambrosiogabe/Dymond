#include <stdlib.h>

#include "memory.h"
#include "value.h"
#include "chunk.h"

void initChunk(Chunk* chunk)
{
	chunk->capacity = 0;
	chunk->count = 0;
	chunk->lineCapacity = 0;
	chunk->lineCount = 0;
	chunk->code = nullptr;
	chunk->lineEncoding = nullptr;
	initValueArray(&chunk->constants);
}

void writeChunk(Chunk* chunk, uint8_t byte, int line)
{
	if (chunk->capacity < chunk->count + 1)
	{
		int oldCapacity = chunk->capacity;
		chunk->capacity = GROW_CAPACITY(oldCapacity);
		chunk->code = GROW_ARRAY(chunk->code, uint8_t, oldCapacity, chunk->capacity);
	}

	// Initialize the run-length line number encoding if size is zero
	// Or if the array needs to grow
	int lineIndex = hasLine(chunk, line);
	if (lineIndex == -1 && chunk->lineCapacity < chunk->lineCount + 1)
	{
		int oldCapacity = chunk->lineCapacity;
		chunk->lineCapacity = GROW_CAPACITY(oldCapacity);
		chunk->lineEncoding = GROW_ARRAY(chunk->lineEncoding, LineEncoding, oldCapacity, chunk->lineCapacity);
		for (int i = oldCapacity; i < chunk->lineCapacity; i++)
		{
			chunk->lineEncoding[i].numInstructions = 0;
			chunk->lineEncoding[i].line = -1;
		}
	}

	// If there was no line found, add it a new column to
	// the array to store numInstructions per this line
	if (lineIndex == -1)
	{
		lineIndex = chunk->lineCount;
		chunk->lineEncoding[lineIndex].line = line;
		chunk->lineCount++;
	}

	chunk->code[chunk->count] = byte;
	chunk->lineEncoding[lineIndex].numInstructions++;
	chunk->count++;
}

bool writeConstant(Chunk* chunk, Value value, int line)
{
	int constant = addConstant(chunk, value);
	if (constant < UINT8_MAX)
	{
		writeChunk(chunk, OP_CONSTANT, line);
		writeChunk(chunk, constant, line);
		return true;
	}

	if (constant > UINT16_MAX)
	{
		return false;
	}

	writeChunk(chunk, OP_CONSTANT_LONG, line);
	uint8_t lowerHalf = constant & 0xFF;
	uint8_t upperHalf = (constant & 0xFF00) >> 8;
	writeChunk(chunk, upperHalf, line);
	writeChunk(chunk, lowerHalf, line);
	return true;
}

int addConstant(Chunk* chunk, Value value)
{
	writeValueArray(&chunk->constants, value);
	return chunk->constants.count - 1;
}

int getLine(Chunk* chunk, int offset)
{
	for (int i = 0; i < chunk->lineCount; i++)
	{
		offset -= chunk->lineEncoding[i].numInstructions;
		if (offset < 0)
		{
			return chunk->lineEncoding[i].line;
		}
	}
	return -1;
}

int hasLine(Chunk* chunk, int line)
{
	for (int i = 0; i < chunk->lineCount; i++)
	{
		if (chunk->lineEncoding[i].line == line)
		{
			return i;
		}
	}
	return -1;
}

void freeChunk(Chunk* chunk)
{
	FREE_ARRAY(uint8_t, chunk->code, chunk->capacity);
	FREE_ARRAY(LineEncoding, chunk->lineEncoding, chunk->lineCapacity);
	freeValueArray(&chunk->constants);
	initChunk(chunk);
}