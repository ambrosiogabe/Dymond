#include <stdio.h>
#include <string.h>

#include "memory.h"
#include "object.h"
#include "value.h"
#include "vm.h"

#define ALLOCATE_OBJ(type, objectType) \
	(type*)allocateObject(sizeof(type), objectType)

static Obj* allocateObject(size_t size, ObjType type) {
	Obj* object = (Obj*)reallocate(NULL, 0, size);
	object->type = type;
	
	object->next = vm.objects;
	vm.objects = object;
	return object;
}

static ObjString* allocateString(char* chars, int length) {
	ObjString* string = ALLOCATE_OBJ(ObjString, OBJ_STRING);
	string->length = length;
	string->chars = chars;

	return string;
}

ObjString* takeString(char* chars, int length) {
	return allocateString(chars, length);
}

ObjString* copyString(const char* chars, int length) {
	int sanitizedLength = length;
	for (char* c = chars; c < chars + length; c++) {
		if (*c == '\\' && c + 1 < chars + length) {
			switch (*(c + 1)) {
				case 'n':
					*c = '\n';
					sanitizedLength--;
					break;
				case 't':
					*c = '\t';
					sanitizedLength--;
					break;
				case '"':
					*c = '"';
					sanitizedLength--;
					break;
				case 'b':
					*c = '\b';
					sanitizedLength--;
					break;
				case '\\':
					*c = '\\';
					sanitizedLength--;
					break;
				default:
					break;
			}
		}
	}

	char* heapChars = ALLOCATE(char, sanitizedLength + 1);
	int offset = 0;
	for (int i=0; i < length; i++) {
		heapChars[i - offset] = chars[i];
		if (chars[i] == '\n' || chars[i] == '\t' || chars[i] == '"' || chars[i] == '\b' || chars[i] == '\\') {
			i++;
			offset++;
		}
	}
	heapChars[sanitizedLength] = '\0';

	return allocateString(heapChars, sanitizedLength);
}

void printObject(Value value, bool printStringEscapedChars) {
	switch (OBJ_TYPE(value)) {
		case OBJ_STRING: {
			if (printStringEscapedChars) {
				ObjString* string = AS_STRING(value);
				for (int i = 0; i < string->length; i++) {
					if (string->chars[i] != '\n' && string->chars[i] != '\t' && string->chars[i] != '\\' &&
						string->chars[i] != '\b')
						printf("%c", string->chars[i]);
					else {
						printf("\\");
						switch (string->chars[i]) {
						case '\n': printf("n"); break;
						case '\t': printf("t"); break;
						case '\b': printf("b"); break;
						case '\\': printf("\\"); break;
						}
					}
				}
			}
			else {
				printf("%s", AS_CSTRING(value));
			}
			break;
		}
	}
}