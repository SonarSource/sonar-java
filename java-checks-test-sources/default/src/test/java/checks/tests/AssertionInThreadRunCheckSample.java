package checks.tests;

import org.easymock.EasyMockSupport;

import static org.easymock.EasyMock.expectLastCall;

class AssertionInThreadRunCheckSample extends Thread {
  @Override
  public void run() {
    org.junit.Assert.assertTrue(true); // Noncompliant [[sc=22;ec=32]] {{Remove this assertion.}}
    org.junit.jupiter.api.Assertions.assertTrue(true); // Noncompliant [[sc=38;ec=48]] {{Remove this assertion.}}
    org.junit.Assert.assertEquals(true, false); // Noncompliant {{Remove this assertion.}}
    org.junit.jupiter.api.Assertions.assertEquals("a", "b"); // Noncompliant {{Remove this assertion.}}
    junit.framework.Assert.assertEquals(true, true); // Noncompliant {{Remove this assertion.}}
    junit.framework.Assert.fail("message"); // Noncompliant {{Remove this assertion.}}
    org.fest.assertions.Assertions.assertThat(true).isTrue();// Noncompliant {{Remove this assertion.}}
    org.assertj.core.api.Assertions.assertThat(true).isTrue();// Noncompliant {{Remove this assertion.}}
  }
  public void foo() {}
  public void run(int i){}
}

abstract class AssertionInThreadRunCheckSample_B {
  public void run() {
    org.junit.Assert.assertTrue(true);
    org.junit.jupiter.api.Assertions.assertTrue(true);
  }
  abstract void foo();
}

class AssertionInThreadRunCheckSample_C extends junit.framework.TestCase {
  class A extends Thread {
    @Override
    public void run() {
      assertTrue(true); // Noncompliant
    }
  }
}

class AssertionInThreadRunCheckSample_D extends Thread {

  @Override
  public void run() { // Compliant
    foo();
  }

  void foo() { }
}


class AssertionInThreadRunCheckSample_ThreadStopperTest extends EasyMockSupport {

  public void failsToScan() {

    new Thread("testFailsToScan") {
      @Override
      public void run() {
        expectLastCall().once();
      }
    };

  }
}
