#include <math.h>

#include "tensorflow/lite/core/c/common.h"
#include "tensorflow/lite/micro/micro_mutable_op_resolver.h"
#include "tensorflow/lite/micro/micro_interpreter.h"
#include "tensorflow/lite/micro/tflite_bridge/micro_error_reporter.h"
#include "tensorflow/lite/schema/schema_generated.h"

// #include "hello_world_float_model_data.h"
#include "hello_world_int8_model_data.h"

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
    float test_input = 0.5f;
    float prediction = RunSingleInference(test_input);
    printf("Predicted output for %.2f is %.4f\n", test_input, prediction);
    return 0;
}