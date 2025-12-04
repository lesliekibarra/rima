#ifndef MATMUL_COMMON_H
#define MATMUL_COMMON_H

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

static inline uint64_t read_cycle(void) {
    uint64_t c;
    asm volatile ("rdcycle %0" : "=r"(c));
    return c;
}

static inline uint64_t read_instret(void) {
    uint64_t c;
    asm volatile ("rdinstret %0" : "=r"(c));
    return c;
}


#define MATMUL_M 2
#define MATMUL_K 2
#define MATMUL_N 2

// ---------------------
// Test matrices
// ---------------------
// Small 2x2 int8 test matrices: C = A * B
static int8_t A[MATMUL_M * MATMUL_K] = {
    1, 2,
    3, 4
};

static int8_t B[MATMUL_K * MATMUL_N] = {
    5,  6,
    7,  8
};

// Software result buffer
static int32_t C_sw[MATMUL_M * MATMUL_N];

// ---------------------
// Software matmul
// ---------------------
static inline void matmul_sw(void)
{
    for (int i = 0; i < MATMUL_M; i++) {
        for (int j = 0; j < MATMUL_N; j++) {
            int32_t acc = 0;
            for (int k = 0; k < MATMUL_K; k++) {
                int8_t a = A[i * MATMUL_K + k];
                int8_t b = B[k * MATMUL_N + j];
                acc += (int32_t)a * (int32_t)b;
            }
            C_sw[i * MATMUL_N + j] = acc;
        }
    }
}

#endif
