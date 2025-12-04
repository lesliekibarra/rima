#include "matmul_common.h"


int main(void)
{
    printf("Software-only %dx%dx%d matmul test\n",
           MATMUL_M, MATMUL_K, MATMUL_N);

    uint64_t c0 = read_cycle();
    uint64_t i0 = read_instret();
    matmul_sw();
    uint64_t c1 = read_cycle();
    uint64_t i1 = read_instret();

    printf("SW result:\n");
    for (int i = 0; i < MATMUL_M * MATMUL_N; i++) {
        printf("C_sw[%d] = %d\n", i, C_sw[i]);
    }

    printf("SW: cycles=%llu, instret=%llu\n",
           (unsigned long long)(c1 - c0),
           (unsigned long long)(i1 - i0));

    return 0;
}