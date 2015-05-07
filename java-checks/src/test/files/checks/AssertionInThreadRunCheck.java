import java.lang.Override;
import java.lang.Thread;

class A extends Thread {
  @Override
  public void run() {
    org.junit.Assert.assertTrue(true); // Noncompliant {{Remove this assertion.}}
    org.junit.Assert.assertEquals(true, false); // Noncompliant {{Remove this assertion.}}
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
  }
  abstract foo();
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