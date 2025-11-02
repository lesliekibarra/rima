
//======================================================================
// VectorDotProduct.scala
// Description : Chisel module for vector dot product with accumulation
// Author: Leslie Ibarra
//======================================================================

package rima

import chisel3._
import chisel3.util._
import scala.language.reflectiveCalls

class VectorDotProduct(
    val lanes: Int = 16,
    val dataWidth: Int = 16,
    val accumulatorWidth: Int = 48
) extends Module {

    // IO definition
    val io = IO(new Bundle {
        // Input vectors
        val a = Input(Vec(lanes, SInt(dataWidth.W)))
        val b = Input(Vec(lanes, SInt(dataWidth.W)))
        
        // Control signals
        val load = Input(Bool())
        val accumulate = Input(Bool())
        val clear = Input(Bool())

        // Output result
        val result = Output(SInt(accumulatorWidth.W))
    })

    val products = Wire(Vec(lanes, SInt((2 * dataWidth).W)))

    for (i <- 0 until lanes) {
        products(i) := io.a(i) * io.b(i)
    }

    val sum = products.reduce(_ +& _).asSInt

    val accumulator = RegInit(0.S(accumulatorWidth.W))

    when (io.clear) {
        accumulator := 0.S
    } .elsewhen (io.load) {
        accumulator := sum
    } .elsewhen (io.accumulate) {
        accumulator := accumulator + sum
    }
    io.result := accumulator
}