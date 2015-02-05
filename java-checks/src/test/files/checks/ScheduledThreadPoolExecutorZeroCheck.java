import java.util.concurrent.ScheduledThreadPoolExecutor;

class A {
  public void foo() {
    ScheduledThreadPoolExecutor stpe1 = new ScheduledThreadPoolExecutor(0); // Noncompliant
    ScheduledThreadPoolExecutor stpe2 = new ScheduledThreadPoolExecutor(POOL_SIZE);
    stpe2.setCorePoolSize(0);  // Noncompliant
    stpe2.setCorePoolSize(12);  // Compliant

  }
}