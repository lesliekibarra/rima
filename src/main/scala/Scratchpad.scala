package chipyard.rima

import chisel3._
import chisel3.util._

class Scratchpad(depth: Int, width: Int) extends Module {
    val io = IO(new Bundle {
    val write = Flipped(Decoupled(new Bundle {
        val addr = UInt(log2Ceil(depth).W)
        val data = UInt(width.W)
    }))
    val read = Flipped(Decoupled(new Bundle {
        val addr = UInt(log2Ceil(depth).W)
    }))
    val rdata = Output(UInt(width.W))
    })

    val mem = SyncReadMem(depth, UInt(width.W))

    // Write
    when(io.write.valid) {
        mem.write(io.write.bits.addr, io.write.bits.data)
    }
    io.write.ready := true.B

    // Read
    val rreg = RegInit(0.U(width.W))
    when(io.read.valid) {
        rreg := mem.read(io.read.bits.addr, true.B)
    }
    io.read.ready := true.B
    io.rdata := rreg
}