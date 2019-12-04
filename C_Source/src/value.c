#include <stdio.h>
#include <string.h>

#include "object.h"
#include "memory.h"
#include "value.h"

void initValueArray(ValueArray* array)
{
	array->values = nullptr;
	array->count = 0;
	array->capacity = 0;
}

void writeValueArray(ValueArray* array, Value value)
{
	if (array->capacity < array->count + 1)
	{
		int oldCapacity = array->capacity;
		array->capacity = GROW_CAPACITY(oldCapacity);
		array->values = GROW_ARRAY(array->values, Value, oldCapacity, array->capacity);
	}

	array->values[array->count] = value;
	array->count++;
}

void freeValueArray(ValueArray* array)
{
	FREE_ARRAY(Value, array->values, array->capacity);
	initValueArray(array);
}

void printValue(Value value, bool printStringEscapedChars)
{
	switch (value.type)
	{
	case VAL_BOOL:    printf(AS_BOOL(value) ? "true" : "false"); break;
	case VAL_NULL:    printf("null"); break;
	case VAL_NUMBER:  printf("%g", AS_NUMBER(value)); break;
	case VAL_OBJ:     printObject(value, printStringEscapedChars); break;
	}
}

bool valuesEqual(Value a, Value b)
{
	if (a.type != b.type) return false;

	switch (a.type)
	{
	case VAL_BOOL:   return AS_BOOL(a) == AS_BOOL(b); break;
	case VAL_NUMBER: return AS_NUMBER(a) == AS_NUMBER(b); break;
	case VAL_NULL:   return true;
	case VAL_OBJ:    return AS_OBJ(a) == AS_OBJ(b);
	}
}