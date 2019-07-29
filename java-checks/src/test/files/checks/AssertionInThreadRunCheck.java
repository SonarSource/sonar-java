import java.lang.Override;
import java.lang.Thread;
import static org.easymock.EasyMock.expectLastCall;

import org.easymock.EasyMockSupport;
class A extends Thread {
  @Override
  public void run() {
    org.junit.Assert.assertTrue(true); // Noncompliant [[sc=22;ec=32]] {{Remove this assertion.}}
    org.junit.jupiter.api.Assertions.assertTrue(true); // Noncompliant [[sc=38;ec=48]] {{Remove this assertion.}}
    org.junit.Assert.assertEquals(true, false); // Noncompliant {{Remove this assertion.}}
    org.junit.jupiter.api.Assertions.assertEquals("a", "b"); // Noncompliant {{Remove this assertion.}}
    junit.framework.Assert.assertEquals(true, true); // Noncompliant {{Remove this assertion.}}
    junit.framework.Assert.fail("message"); // Noncompliant {{Remove this assertion.}}
    org.fest.assertions.Assertions.assertThat(true).isTrue();// Noncompliant {{Remove this assertion.}}
  }
  public void foo() {}
  public void run(int i){}
}

abstract class B {
  public void run() {
    org.junit.Assert.assertTrue(true);
    org.junit.jupiter.api.Assertions.assertTrue(true);
  }
  abstract void foo();
}

class C extends junit.framework.TestCase {
  class A extends Thread {
    @Override
    public void run() {
      assertTrue(true); // Noncompliant
    }
  }
}

class D extends Thread {
  
  @Override
  public void run() { // Compliant
    foo();
  }
  
}


public class ThreadStopperTest extends EasyMockSupport {

  public void failsToScan() {

    new Thread("testFailsToScan") {
      @Override
      public void run() {
        expectLastCall().once();
      }
    };

  }
}
