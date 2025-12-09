#include <math.h>
#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>   // for abs()
#include <riscv-pk/encoding.h>


#include "tensorflow/lite/core/c/common.h"
#include "tensorflow/lite/micro/micro_mutable_op_resolver.h"
#include "tensorflow/lite/micro/micro_interpreter.h"
#include "tensorflow/lite/micro/tflite_bridge/micro_error_reporter.h"
#include "tensorflow/lite/schema/schema_generated.h"

// #include "hello_world_float_model_data.h"
#include "hello_world_int8_model_data.h"

// ---- timing helper (file-scope, after includes) ----
/*static inline uint64_t rdcycle(void) {
    return read_csr(mcycle);
}*/

namespace {
    using HelloWorldOpResolver = tflite::MicroMutableOpResolver<1>;

    TfLiteStatus RegisterOps(HelloWorldOpResolver& op_resolver) {
    TF_LITE_ENSURE_STATUS(op_resolver.AddFullyConnected());
    return kTfLiteOk;
    }
}  // namespace

float RunSingleInference(float input_value) {
    const tflite::Model* model = tflite::GetModel(g_hello_world_int8_model_data);
    if (model->version() != TFLITE_SCHEMA_VERSION) return -1;

    HelloWorldOpResolver op_resolver;
    if (RegisterOps(op_resolver) != kTfLiteOk) return -1;

    constexpr int kTensorArenaSize = 2048;
    static uint8_t tensor_arena[kTensorArenaSize];

    tflite::MicroInterpreter interpreter(model, op_resolver, tensor_arena, kTensorArenaSize);
    if (interpreter.AllocateTensors() != kTfLiteOk) return -1;

    TfLiteTensor* input = interpreter.input(0);
    TfLiteTensor* output = interpreter.output(0);

    // Quantize the input
    int8_t q_input = static_cast<int8_t>(input_value / input->params.scale + input->params.zero_point);
    input->data.int8[0] = q_input;

    if (interpreter.Invoke() != kTfLiteOk) return -1;

    // Dequantize output
    float y_pred = (output->data.int8[0] - output->params.zero_point) * output->params.scale;
    return y_pred;
}

int main(int argc, char* argv[]) {
    printf("Hello World Test\n");
    printf("Sanity Check:\n");
    printf("int test: %d\n");
    printf("hex test: 0x%x\n", 0xABCD);

    printf("Starting Single Inference\n");
    float test_input = 0.5f;
    uint64_t start = rdcycle();
    float prediction = RunSingleInference(test_input);
    uint64_t end   = rdcycle();
    uint64_t cycles = end - start;

    printf("RunSingleInference took %lu cycles\n",
        (unsigned long)cycles);


    int in_scaled  = (int)(test_input * 100.0f);      // 2 decimal places
    int out_scaled = (int)(prediction * 10000.0f);    // 4 decimal places

    printf("Predicted output for %d.%02d is %d.%04d\n",
        in_scaled / 100,  abs(in_scaled % 100),
        out_scaled / 10000, abs(out_scaled % 10000));


    return 0;
}