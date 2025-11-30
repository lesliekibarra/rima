package chipyard

import chisel3._
import chisel3.util.{log2Up}

import org.chipsalliance.cde.config.{Parameters, Config, Field}
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.tilelink.{BootROMParams}
import freechips.rocketchip.prci.{SynchronousCrossing, AsynchronousCrossing, RationalCrossing}
import freechips.rocketchip.rocket._
import freechips.rocketchip.tile._
import boom.v3.common._
import boom.v3.ifu._
import boom.v3.exu._
import boom.v3.lsu._


class WithNCortexA72LikeBooms(n: Int = 1) extends Config(
  new WithTAGELBPD ++ // Use TAGE-L branch predictor (closest match to A72’s multi-level predictor)
  new Config((site, here, up) => {
    case TilesLocated(InSubsystem) => {
      val prev = up(TilesLocated(InSubsystem), site)
      val idOffset = up(NumTiles)

        (0 until n).map { i =>
          val coreWidth = 4 // A72 decodes 3 instructions per cycle
          val memWidth = 2  // A72 has dual AGUs, dual memory issue pipelines

          BoomTileAttachParams(
            tileParams = BoomTileParams(
              core = BoomCoreParams(
                fetchWidth = 4, // A72 has a 4-wide instruction fetch stage
                decodeWidth = coreWidth,  // A72 has a 3-wide decode stage
                numFetchBufferEntries = coreWidth * 8, // A72 has a larger fetch buffer to hold more instructions
                enablePrefetching = true, // A72 employs prefetching mechanisms
                ftq = FtqParameters(nEntries=32), // A72 PC tracking and branch prediction structures
                
                // Branch Prediction
                enableBranchPrediction = true,
                maxBrCount = 32, // A72 max speculative branches
                numRasEntries = 32, // A72 return address stack size
                enableBranchPrintf = true,

                // Reorder Buffer (ROB)
                numRobEntries = 128, // A72 reorder buffer size
                
                numDCacheBanks = memWidth, // Dual memory pipelines
                issueParams = Seq(
                  // Memory Issue Queue
                  IssueParams(
                    issueWidth = memWidth,  // 2 memory issue per cycle, dual AGUs
                    numEntries = 8, // A72 memory issue queue size
                    iqType = IQT_MEM.litValue,
                    dispatchWidth = coreWidth
                  ), 
                  // Integer Issue Queue
                  IssueParams(
                    issueWidth = coreWidth, // 3 integer issue per cycle
                    numEntries = 32, // A72 integer issue queue size
                    iqType = IQT_INT.litValue,
                    dispatchWidth = coreWidth
                  ),
                  // Floating Point / SIMD Issue Queue
                  IssueParams(
                    issueWidth = 1, // Approximates NEON dual FP pipes
                    numEntries = 8, // A72 floating point issue queue size
                    iqType = IQT_FP.litValue,
                    dispatchWidth = coreWidth
                  )
                ),
                // Registers / Renaming
                numIntPhysRegisters = 128,
                numFpPhysRegisters = 128,

                // Load/store Queues
                numLdqEntries = 48,
                numStqEntries = 48,

                // FPU / NEON approximation (NEON ≈ dual 128b FP/INT pipes)
                fpu = Some(
                  freechips.rocketchip.tile.FPUParams(
                    sfmaLatency=4, 
                    dfmaLatency=4,
                    divSqrt=true
                  )
                ),
              ),
              // L1 Data Cache (A72 = 32 KB, 4-way, write-back)
              dcache = Some(DCacheParams(
                rowBits = 64, 
                nSets = 64,  // 64 sets x 8 ways x 64B = 32KB
                nWays = 8,
                blockBytes= 64,
                nMSHRs = 4, // Non-blocking cache, high MLP like A72
                nTLBWays = 8
              )),
              // L1 Instruction Cache (A72 = 48 KB, 3-way)
              icache = Some(ICacheParams(
                rowBits = 64,
                nSets = 64, //
                nWays = 8,  // A72 is 3-way, but boom only supports powers of 2
                blockBytes = 64,
                fetchBytes = 8, // 4 instructions per fetch (A72 has a 4-wide fetch stage) x 8B per instruction = 32B fetch
                nTLBWays = 8
              )),
              tileId = idOffset + i
            ),
            crossingParams = RocketCrossingParams()
          )
        } ++ prev
    }
    case NumTiles => up(NumTiles) + n
  })
)

class WithQuadCortexA72LikeBooms extends Config(
  new chipyard.config.WithSystemBusWidth(512) ++
  new WithNCortexA72LikeBooms(4) ++
  new WithSynchronousBoomTiles ++
  new chipyard.config.AbstractConfig
)

class WithQuadCortexA72LikeBoomsAndGemmini extends Config(
  // Gemmini attached to a Rocket tile
  new gemmini.DefaultGemminiConfig ++
  new freechips.rocketchip.rocket.WithNHugeCores(1) ++
  new WithQuadCortexA72LikeBooms

  //FIXME: build error due to using Boom with Gemmini
)

