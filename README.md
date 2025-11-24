# rima
RISC-V INT8 Matrix Accelerator 

RIMA is a RISC-V INT8 matrix accelerator integrated into the Chipyard framework.  
This guide explains how to integrate, build, and simulate RIMA configurations within Chipyard.
---

## Activate the Chipyard Build Environment

From the top-level Chipyard directory, activate the environment:

```
source env.sh
```
(You should see your terminal prompt change to indicate the environment is active.)

## Add the RIMA Submodule to Chipyard

From the Chipyard root directory, add RIMA as a Git submodule:

```
cd generators/
git submodule add https://git-repository.com/yourproject.git
```

## Integration with Chipyard

**1. Add the RIMA Project to Chipyard’s SBT Build**

Edit Chipyard’s top-level `build.sbt` and append:

```
// -- RIMA --
lazy val rima = (project in file("generators/rima"))
  .dependsOn(boom, rocketchip, rocketchip_inclusive_cache, gemmini)
```

(This registers RIMA as an SBT subproject so Chipyard can compile its hardware sources.)


**2. Move the top-level configuration file into Chipyard’s config directory**

Chipyard only detects configurations used with `make CONFIG=...` if they’re compiled as part of 
the Chipyard project, so the top-level config file (`RimaConfigs.scala`) needs to be placed
in Chipyard's `config/` directory.

Move the file from RIMA into Chipyard:
```
mv generators/rima/src/main/scala/RimaConfigs.scala generators/chipyard/src/main/scala/config/
```

## Building a Configuration (Verilator)

To build a simulation using a custom configuration:

```
# Navigate to the Verilator simulation directory
cd sims/verilator

# Build with your desired configuration
make CONFIG=<ConfigName>
```

Chipyard will generate Verilog and a Verilator simulation using your RIMA-enabled configuration. 
This step may take several minutes.

## Adding RIMA Tests

To include RIMA test programs in the Chipyard test suite, append to `chipyard/tests/CMakeLists.txt`:

```
#################################
# RIMA Tests
#################################
add_subdirectory(
    ../generators/rima/tests
    ${CMAKE_CURRENT_BINARY_DIR}/tests
)
```

This allows CMake to include and build the RIMA tests alongside other Chipyard tests.

## Building Tests

To compile the RIMA test binaries:

```
cd generators/rima/tests
mkdir build && cd build
cmake ..
make
```

After building, you will find the following outputs:
.riscv — Compiled RISC-V binaries
.dump — Disassembly of the compiled programs

## Running Simulations

Once the simulation and tests are built, run a test using:

```
cd sims/verilator
./simulator-<ConfigName> <path_to_test>/<test_name>.riscv
```

Simulation logs and output files will be generated in the current directory.

## Notes

Ensure all Chipyard submodules are initialized:

`git submodule update --init --recursive`

