#include <stdarg.h>
#include <stdio.h>
#include <string.h>

#include "common.h"
#include "compiler.h"
#include "object.h"
#include "memory.h"
#include "debug.h"
#include "vm.h"

VM vm;

static void resetStack()
{
	vm.sp = vm.stack;
}

static void runtimeError(const char* format, ...)
{
	va_list args;
	va_start(args, format);
	vfprintf(stderr, format, args);
	va_end(args);
	fputs("\n", stderr);

	size_t instruction = vm.ip - vm.chunk->code;
	int line = getLine(&vm.chunk, instruction);
	fprintf(stderr, "[line %d] in script\n", line);

	resetStack();
}

void initVM()
{
	resetStack();
	vm.objects = nullptr;
	initTable(&vm.globals);
	initTable(&vm.strings);
}

void freeVM()
{
	freeTable(&vm.globals);
	freeTable(&vm.strings);
	freeObjects();
}

void push(Value value)
{
	if (vm.sp - vm.stack > STACK_MAX)
	{
		printf("STACK OVERFLOW ");
		printf("%d ", (int)(vm.sp - vm.stack));
		printf("%d \n", STACK_MAX);
	}
	else
	{
		*vm.sp = value;
		vm.sp++;
	}
}

Value pop()
{
	vm.sp--;
	return *vm.sp;
}

static Value peek(int distance)
{
	return vm.sp[-1 - distance];
}

static bool isFalsey(Value value)
{
	return IS_NULL(value) || (IS_BOOL(value) && !AS_BOOL(value));
}

static void concatenate()
{
	ObjString* b = AS_STRING(pop());
	ObjString* a = AS_STRING(pop());

	int length = a->length + b->length;
	char* chars = ALLOCATE(char, length + 1);
	memcpy(chars, a->chars, a->length);
	memcpy(chars + a->length, b->chars, b->length);
	chars[length] = '\0';

	ObjString* result = takeString(chars, length);
	push(OBJ_VAL(result));
}

InterpretResult interpret(const char* source)
{
	Chunk chunk;
	initChunk(&chunk);

	if (!compile(source, &chunk))
	{
		freeChunk(&chunk);
		return INTERPRET_COMPILE_ERROR;
	}

	vm.chunk = &chunk;
	vm.ip = vm.chunk->code;

	InterpretResult result = run();

	freeChunk(&chunk);
	return result;
}

static InterpretResult run()
{
#define READ_BYTE() (*vm.ip++)
#define READ_CONSTANT() (vm.chunk->constants.values[READ_BYTE()])
#define READ_STRING() AS_STRING(READ_CONSTANT())

#define BINARY_OP(valueType, op) \
	do { \
		if (!IS_NUMBER(peek(0)) || !IS_NUMBER(peek(1))) { \
				runtimeError("Operands must be numbers."); \
				return INTERPRET_RUNTIME_ERROR; \
		} \
		\
		double b = AS_NUMBER(pop()); \
		double a = AS_NUMBER(pop()); \
		push(valueType(a op b)); \
	} while (false) 

	for (;;)
	{
#ifdef DEBUG_TRACE_EXECUTION
		printf("          ");
		for (Value* slot = vm.stack; slot < vm.sp; slot++)
		{
			printf("[");
			printValue(*slot, true);
			printf("]");
		}
		printf("\n");
		disassembleInstruction(vm.chunk, (int)(vm.ip - vm.chunk->code));
#endif 

		uint8_t instruction;
		switch (instruction = READ_BYTE())
		{
			case OP_CONSTANT: {
				Value constant = READ_CONSTANT();
				push(constant);
				break;
			}
			case OP_CONSTANT_LONG: {
				uint8_t upperHalf = READ_BYTE();
				uint8_t lowerHalf = READ_BYTE();
				int num = (upperHalf << 8) + lowerHalf;
				Value constant = vm.chunk->constants.values[num];
				push(constant);
				break;
			}
			case OP_NULL: push(NULL_VAL); break;
			case OP_TRUE: push(BOOL_VAL(true)); break;
			case OP_FALSE: push(BOOL_VAL(false)); break;
			case OP_POP: pop(); break;
			case OP_SET_GLOBAL: {
				ObjString* name = READ_STRING();
				if (tableSet(&vm.globals, name, peek(0)))
				{
					tableDelete(&vm.globals, name);
					runtimeError("Undefined variable '%s'", name->chars);
					return INTERPRET_RUNTIME_ERROR;
				}
				break;
			}
			case OP_SET_GLOBAL_LONG: {
				uint8_t upperHalf = READ_BYTE();
				uint8_t lowerHalf = READ_BYTE();
				ObjString* name = AS_STRING(vm.chunk->constants.values[((upperHalf << 8) + lowerHalf)]);
				if (tableSet(&vm.globals, name, peek(0)))
				{
					tableDelete(&vm.globals, name);
					runtimeError("Undefined variable '%s'", name->chars);
					return INTERPRET_RUNTIME_ERROR;
				}
				break;
			}
			case OP_GET_GLOBAL: {
				ObjString* name = READ_STRING();
				Value value;
				if (!tableGet(&vm.globals, name, &value))
				{
					runtimeError("Undefined variable '%s'.", name->chars);
					return INTERPRET_RUNTIME_ERROR;
				}
				push(value);
				break;
			}
			case OP_GET_GLOBAL_LONG: {
				uint8_t upperHalf = READ_BYTE();
				uint8_t lowerHalf = READ_BYTE();
				printf("Number: %02x%02x\n", upperHalf, lowerHalf);
				ObjString* name = AS_STRING(vm.chunk->constants.values[((upperHalf << 8) + lowerHalf)]);
				Value value;
				if (!tableGet(&vm.globals, name, &value))
				{
					runtimeError("Undefined variable '%s'.", name->chars);
					return INTERPRET_RUNTIME_ERROR;
				}
				push(value);
				break;
			}
			case OP_DEFINE_GLOBAL: {
				ObjString* name = READ_STRING();
				if (tableContains(&vm.globals, name))
				{
					runtimeError("Redefinition of global variable '%s'", name->chars);
					return INTERPRET_RUNTIME_ERROR;
				}
				tableSet(&vm.globals, name, peek(0));
				pop();
				break;
			}
			case OP_DEFINE_GLOBAL_LONG: {
				uint8_t upperHalf = READ_BYTE();
				uint8_t lowerHalf = READ_BYTE();
				ObjString* name = AS_STRING(vm.chunk->constants.values[((upperHalf << 8) + lowerHalf)]);

				if (tableContains(&vm.globals, name))
				{
					runtimeError("Redefinition of global variable '%s'", name->chars);
					return INTERPRET_RUNTIME_ERROR;
				}
				tableSet(&vm.globals, name, peek(0));
				pop();
				break;
			}
			case OP_EQUAL: {
				Value b = pop();
				Value a = pop();
				push(BOOL_VAL(valuesEqual(a, b)));
				break;
			}
			case OP_INT_DIVIDE: {
				if (!IS_NUMBER(peek(0)) || !IS_NUMBER(peek(1)))
				{
					runtimeError("Operands must be numbers.");
					return INTERPRET_RUNTIME_ERROR;
				}

				int b = AS_NUMBER(pop());
				int a = AS_NUMBER(pop());
				push(NUMBER_VAL(a / b));
				break;
			}
			case OP_MODULO: {
				if (!IS_NUMBER(peek(0)) || !IS_NUMBER(peek(1)))
				{
					runtimeError("Operands must be numbers.");
					return INTERPRET_RUNTIME_ERROR;
				}

				int b = AS_NUMBER(pop());
				int a = AS_NUMBER(pop());
				push(NUMBER_VAL(a % b));
				break;
			}
			case OP_ADD: {
				if (IS_STRING(peek(0)) && IS_STRING(peek(1)))
				{
					concatenate();
				}
				else if (IS_NUMBER(peek(0)) && IS_NUMBER(peek(1)))
				{
					BINARY_OP(NUMBER_VAL, +);
				}
				else
				{
					runtimeError("Operands must be two numbers or two strings.");
					return INTERPRET_RUNTIME_ERROR;
				}
				break;
			}
			case OP_SUBTRACT:    BINARY_OP(NUMBER_VAL, -); break;
			case OP_MULTIPLY:    BINARY_OP(NUMBER_VAL, *); break;
			case OP_DIVIDE:      BINARY_OP(NUMBER_VAL, / ); break;
			case OP_GREATER:     BINARY_OP(BOOL_VAL, > ); break;
			case OP_LESS:        BINARY_OP(BOOL_VAL, < ); break;
			case OP_NEGATE:
				if (!IS_NUMBER(peek(0)))
				{
					runtimeError("Operand must be a number.");
					return INTERPRET_RUNTIME_ERROR;
				}

				push(NUMBER_VAL(-AS_NUMBER(pop())));
				break;
			case OP_NOT:
				push(BOOL_VAL(isFalsey(pop())));
				break;
			case OP_PRINT: {
				printValue(pop(), false);
				printf("\n");
				break;
			}
			case OP_RETURN: {
				// Exit interpreter
				return INTERPRET_OK;
			}
		}
	}

#undef READ_BYTE
#undef READ_CONSTANT
#undef READ_STRING
#undef BINARY_OP
}