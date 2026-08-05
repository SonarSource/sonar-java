package checks;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

class ScheduledThreadPoolExecutorMaximumPoolSizeCheckSample {

  void directCall() {
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
    executor.setMaximumPoolSize(10); // Noncompliant {{Remove this "setMaximumPoolSize" call; it has no effect on ScheduledThreadPoolExecutor.}}
//           ^^^^^^^^^^^^^^^^^^
  }

  void afterOtherConfig() {
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
    executor.setCorePoolSize(8);
    executor.setMaximumPoolSize(10); // Noncompliant
//           ^^^^^^^^^^^^^^^^^^
  }

  void variableArgument() {
    int maxSize = 20;
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
    executor.setMaximumPoolSize(maxSize); // Noncompliant
//           ^^^^^^^^^^^^^^^^^^
  }

  ScheduledThreadPoolExecutor createPool() {
    return new ScheduledThreadPoolExecutor(5);
  }

  void methodReturnedInstance() {
    createPool().setMaximumPoolSize(10); // Noncompliant
//               ^^^^^^^^^^^^^^^^^^
  }

  void onSubtype() {
    CustomScheduledExecutor custom = new CustomScheduledExecutor(5);
    custom.setMaximumPoolSize(10); // Noncompliant
//         ^^^^^^^^^^^^^^^^^^
  }

  // Compliant cases

  void setCorePoolSizeOnScheduled() {
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
    executor.setCorePoolSize(10); // Compliant - correct way to control pool size
  }

  void setMaximumPoolSizeOnThreadPoolExecutor() {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 10, 60L,
      java.util.concurrent.TimeUnit.SECONDS,
      new java.util.concurrent.LinkedBlockingQueue<>());
    executor.setMaximumPoolSize(20); // Compliant - has effect on ThreadPoolExecutor
  }

  void otherConfigOnScheduled() {
    ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
    executor.setKeepAliveTime(60L, java.util.concurrent.TimeUnit.SECONDS); // Compliant
    executor.allowCoreThreadTimeOut(true); // Compliant
  }

  static class CustomScheduledExecutor extends ScheduledThreadPoolExecutor {
    CustomScheduledExecutor(int corePoolSize) {
      super(corePoolSize);
    }
  }
}
