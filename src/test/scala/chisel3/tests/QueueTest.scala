package chisel3.tests

import org.scalatest._

import chisel3._
import chisel3.tester._

class QueueTest extends FlatSpec with ChiselScalatestTester {
  behavior of "Testers2 with Queue"

  it should "pass through elements, using enqueueNow" in {
    test(new QueueModule(UInt(8.W), 2)) { c =>
      c.in.initSource().setSourceClock(c.clock)
      c.out.initSink().setSinkClock(c.clock)

      c.out.expectInvalid()
      c.in.enqueueNow(42.U)
      parallel(
          c.out.expectDequeueNow(42.U),
          c.in.enqueueNow(43.U)
      )
      c.out.expectDequeueNow(43.U)
    }
  }

  it should "pass through elements, using enqueueSeq" in {
    test(new QueueModule(UInt(8.W), 2)) { c =>
      c.in.initSource().setSourceClock(c.clock)
      c.out.initSink().setSinkClock(c.clock)

      fork {
        c.in.enqueueSeq(Seq(42.U, 43.U, 44.U))
      }

      c.out.expectInvalid()
      c.clock.step(1)  // wait for first element to enqueue
      c.out.expectDequeueNow(42.U)
      c.out.expectPeek(43.U)  // check that queue stalls
      c.clock.step(1)
      c.out.expectDequeueNow(43.U)
      c.out.expectDequeueNow(44.U)
      c.out.expectInvalid()
    }
  }

  it should "work with a combinational queue" in {
    test(new PassthroughQueue(UInt(8.W))) { c =>
      c.in.initSource()
      c.in.setSourceClock(c.clock)
      c.out.initSink()
      c.out.setSinkClock(c.clock)

      fork {
        c.in.enqueueSeq(Seq(42.U, 43.U, 44.U))
      }.fork {
        c.out.expectDequeueSeq(Seq(42.U, 43.U, 44.U))
      }.join()
    }
  }
}
