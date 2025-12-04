#include "matmul_common.h"
#include "rima.h"


// Where hardware will write its result
static int32_t C_hw[4];

int main(void)
{
  printf("Rima accelerator %dx%dx%d matmul test\n",
           MATMUL_M, MATMUL_K, MATMUL_N);

  uint16_t M = MATMUL_M;
  uint16_t N = MATMUL_N;
  uint16_t K = MATMUL_K;

  // --- RIMA path ---
  uint64_t c0 = read_cycle();
  uint64_t i0 = read_instret();

  rima_set_dims(M, N, K);
  rima_load_A((uint64_t)(uintptr_t)A);
  rima_load_B((uint64_t)(uintptr_t)B);
  rima_compute();
  rima_store_C((uint64_t)(uintptr_t)C_hw);

  uint64_t c1 = read_cycle();
  uint64_t i1 = read_instret();

  // --- Software reference (for correctness) ---
  matmul_sw();  // fills C_sw

  int pass = 1;
  for (int i = 0; i < MATMUL_M * MATMUL_N; i++) {
      printf("C_hw[%d] = %d, C_sw[%d] = %d\n",
              i, C_hw[i], i, C_sw[i]);
      if (C_hw[i] != C_sw[i]) {
          pass = 0;
      }
  }

  printf("RIMA: cycles=%llu, instret=%llu\n",
          (unsigned long long)(c1 - c0),
          (unsigned long long)(i1 - i0));

  if (pass) {
      printf("Rima matmul: PASS\n");
  } else {
      printf("Rima matmul: FAIL\n");
  }

  return pass ? 0 : 1;
}