#include <stdio.h>

#include "common.h"
#include "compiler.h"
#include "debug.h"
#include "vm.h"

VM vm;

static void resetStack() {
	vm.sp = vm.stack;
}

void initVM() {
	resetStack();
}

void freeVM() {

}

void push(Value value) {
	if (vm.sp - vm.stack > STACK_MAX) {
		printf("STACK OVERFLOW ");
		printf("%d ", (int)(vm.sp - vm.stack));
		printf("%d \n", STACK_MAX);
	}
	else {
		*vm.sp = value;
		vm.sp++;
	}
}

Value pop() {
	vm.sp--;
	return *vm.sp;
}

InterpretResult interpret(const char* source) {
	Chunk chunk;
	initChunk(&chunk);

	if (!compile(source, &chunk)) {
		freeChunk(&chunk);
		return INTERPRET_COMPILE_ERROR;
	}

	vm.chunk = &chunk;
	vm.ip = vm.chunk->code;

	InterpretResult result = run();

	freeChunk(&chunk);
	return result;
}

static InterpretResult run() {
#define READ_BYTE() (*vm.ip++)
#define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])
#define READ_CONSTANT_LONG() (vm.chunk->constants.values[(READ_BYTE() << 8) + READ_BYTE()])

#define BINARY_OP(op) \
	do { \
		double b = pop(); \
		double a = pop(); \
		push(a op b); \
	} while(false) 

	for (;;) {
#ifdef DEBUG_TRACE_EXECUTION
		printf("          ");
		for (Value* slot = vm.stack; slot < vm.sp; slot++) {
			printf("[");
			printValue(*slot);
			printf("]");
		}
		printf("\n");
		disassembleInstruction(vm.chunk, (int)(vm.ip - vm.chunk->code));
#endif 

		uint8_t instruction;
		switch (instruction = READ_BYTE()) {
			case OP_CONSTANT: {
				Value constant = READ_CONSTANT();
				push(constant);
				break;
			}
			case OP_CONSTANT_LONG: {
				Value constant = READ_CONSTANT_LONG();
				push(constant);
				break;
			}
			case OP_ADD:       BINARY_OP(+); break;
			case OP_SUBTRACT:  BINARY_OP(-); break;
			case OP_MULTIPLY:  BINARY_OP(*); break;
			case OP_DIVIDE:    BINARY_OP(/); break;
			case OP_NEGATE:    push(-pop()); break;
			case OP_RETURN: {
				printValue(pop());
				printf("\n");
				return INTERPRET_OK;
			}
		}
	}

#undef READ_BYTE
#undef READ_CONSTANT
#undef BINARY_OP
}