package chipyard

import chisel3._
import org.chipsalliance.cde.config.{Config}
import freechips.rocketchip.subsystem._

class CortexA72 extends Config(
  new boom.v3.common.WithNMegaBooms(4) ++
  new boom.v3.common.WithBoomCommitLogPrintf ++

  new WithCoherentBusTopology ++
  
  // Shared L2 cache
  new freechips.rocketchip.subsystem.WithNBanks(2) ++
  new freechips.rocketchip.subsystem.WithInclusiveCache(nWays = 8, capacityKB = 512) ++

  new chipyard.config.AbstractConfig
)

class BaselineGemminiConfig extends Config(
  new gemmini.DefaultGemminiConfig ++ // Add accelerator
  new CortexA72 // Reuse everything else
)