import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

class A {
  boolean foo(File f, Lock l) {
    f.delete(); // Noncompliant [[sc=5;ec=16]] {{Do something with the "boolean" value returned by "delete".}}
    boolean b1 = f.delete(); // Noncompliant [[sc=5;ec=29]] {{Do something with the "boolean" value returned by "delete".}}
    boolean b2 = f.delete(); // Compliant
    if (b2 || f.delete()) {} // Compliant
    l.tryLock(); // Noncompliant [[sc=5;ec=17]] {{Do something with the "boolean" value returned by "tryLock".}}
    l.tryLock(0L, TimeUnit.DAYS); // Compliant
    return l.tryLock(); // Compliant
  }
}
