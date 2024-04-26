package checks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirtualThreadNotSynchronizedCheckSample {

  void smoketest() {
    Thread.startVirtualThread(() -> { // Noncompliant {{Use a platform thread instead of a virtual thread}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^
      synchronized(this) {
//  ^^^<
        System.out.println();
      }
    });
  }

  void runnableToCheckForSynchronized() {
    Thread.startVirtualThread(() -> synchronizedMethod()); // Noncompliant
    Thread.ofVirtual().start(() -> synchronizedMethod()); // Noncompliant
    Thread.ofVirtual().unstarted(() -> synchronizedMethod()); // Noncompliant

    Thread.Builder builderToCheck = Thread.ofVirtual(); // Secondary
    builderToCheck.start(() -> synchronizedMethod()); // Noncompliant

    Executors.newVirtualThreadPerTaskExecutor().execute(() -> synchronizedMethod()); // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//  ^^^<
    ExecutorService executorToCheck = Executors.newVirtualThreadPerTaskExecutor();
    executorToCheck.execute(() -> synchronizedMethod()); // Noncompliant
    executorToCheck.submit(() -> synchronizedMethod()); // Noncompliant
    executorToCheck.submit(() -> synchronizedMethodForCallable()); // Noncompliant
    executorToCheck.submit(() -> synchronizedMethod(), 42); // Noncompliant
  }

  void runnableNotToCheckForSynchronized(){
    Thread.ofPlatform().start(() -> synchronizedMethod()); // Compliant
    Thread.ofPlatform().unstarted(() -> synchronizedMethod()); // Compliant

    Thread.Builder builderNotToCheck = Thread.ofPlatform(); // Compliant
    builderNotToCheck.start(() -> synchronizedMethod()); // Compliant

    Executors.newSingleThreadExecutor().execute(() -> synchronizedMethod()); // Compliant
    ExecutorService executorNotToCheck = Executors.newCachedThreadPool();
    executorNotToCheck.execute(() -> synchronizedMethod()); // Compliant
    executorNotToCheck.submit(() -> synchronizedMethod()); // Compliant
    executorNotToCheck.submit(() -> synchronizedMethodForCallable()); // Compliant
    executorNotToCheck.submit(() -> synchronizedMethod(), 42); // Compliant
  }

  void runnableToCheckForSynchronized(Thread.Builder.OfVirtual builderToCheck) {
    builderToCheck.start(() -> synchronizedMethod()); // Noncompliant
  }

  void runnableNotToCheckForSynchronized(Thread.Builder.OfPlatform builderNotToCheck) {
    builderNotToCheck.start(() -> synchronizedMethod()); // Compliant
  }

  void runnableNotToCheckForSynchronized(Thread.Builder builderNotToCheck) {
    builderNotToCheck.start(() -> synchronizedMethod()); // Compliant
  }

  private synchronized void synchronizedMethod() {}
  private synchronized int synchronizedMethodForCallable() { return 42; }

  private void nonSynchronizedMethod() {}

  private void methodWithSynchronizedBlock() {
    synchronized (this) {}
//  ^^^<
  }

  private void methodInvokingSynchronizedBlockL2() {
    // recursive calls must not break analysis
    methodInvokingSynchronizedBlockL4();
    // actual call of a method with synchronized block
    methodWithSynchronizedBlock();
  }

  private void methodInvokingSynchronizedBlockL3() {
    methodInvokingSynchronizedBlockL2();
  }

  private void methodInvokingSynchronizedBlockL4() {
    methodInvokingSynchronizedBlockL3();
  }

  void testPropagationOfSynchronizedAttribute(boolean condition) {
    Thread.startVirtualThread(() -> { // Noncompliant
      synchronizedMethod();
    });

    Thread.startVirtualThread(() -> { // Compliant
      nonSynchronizedMethod();
    });

    Thread.startVirtualThread(() -> { // Noncompliant
      methodWithSynchronizedBlock();
    });

    Thread.startVirtualThread(() -> { // Noncompliant
      if (condition) {
        System.out.println();
        synchronized (this) {
          System.out.println();
        }
      }
    });

    Thread.startVirtualThread(() -> { // Noncompliant
      if (condition) {
        System.out.println();
        methodWithSynchronizedBlock();
      }
    });

    Thread.startVirtualThread(() -> { // Compliant
      if (condition) {
        System.out.println();
        nonSynchronizedMethod();
      }
    });

    Thread.startVirtualThread(() -> { // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^^
      methodInvokingSynchronizedBlockL4();
    });
  }

  void increaseCoverageNoVariableTree() {
    builderNotToCheck.start(() -> synchronizedMethod()); // Compliant
    executorNotToCheck.execute(() -> synchronizedMethod()); // Compliant
  }

  private ExecutorService executorNotToCheck;
  private Thread.Builder builderNotToCheck;

  void increaseCoverageVariableWithoutInitializer() {
    Thread.Builder builderToCheck;
    builderToCheck = Thread.ofVirtual();
    builderToCheck.start(() -> synchronizedMethod()); // Compliant
  }

  void increaseCoverageSynchronizedAttributeAlreadyFound() {
    Thread.startVirtualThread(() -> { // Noncompliant
      synchronized(this) {}
      synchronized(this) {}
      synchronizedMethod();
    });
  }

  void increaseCoverageMethodHasNoBlock(Fooable fooable) {
    Thread.startVirtualThread(() -> { // Compliant
      fooable.foo();
    });
  }

  interface Fooable {
    void foo();
  }
}
