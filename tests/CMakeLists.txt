
cmake_minimum_required(VERSION 3.10)

include(FetchContent)

FetchContent_Declare(
    tflite_micro
    GIT_REPOSITORY https://github.com/tensorflow/tflite-micro.git
    GIT_TAG main
)

FetchContent_MakeAvailable(tflite_micro)

set(
    TFLITE_MICRO_LIBS_DIR
    ${CMAKE_CURRENT_SOURCE_DIR}/lib
)

set(
    TFLITE_MICRO_MODELS_DIR
    ${CMAKE_CURRENT_SOURCE_DIR}/models
)

message(STATUS "TFLITE_MICRO_LIBS_DIR: ${TFLITE_MICRO_LIBS_DIR}")
message(STATUS "TFLITE_MICRO_MODELS_DIR: ${TFLITE_MICRO_MODELS_DIR}")

# Path to TensorFlow Lite Micro makefile
set(TFLM_TOOLS_MAKE_DIR ${tflite_micro_SOURCE_DIR}/tensorflow/lite/micro/tools/make)
set(TFLM_MAKEFILE  ${TFLM_TOOLS_MAKE_DIR}/Makefile)

message(STATUS "TFLM_MAKEFILE: ${TFLM_MAKEFILE}")

# Define a custom target to download dependencies
add_custom_target(
    tflm_downloads
    COMMAND ${CMAKE_MAKE_PROGRAM} -f ${TFLM_MAKEFILE} third_party_downloads
    WORKING_DIRECTORY ${tflite_micro_SOURCE_DIR}
    COMMENT "Running TensorFlow Lite Micro third_party_downloads..."
)

message(STATUS "Defined tflm_downloads target")

add_executable(hello_world_test
    hello_world_test.cpp
    ${TFLITE_MICRO_MODELS_DIR}/hello_world_int8_model_data.cc
)

message(STATUS "Added hello_world_test executable")

add_dependencies(hello_world_test tflm_downloads)

message(STATUS "Added dependency on tflm_downloads")

target_include_directories(
    hello_world_test PRIVATE
    ${tflite_micro_SOURCE_DIR}
    ${TFLM_TOOLS_MAKE_DIR}/downloads/flatbuffers/include  # Add FlatBuffers include directory
    ${TFLM_TOOLS_MAKE_DIR}/downloads/gemmlowp  # Add gemmlowp include directory
    ${TFLITE_MICRO_MODELS_DIR}  # Add models directory
    ${tflite_micro_SOURCE_DIR}
)

message(STATUS "Included model directory")
message(STATUS "tflite_micro_SOURCE_DIR: ${tflite_micro_SOURCE_DIR}")

target_link_libraries(
    hello_world_test PRIVATE 
    ${TFLITE_MICRO_LIBS_DIR}/libtensorflow-microlite.a  # Link the static library
)

message(STATUS "Linked TensorFlow Lite Micro library")
target_compile_definitions(hello_world_test PRIVATE TF_LITE_USE_GLOBAL_CMATH_FUNCTIONS)

add_dump_target(hello_world_test)