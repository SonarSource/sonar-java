abstract class A
  extends ThreadGroup { // Noncompliant

  A (ThreadGroup tg) { // Noncompliant
  }

  void foo(
    Object o,
    ThreadGroup threadGroup) {  // Noncompliant 

    ThreadGroup tg = // Noncompliant [[sc=5;ec=16]] {{Remove this use of "ThreadGroup". Prefer the use of "ThreadPoolExecutor".}}
      new ThreadGroup("A"); // Compliant

    tg.activeCount(); // Compliant - not following method invocation, only declarations of ThreadGroup
    tg.activeGroupCount(); // Compliant
    tg.allowThreadSuspension(true); // Compliant
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
    tg.resume(); // Compliant
    tg.setMaxPriority(0); // Compliant
    tg.stop(); // Compliant
    tg.suspend(); // Compliant
    tg.uncaughtException(new Thread(), new Exception()); // Compliant
    tg.toString(); // Compliant
    tg.equals(o);  // Compliant - not overriden in ThreadGroup
  }

  void qix(Object o, boolean b) {
  }

  abstract ThreadGroup getThreadGroup(); // Noncompliant
}

class B extends A { // Compliant
  @Override
  ThreadGroup getThreadGroup() { // Compliant
    return null;
  }

  @Override
  void foo(Object o, ThreadGroup threadGroup) { } // Compliant

  @Override
  void qix(Object o, boolean b) {
    ThreadGroup tg = new ThreadGroup("B"); // Noncompliant
  }
}

class C {
  Object foo() {
    Object o = new Object();
  }
}
