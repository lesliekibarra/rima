#ifndef RIMA_H
#define RIMA_H

#include <stdint.h>
#include "rocc.h"

// -----------------------------------------------------------------------------
// RIMA funct codes (these must match what your RoCC accelerator decodes)
// -----------------------------------------------------------------------------
#define RIMA_FUNCT_SET_DIMS  0
#define RIMA_FUNCT_LOAD_A    1
#define RIMA_FUNCT_LOAD_B    2
#define RIMA_FUNCT_COMPUTE   3
#define RIMA_FUNCT_STORE_C   4

// -----------------------------------------------------------------------------
// Set matrix dimensions M x N and inner dimension K
// Encoded as:
//   rs1[15:0]   = M
//   rs1[31:16]  = N
//   rs2[15:0]   = K
// Uses ROCC_INSTRUCTION_SS: two source registers, no destination.
// -----------------------------------------------------------------------------
static inline void rima_set_dims(uint16_t M, uint16_t N, uint16_t K)
{
    uint32_t rs1 = ((uint32_t)N << 16) | (uint32_t)M;
    uint32_t rs2 = (uint32_t)K;

    // X = 0 => CUSTOM_0 opcode
    ROCC_INSTRUCTION_SS(0, rs1, rs2, RIMA_FUNCT_SET_DIMS);
}

// -----------------------------------------------------------------------------
// Load matrix A from memory starting at base_addr
// We pass base_addr in rs1, rs2 is unused (set to 0).
// -----------------------------------------------------------------------------
static inline void rima_load_A(uint64_t base_addr)
{
    uint64_t rs1 = base_addr;
    uint64_t rs2 = 0;

    ROCC_INSTRUCTION_SS(0, rs1, rs2, RIMA_FUNCT_LOAD_A);
}

// -----------------------------------------------------------------------------
// Load matrix B from memory starting at base_addr
// -----------------------------------------------------------------------------
static inline void rima_load_B(uint64_t base_addr)
{
    uint64_t rs1 = base_addr;
    uint64_t rs2 = 0;

    ROCC_INSTRUCTION_SS(0, rs1, rs2, RIMA_FUNCT_LOAD_B);
}

// -----------------------------------------------------------------------------
// Trigger the matrix multiply compute phase
// No operands needed: we just use the funct field.
// This matches ROCC_INSTRUCTION(X, funct) in rocc.h
// -----------------------------------------------------------------------------
static inline void rima_compute(void)
{
    ROCC_INSTRUCTION(0, RIMA_FUNCT_COMPUTE);
}

// -----------------------------------------------------------------------------
// Store result matrix C to memory starting at base_addr
// -----------------------------------------------------------------------------
static inline void rima_store_C(uint64_t base_addr)
{
    uint64_t rs1 = base_addr;
    uint64_t rs2 = 0;

    ROCC_INSTRUCTION_SS(0, rs1, rs2, RIMA_FUNCT_STORE_C);
}

#endif // RIMA_H