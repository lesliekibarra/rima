package chipyard.rima

import chisel3._
import chisel3.util._
import java.rmi.server.UID

class RimaComputeEngineSIMD(vecSize: Int) extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val done = Output(Bool())

    val M = Input(UInt(16.W))
    val N = Input(UInt(16.W))
    val K = Input(UInt(16.W))

    val spA = Decoupled(new Bundle { val addr = UInt(12.W) })
    val spB = Decoupled(new Bundle { val addr = UInt(12.W) })
    val spC = Decoupled(new Bundle { 
      val addr = UInt(12.W)
      val data = UInt(32.W)
    })

    val spAData = Input(UInt(8.W))
    val spBData = Input(UInt(8.W))
  })
  
  // States
  val sIdle :: sReqA :: sReqB :: sMAC :: sWrite :: sDone :: Nil = Enum(6)
  val state = RegInit(sIdle)

  // Loop counters
  val m = Reg(UInt(16.W))
  val n = Reg(UInt(16.W))
  val k = Reg(UInt(16.W))

  val sum = RegInit(0.S(32.W))

  // Defaults
  io.spA.valid      := false.B
  io.spA.bits.addr  := 0.U
  io.spB.valid      := false.B
  io.spB.bits.addr  := 0.U
  io.spC.valid      := false.B
  io.spC.bits.addr  := 0.U
  io.spC.bits.data  := 0.U

  io.done := (state === sDone)

  // Helpers to avoid huge widths (clip to 12 bits)
  def addrA(m: UInt, k: UInt) = (m * io.K + k)(11,0)
  def addrB(k: UInt, n: UInt) = (k * io.N + n)(11,0)
  def addrC(m: UInt, n: UInt) = (m * io.N + n)(11,0)

  switch(state) {
    is(sIdle) {
      when(io.start) {
        m   := 0.U
        n   := 0.U
        k   := 0.U
        sum := 0.S
        state := sReqA
      }
    }

    // Request A[m,k]
    is(sReqA) {
      io.spA.valid     := true.B
      io.spA.bits.addr := addrA(m, k)
      when(io.spA.ready) {
        state := sReqB
      }
    }

    // Request B[k,n]
    is(sReqB) {
      io.spB.valid     := true.B
      io.spB.bits.addr := addrB(k, n)
      when(io.spB.ready) {
        // Assuming 1-cycle read latency in scratchpads:
        // rdata will be valid next cycle
        state := sMAC
      }
    }

    // Perform one MAC: sum += A[m,k] * B[k,n]
    is(sMAC) {
      val a = io.spAData.asSInt  // int8
      val b = io.spBData.asSInt  // int8
      sum := sum + (a * b)

      when(k === io.K - 1.U) {
        // Finished inner loop, move to write C[m,n]
        state := sWrite
      } .otherwise {
        // Next k
        k := k + 1.U
        state := sReqA
      }
    }

    // Write C[m,n] = sum
    is(sWrite) {
      io.spC.valid       := true.B
      io.spC.bits.addr   := addrC(m, n)
      io.spC.bits.data   := sum.asUInt

      when(io.spC.ready) {
        // Reset accumulator for next output
        sum := 0.S
        k   := 0.U

        when(n === io.N - 1.U) {
          when(m === io.M - 1.U) {
            // Done all elements
            state := sDone
          } .otherwise {
            // Next row: (m+1, n=0)
            m := m + 1.U
            n := 0.U
            state := sReqA
          }
        } .otherwise {
          // Next column in same row: (m, n+1)
          n := n + 1.U
          state := sReqA
        }
      }
    }

    // Signal done until host deasserts start
    is(sDone) {
      when(!io.start) {
        state := sIdle
      }
    }
  }
}