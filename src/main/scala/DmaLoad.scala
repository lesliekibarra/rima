package chipyard.rima

import chisel3._
import chisel3.util._
import freechips.rocketchip.rocket._
import org.chipsalliance.cde.config.{Parameters, Config, Field}

class DmaLoad(width: Int)(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val baseAddr = Input(UInt(64.W))
    val spWrite = Decoupled(new Bundle {
      val addr = UInt(12.W)
      val data = UInt(width.W)
    })
    val done = Output(Bool())
    val mem  = new HellaCacheIO
  })

  val sIdle :: sReq :: sResp :: sDone :: Nil = Enum(4)
  val state = RegInit(sIdle)

  val addr = Reg(UInt(64.W))
  val count = RegInit(0.U(12.W))

  // -------- DEFAULTS --------

  // scratchpad write channel
  io.spWrite.valid      := false.B
  io.spWrite.bits.addr  := 0.U
  io.spWrite.bits.data  := 0.U 

  // memory interface
  io.mem.req.valid := false.B
  io.mem.req.bits := DontCare

  io.mem.s1_kill := false.B
  io.mem.s2_kill := false.B

  io.mem.s1_data.data := 0.U
  io.mem.s1_data.mask := 0.U

  io.mem.keep_clock_enabled := true.B
  
  io.done := (state === sDone)

  // -------- FSM --------
  switch(state) {
    is(sIdle) {
      when(io.start) {
        addr := io.baseAddr
        count := 0.U
        state := sReq
      }
    }

    is(sReq) {
      io.mem.req.valid := true.B
      io.mem.req.bits.addr := addr
      io.mem.req.bits.cmd := M_XRD
      io.mem.req.bits.tag := count
      io.mem.req.bits.size := log2Ceil(8).U

      when(io.mem.req.ready) {
        state := sResp
      }
    }

    is(sResp) {
      when(io.mem.resp.valid) {
        io.spWrite.valid := true.B
        io.spWrite.bits.addr := count
        io.spWrite.bits.data := io.mem.resp.bits.data(width-1,0)
        when(io.spWrite.ready) {
          addr := addr + 1.U
          count := count + 1.U
          when(count === 255.U) { // example fixed size
            state := sDone
          }.otherwise {
            state := sReq
          }
        }
      }
    }

    is(sDone) {
      state := sIdle
    }
  }
}