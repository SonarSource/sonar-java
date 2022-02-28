package checks.tests;

import org.junit.Test;

import static java.lang.Thread.sleep;

public class ThreadSleepInTestsCheck {

  @Test
  public void test() throws InterruptedException {
    foo();
    Thread.sleep(1000); // Noncompliant [[sc=12;ec=17]] {{Remove this use of "Thread.sleep()".}}
    bar();
  }

  @Test
  public void test2() throws InterruptedException {
    foo();
    sleep(1000); // Noncompliant [[sc=5;ec=10]] {{Remove this use of "Thread.sleep()".}}
    bar();
  }

  @Test
  public void test3() throws InterruptedException {
    foo();
    A.sleep(1000); // Compliant
    bar();
  }

  void foo() {}
  void bar() {}

  static class A {
    static void sleep(long l) { }
  }
}
