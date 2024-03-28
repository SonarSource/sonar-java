package checks.tests;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class ThreadSleepInTestsCheckSample {

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

  @Test
  public void test4() throws InterruptedException {
    foo();
    java.util.concurrent.TimeUnit.SECONDS.sleep(1); // Noncompliant [[sc=43;ec=48]] {{Remove this use of "TimeUnit.sleep()".}}
    bar();
  }

  @Test
  public void test5() throws InterruptedException {
    foo();
    TimeUnit.HOURS.sleep(1); // Noncompliant [[sc=20;ec=25]] {{Remove this use of "TimeUnit.sleep()".}}
    bar();
  }



  void foo() {}
  void bar() {}

  static class A {
    static void sleep(long l) { }
  }
}
