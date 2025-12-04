#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>

#include "rima.h"

// Small 2x2 int8 test matrices
// C = A * B (2x2)
static int8_t A[4] = {
  1, 2,
  3, 4
};

static int8_t B[4] = {
  5,  6,
  7,  8
};

// Where hardware will write its result
static int32_t C_hw[4];

// Software reference
static int32_t C_sw[4];

static void matmul_sw_2x2()
{
  for (int i = 0; i < 2; i++) {
    for (int j = 0; j < 2; j++) {
      int32_t acc = 0;
      for (int k = 0; k < 2; k++) {
        int8_t a = A[i*2 + k];
        int8_t b = B[k*2 + j];
        acc += (int32_t)a * (int32_t)b;
      }
      C_sw[i*2 + j] = acc;
    }
  }
}

int main(void)
{
  printf("Rima accelerator 2x2 matmul test\n");

  // Set matrix dims: M = 2, N = 2, K = 2
  uint16_t M = 2;
  uint16_t N = 2;
  uint16_t K = 2;

  rima_set_dims(M, N, K);

  // Issue DMA loads for A and B
  // Your DmaLoad expects a base address; we just pass array addresses
  rima_load_A((uint64_t)(uintptr_t)A);
  rima_load_B((uint64_t)(uintptr_t)B);

  // Start compute
  rima_compute();

  // Store results to C_hw
  rima_store_C((uint64_t)(uintptr_t)C_hw);

  // For now, we *assume* each RoCC command is blocking
  // and everything is done when store_C returns.
  // (This matches a simple "busy until done" RoCC design.)

  // Compute software reference result
  matmul_sw_2x2();

  // Compare
  int pass = 1;
  for (int i = 0; i < 4; i++) {
    printf("C_hw[%d] = %d, C_sw[%d] = %d\n", i, C_hw[i], i, C_sw[i]);
    if (C_hw[i] != C_sw[i]) {
      pass = 0;
    }
  }

  if (pass) {
    printf("Rima matmul: PASS\n");
  } else {
    printf("Rima matmul: FAIL\n");
  }

  return pass ? 0 : 1;
}