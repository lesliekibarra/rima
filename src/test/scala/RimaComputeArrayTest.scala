package rima

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ComputeArrayTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "ComputeArray"

  it should "perform 2x2 matrix multiplication correctly (with waveforms)" in {
    test(new ComputeArray(dim = 2, lanes = 2, dataWidth = 8, accWidth = 32))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      // Define matrices A (2x2) and B (2x2)
      val A = Seq(Seq(1, 2), Seq(3, 4))
      val B = Seq(Seq(5, 6), Seq(7, 8))

      val C_expected = Seq(
        Seq(1*5 + 2*7, 1*6 + 2*8),
        Seq(3*5 + 4*7, 3*6 + 4*8)
      )

      // Clear accumulators
      dut.io.clear.poke(true.B)
      dut.clock.step(1)
      dut.io.clear.poke(false.B)
      dut.io.accumulate.poke(false.B)
      dut.io.load.poke(true.B)

      // Feed input data for 2 systolic steps
      for (cycle <- 0 until 2) {
        for (i <- 0 until 2) {
          for (lane <- 0 until 2) {
            val a_val = A(i)(lane)
            val b_val = B(lane)(i)
            dut.io.a_in(i)(lane).poke(a_val.S)
            dut.io.b_in(i)(lane).poke(b_val.S)
          }
        }
        dut.clock.step(1)
        dut.io.load.poke(false.B)
      }

      // Let systolic data propagate
      dut.clock.step(2)

      // Check results
      for (i <- 0 until 2) {
        for (j <- 0 until 2) {
          val got = dut.io.results(i)(j).peek().litValue
          val exp = C_expected(i)(j)
          println(s"Result($i,$j) = $got (expected $exp)")
          assert(got == exp, s"Mismatch at C($i,$j): got $got, expected $exp")
        }
      }
    }
  }
}
