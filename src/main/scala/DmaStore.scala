package chipyard.rima

import chisel3._
import chisel3.util._
import freechips.rocketchip.rocket._
import org.chipsalliance.cde.config.{Parameters, Config, Field}

class DmaStore(width: Int)(implicit p: Parameters)  extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val baseAddr = Input(UInt(64.W))
    val spRead = Decoupled(new Bundle {
      val addr = UInt(12.W)
    })
    val mem = new HellaCacheIO
    val done = Output(Bool())
    val spRdata  = Input(UInt(width.W))
  })

  val sIdle :: sRead :: sReq :: sWait :: sDone :: Nil = Enum(5)
  val state = RegInit(sIdle)

  val addr = Reg(UInt(64.W))
  val count = Reg(UInt(12.W))
  val rdata = Reg(UInt(width.W))

// -------- DEFAULTS --------

  // scratchpad read channel
  io.spRead.valid := false.B
  io.spRead.bits.addr := 0.U

  // memory interface
  io.mem.req.valid := false.B
  io.mem.req.bits := 0.U.asTypeOf(io.mem.req.bits)

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
        state := sRead
      }
    }

    is(sRead) {
      io.spRead.valid := true.B
      io.spRead.bits.addr := count
      when(io.spRead.ready) {
        rdata := io.spRdata
        state := sReq
      }
    }

    is(sReq) {
      io.mem.req.valid := true.B
      io.mem.req.bits.addr := addr
      io.mem.req.bits.cmd := M_XWR
      io.mem.req.bits.data := rdata
      io.mem.req.bits.size := log2Ceil(width/8).U
      io.mem.req.bits.tag := count

      when(io.mem.req.ready) {
        state := sWait
      }
    }

    is(sWait) {
      when(io.mem.resp.valid) {
        addr := addr + ( width/8).U
        count := count + 1.U
        when(count === 255.U) {
          state := sDone
        }.otherwise {
          state := sRead
        }
      }
    }

    is(sDone) {
      state := sIdle
    }
  }
}