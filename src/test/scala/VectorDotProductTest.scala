package rima

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class VectorDotProductTest extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "VectorDotProduct"

  it should "compute a single dot product correctly when load is high" in {
    test(new VectorDotProduct(lanes = 4, dataWidth = 8, accumulatorWidth = 32))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        val a = Seq(1, 2, 3, 4)
        val b = Seq(5, 6, 7, 8)
        val expected = (a zip b).map { case (x, y) => x * y }.sum

        for (i <- a.indices) {
          dut.io.a(i).poke(a(i).S)
          dut.io.b(i).poke(b(i).S)
        }

        dut.io.load.poke(true.B)
        dut.io.accumulate.poke(false.B)
        dut.io.clear.poke(false.B)
        dut.clock.step(1)

        dut.io.result.expect(expected.S)
      }
  }

  it should "accumulate over multiple cycles" in {
  test(new VectorDotProduct(lanes = 2, dataWidth = 8, accumulatorWidth = 32))
    .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // --- Cycle 0: Clear the accumulator ---
      dut.io.clear.poke(true.B)
      dut.clock.step(1)
      dut.io.clear.poke(false.B)

      // --- Cycle 1: First dot product ---
      val a1 = Seq(1, 2)
      val b1 = Seq(3, 4)
      val dot1 = (a1 zip b1).map { case (x, y) => x * y }.sum // = 11

      for (i <- a1.indices) {
        dut.io.a(i).poke(a1(i).S)
        dut.io.b(i).poke(b1(i).S)
      }

      dut.io.load.poke(true.B)
      dut.io.accumulate.poke(false.B)
      dut.clock.step(1)
      dut.io.load.poke(false.B)
      dut.io.result.expect(dot1.S)

      // --- Cycle 2: Second dot product ---
      val a2 = Seq(2, 3)
      val b2 = Seq(4, 5)
      val dot2 = (a2 zip b2).map { case (x, y) => x * y }.sum // = 23

      for (i <- a2.indices) {
        dut.io.a(i).poke(a2(i).S)
        dut.io.b(i).poke(b2(i).S)
      }

      dut.io.accumulate.poke(true.B)
      dut.clock.step(1)
      dut.io.accumulate.poke(false.B)

      val expectedAcc = dot1 + dot2 // = 34
      println(s"Cycle 2 result = ${dut.io.result.peek().litValue}")
      dut.io.result.expect(expectedAcc.S)

      // --- Cycle 3: Verify it holds steady ---
      dut.clock.step(1)
      dut.io.result.expect(expectedAcc.S)
    }
}

  it should "clear accumulator when clear signal is asserted" in {
    test(new VectorDotProduct(lanes = 2, dataWidth = 8, accumulatorWidth = 32))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        // preload
        dut.io.a(0).poke(1.S)
        dut.io.b(0).poke(2.S)
        dut.io.a(1).poke(3.S)
        dut.io.b(1).poke(4.S)
        dut.io.load.poke(true.B)
        dut.io.accumulate.poke(false.B)
        dut.io.clear.poke(false.B)
        dut.clock.step(1)

        // clear
        dut.io.clear.poke(true.B)
        dut.clock.step(1)
        dut.io.clear.poke(false.B)

        dut.io.result.expect(0.S)
      }
  }

  it should "produce a visual waveform for debugging" in {
    test(new VectorDotProduct(lanes = 4, dataWidth = 8, accumulatorWidth = 32))
      .withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
        dut.io.clear.poke(false.B)
        dut.io.load.poke(true.B)
        dut.io.accumulate.poke(false.B)

        for (i <- 0 until 4) {
          dut.io.a(i).poke((i + 1).S)
          dut.io.b(i).poke((i + 2).S)
        }

        dut.clock.step(1)
        println(s"Cycle 1 result = ${dut.io.result.peek().litValue}")
        dut.clock.step(10)
      }
  }
}
