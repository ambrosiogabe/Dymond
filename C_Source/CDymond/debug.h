#ifndef cdymond_debug_h
#define cdymond_debug_h

#include "chunk.h"

void disassembleChunk(Chunk* chunk, const char* name);
int disassembleInstruction(Chunk* chunk, int i);

#endif
