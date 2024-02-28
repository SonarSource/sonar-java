package checks;

import java.util.concurrent.ScheduledThreadPoolExecutor;

class ScheduledThreadPoolExecutorZeroCheckSample {
  private static final int POOL_SIZE = 42;
  public void foo() {
    ScheduledThreadPoolExecutor stpe1 = new ScheduledThreadPoolExecutor(0); // Noncompliant [[sc=73;ec=74]] {{Increase the "corePoolSize".}}
    ScheduledThreadPoolExecutor stpe2 = new ScheduledThreadPoolExecutor(POOL_SIZE);
    stpe2.setCorePoolSize(0);  // Noncompliant [[sc=27;ec=28]] {{Increase the "corePoolSize".}}
    stpe2.setCorePoolSize(12);  // Compliant
  }
}
