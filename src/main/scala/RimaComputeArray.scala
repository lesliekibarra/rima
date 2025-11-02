package rima

import chisel3._
import chisel3.util._

/**
  * ComputeUnit
  *
  * A systolic processing element that encapsulates a VectorDotProduct.
  * Passes data to its right and bottom neighbors, forming a systolic array.
  */
class ComputeUnit(
    val lanes: Int = 16,
    val dataWidth: Int = 16,
    val accWidth: Int = 48
) extends Module {
  val io = IO(new Bundle {
    val a_in = Input(Vec(lanes, SInt(dataWidth.W)))
    val b_in = Input(Vec(lanes, SInt(dataWidth.W)))
    val a_out = Output(Vec(lanes, SInt(dataWidth.W)))
    val b_out = Output(Vec(lanes, SInt(dataWidth.W)))

    val clear = Input(Bool())
    val accumulate = Input(Bool())
    val load = Input(Bool())

    val result = Output(SInt(accWidth.W))
  })

  // Instantiate the reusable VectorDotProduct
  val vdp = Module(new VectorDotProduct(lanes, dataWidth, accWidth))
  vdp.io.a := io.a_in
  vdp.io.b := io.b_in
  vdp.io.clear := io.clear
  vdp.io.accumulate := io.accumulate
  vdp.io.load := io.load

  io.result := vdp.io.result

  // Systolic pass-through
  io.a_out := io.a_in
  io.b_out := io.b_in
}

/**
  * ComputeArray
  *
  * NxN systolic array composed of ComputeUnits.
  * Each unit performs a vector dot product and passes its inputs
  * horizontally and vertically across the array.
  */
class ComputeArray(
    val dim: Int = 4,
    val lanes: Int = 16,
    val dataWidth: Int = 16,
    val accWidth: Int = 48
) extends Module {
  val io = IO(new Bundle {
    val a_in = Input(Vec(dim, Vec(lanes, SInt(dataWidth.W)))) // input rows
    val b_in = Input(Vec(dim, Vec(lanes, SInt(dataWidth.W)))) // input cols

    val clear = Input(Bool())
    val accumulate = Input(Bool())
    val load = Input(Bool())

    val results = Output(Vec(dim, Vec(dim, SInt(accWidth.W))))
  })

  val peArray = Seq.fill(dim, dim)(Module(new ComputeUnit(lanes, dataWidth, accWidth)))

  for (i <- 0 until dim) {
    for (j <- 0 until dim) {
      val cu = peArray(i)(j)

      // Connect A (rows)
      if (j == 0) cu.io.a_in := io.a_in(i)
      else cu.io.a_in := peArray(i)(j - 1).io.a_out

      // Connect B (columns)
      if (i == 0) cu.io.b_in := io.b_in(j)
      else cu.io.b_in := peArray(i - 1)(j).io.b_out

      cu.io.clear := io.clear
      cu.io.accumulate := io.accumulate
      cu.io.load := io.load

      io.results(i)(j) := cu.io.result
    }
  }
}
