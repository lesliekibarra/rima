package rima

import chisel3._
import org.chipsalliance.cde.config.{Config, Parameters}

class BaselineConfig extends Config(
  new boom.v3.common.WithNLargeBooms(2) ++
  new chipyard.config.WithSystemBusWidth(128) ++
  
  // Shared L2 cache
  new freechips.rocketchip.subsystem.WithNBanks(2) ++
  new freechips.rocketchip.subsystem.WithInclusiveCache(nWays = 8, capacityKB = 512) ++

  new chipyard.config.AbstractConfig
)

class BaselineGemminiConfig extends Config(
  new gemmini.DefaultGemminiConfig ++ // Add accelerator
  new BaselineConfig // Reuse everything else
)