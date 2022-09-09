#ifndef clox_chunk_h
#define clox_chunk_h

#include "common.h"
#include "value.h"

// Value-less Enum's enumerations are represented as 0-indexed uint8_t
typedef enum {
	OP_CONSTANT,
	OP_ADD,
	OP_SUBTRACT,
	OP_MULTIPLY,
	OP_DIVIDE,
	OP_NEGATE,
	OP_RETURN,
} OpCode;

typedef struct {
	int count;
	int capacity;
	
	// Pointer to start of uint8_t dynamic array (OpCodes)
	uint8_t* code;

	// Pointer to start of int dynamic array (Line numbers)
	int* lines;

	// Actual dynamic array of constants
	ValueArray constants;
} Chunk;

void initChunk(Chunk* chunk);
void freeChunk(Chunk* chunk);
void writeChunk(Chunk* chunk, uint8_t byte, int line);
int addConstant(Chunk* chunk, Value value);

#endif