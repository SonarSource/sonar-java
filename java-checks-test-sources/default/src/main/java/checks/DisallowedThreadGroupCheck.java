package checks;

abstract class DisallowedThreadGroupCheck
  extends ThreadGroup { // Noncompliant

  DisallowedThreadGroupCheck (ThreadGroup tg) { // Noncompliant
    super("DisallowedThreadGroupCheck");
  }

  void foo(
    Object o,
    ThreadGroup threadGroup) { // Noncompliant

    ThreadGroup tg = // Noncompliant {{Remove this use of "ThreadGroup". Prefer the use of "ThreadPoolExecutor".}}
//  ^^^^^^^^^^^
      new ThreadGroup("DisallowedThreadGroupCheck"); // Compliant

    tg.activeCount(); // Compliant - not following method invocation, only declarations of ThreadGroup
    tg.activeGroupCount(); // Compliant
    getThreadGroup().checkAccess(); // Compliant
    tg.destroy(); // Compliant
    tg.enumerate(new Thread[0]); // Compliant
    tg.enumerate(new Thread[0], true); // Compliant
    tg.enumerate(new ThreadGroup[0]); // Compliant
    tg.enumerate(new ThreadGroup[0], false); // Compliant
    tg.getMaxPriority(); // Compliant
    tg.getName(); // Compliant
    tg.getParent(); // Compliant
    tg.interrupt(); // Compliant
    tg.isDaemon(); // Compliant
    tg.isDestroyed(); // Compliant
    tg.setDaemon(true); // Compliant
    tg.list(); // Compliant
    tg.parentOf(tg); // Compliant
    tg.setMaxPriority(0); // Compliant
    tg.uncaughtException(new Thread(), new Exception()); // Compliant
    tg.toString(); // Compliant
    tg.equals(o);  // Compliant - not overridden in ThreadGroup
  }

  void qix(Object o, boolean b) {
  }

  abstract ThreadGroup getThreadGroup(); // Noncompliant
}

class DisallowedThreadGroupCheckB extends DisallowedThreadGroupCheck { // Compliant

  DisallowedThreadGroupCheckB(ThreadGroup tg) {
    super(tg);
  }

  @Override
  ThreadGroup getThreadGroup() { // Compliant
    return null;
  }

  @Override
  void foo(Object o, ThreadGroup threadGroup) { } // Compliant

  @Override
  void qix(Object o, boolean b) {
    ThreadGroup tg = new ThreadGroup("DisallowedThreadGroupCheckB"); // Noncompliant
  }
}

class DisallowedThreadGroupCheckC {
  Object foo() {
    Object o = new Object();
    return o;
  }
}
