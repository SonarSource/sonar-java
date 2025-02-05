package checks.spring;

import org.springframework.scheduling.annotation.Scheduled;

public class ScheduledOnlyOnNoArgMethodCheckSample {
  @Scheduled(fixedDelay = 5000)
  public void noArg() {
  }

  @Scheduled(fixedDelay = 5000)
// ^^^^^^^^^ >
  public void oneArg(int i) { // Noncompliant {{"@Scheduled" annotation should only be applied to no-arg methods}}
//            ^^^^^^
  }

  @Scheduled(fixedDelay = 5000)
// ^^^^^^^^^ >
  public static void oneArgStatic(int i) { // Noncompliant {{"@Scheduled" annotation should only be applied to no-arg methods}}
//                   ^^^^^^^^^^^^
  }

  @Scheduled(cron = "*/5 * * * * MON-FRI")
// ^^^^^^^^^ >
  public void oneArgCron(int i) { // Noncompliant {{"@Scheduled" annotation should only be applied to no-arg methods}}
//            ^^^^^^^^^^
  }

  @Deprecated
  @Scheduled(fixedDelay = 5000)
// ^^^^^^^^^ >
  public int twoArgAndReturnType(int i, int j) { // Noncompliant {{"@Scheduled" annotation should only be applied to no-arg methods}}
//           ^^^^^^^^^^^^^^^^^^^
    return 0;
  }

  @Scheduled(fixedDelay = 5000)
// ^^^^^^^^^ >
  @Scheduled(fixedDelay = 20000)
// ^^^^^^^^^ >
  public void repeated(int i) { // Noncompliant {{"@Scheduled" annotation should only be applied to no-arg methods}}
//            ^^^^^^^^
  }

  public int noAnnotation() {
    return -1;
  }

  public int noAnnotationWithArgs(Integer a, Double b) {
    return -1;
  }
}
