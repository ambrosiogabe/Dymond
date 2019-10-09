#include "common.h"
#include "chunk.h"
#include "debug.h"
#include "vm.h"
#include <stdlib.h>

int main(int argc, const char* argv[]) {
	initVM();

	Chunk chunk;
	initChunk(&chunk);

	writeConstant(&chunk, 1.2, 123);
	writeConstant(&chunk, 3.4, 123);
	writeChunk(&chunk, OP_ADD, 123);

	writeConstant(&chunk, 5.6, 123);
	writeChunk(&chunk, OP_DIVIDE, 123);

	writeChunk(&chunk, OP_RETURN, 200);
	
	disassembleChunk(&chunk, "Test chunk");
	interpret(&chunk);
	freeVM();
	freeChunk(&chunk);
	system("Pause");
	return 0;
}