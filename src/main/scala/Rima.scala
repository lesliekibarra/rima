package chipyard.rima

import org.chipsalliance.cde.config.{Parameters, Config, Field}
import chisel3._
import chisel3.util._
import chisel3.experimental.IntParam

import freechips.rocketchip.tile._
import freechips.rocketchip.diplomacy._

import chipyard.rima._

import freechips.rocketchip.rocket.{
  MStatus, HellaCacheIO, TLBPTWIO, CanHavePTW, CanHavePTWModule,
  SimpleHellaCacheIF, M_XRD, PTE, PRV, M_SZ
}
import freechips.rocketchip.tilelink.{
  TLNode, TLIdentityNode, TLClientNode, TLMasterParameters, TLMasterPortParameters
}


class Rima(opcodes: OpcodeSet, val n: Int = 4)(implicit p: Parameters) extends LazyRoCC(opcodes) {
  override lazy val module = new RimaModule(this)
}

class RimaModule(outer: Rima)(implicit p: Parameters) extends LazyRoCCModuleImp(outer)
    with HasCoreParameters {

  // Scratchpads
  val scratchpadA = Module( new Scratchpad(4096, 8))
  val scratchpadB = Module( new Scratchpad(4096, 8))
  val scratchpadC = Module( new Scratchpad(4096, 32))

  // DMA Engines
  val dmaLoadA = Module(new DmaLoad(8))
  val dmaLoadB = Module(new DmaLoad(8))
  val dmaStoreC = Module(new DmaStore(32))

  // Hook up the core's memory interface
  dmaLoadA.io.mem <> io.mem
  dmaLoadB.io.mem <> io.mem
  dmaStoreC.io.mem <> io.mem

  // Compute Engine
  val compute = Module(new RimaComputeEngineSIMD(8))

  // RoCC instruction decode
  val cmd = Queue(io.cmd)
  val funct = cmd.bits.inst.funct

  // Define opcodes
  val isLoadA  = funct === 0.U
  val isLoadB  = funct === 1.U
  val isCompute = funct === 2.U
  val isStoreC = funct === 3.U
  val isSetDims = funct === 4.U

  // Parameter registers
  val M = Reg(UInt(16.W))
  val N = Reg(UInt(16.W))
  val K = Reg(UInt(16.W))

  when(cmd.fire && isSetDims) {
    M := cmd.bits.rs1(15,0)
    N := cmd.bits.rs1(31,16)
    K := cmd.bits.rs2(15,0)
  }

  // FSM
  val sIdle :: sLoadA :: sLoadB :: sCompute :: sStoreC :: Nil = Enum(5)
  val state = RegInit(sIdle)

  // Only accept new commands in idle state
  cmd.ready := (state === sIdle)

  switch(state) {
    is (sIdle) {
      when (cmd.fire && isLoadA) {
        state := sLoadA
      } .elsewhen (cmd.fire && isLoadB) {
        state := sLoadB
      } .elsewhen (cmd.fire && isCompute) {
        state := sCompute
      } .elsewhen (cmd.fire && isStoreC) {
        state := sStoreC
      }
    }
    is (sLoadA) {
      when (dmaLoadA.io.done) {
        state := sIdle
      }
    }
    is (sLoadB) {
      when (dmaLoadB.io.done) {
        state := sIdle
      }
    }
    is (sCompute) {
      when (compute.io.done) {
        state := sIdle
      }
    }
    is (sStoreC) {
      when (dmaStoreC.io.done) {
        state := sIdle
      }
    }
  }


  // DMA wiring
  dmaLoadA.io.start := (state === sLoadA)
  dmaLoadA.io.baseAddr := cmd.bits.rs1
  scratchpadA.io.write <> dmaLoadA.io.spWrite

  dmaLoadB.io.start := (state === sLoadB)
  dmaLoadB.io.baseAddr := cmd.bits.rs1
  scratchpadB.io.write <> dmaLoadB.io.spWrite

  dmaStoreC.io.start := (state === sStoreC)
  dmaStoreC.io.baseAddr := cmd.bits.rs1

  // connect read address channel
  scratchpadC.io.read <> dmaStoreC.io.spRead

  // connect data channel
  dmaStoreC.io.spRdata := scratchpadC.io.rdata

  // Compute Engine wiring
  compute.io.start := (state === sCompute)
  compute.io.M := M
  compute.io.N := N
  compute.io.K := K

  compute.io.spA <> scratchpadA.io.read
  compute.io.spB <> scratchpadB.io.read
  compute.io.spC <> scratchpadC.io.write

  compute.io.spAData := scratchpadA.io.rdata
  compute.io.spBData := scratchpadB.io.rdata

  // RoCC Response
  io.resp.valid := (state === sIdle && cmd.valid && isCompute)
  io.resp.bits.rd := cmd.bits.inst.rd
  io.resp.bits.data := 0.U // Placeholder

  // Busy signal
  io.busy := (state =/= sIdle)

  io.interrupt := false.B
}

class WithRima(op: OpcodeSet = OpcodeSet.custom0) extends Config((site, here, up) => {
  case BuildRoCC => up(BuildRoCC) ++ Seq((p: Parameters) => {
    val rima = LazyModule(new Rima(op, n = 4)(p))
    rima
  })
})