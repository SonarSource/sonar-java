import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;

class BoxedBoolean {
  @SuppressWarnings("boxing")
  void S5411(Boolean b) {
    if (b) { // NoIssue
      int i;
    }
  }

  void S5411NotSuppressed(Boolean b) {
    if (b) { // WithIssue
      int i;
    }
  }

  void S2153(Boolean b) {
    double d = 1.0;
    @SuppressWarnings("boxing")
    int dIntValue1 = new Double(d).intValue(); // NoIssue

    int dIntValue2 = new Double(d).intValue(); // WithIssue

    int sum = dIntValue1 + dIntValue2;
  }
}

class TypeParameterHidesAnotherType<T> {

  @SuppressWarnings("hiding")
  private static <T> T method_suppressed() { // NoIssue
    return null;
  }

  private static <T> T method_not_suppressed() { // WithIssue
    return null;
  }

}

class NullDereferenceCheck {

  @SuppressWarnings("null")
  void S2259(String s) {
    if (s == null) {
      s.toString(); // NoIssue
    }
  }

  void S2259NotSuppressed(String s) {
    if (s == null) {
      s.toString(); // WithIssue
    }
  }
}

class UnclosedResources {

  @SuppressWarnings("resource")
  void S2093(String fileName) {
    FileReader fr = null;
    try { // NoIssue
      fr = new FileReader(fileName);
    } catch (Exception e) {
    } finally {
      if (fr != null) {
        try {
          fr.close();
        } catch (IOException e) {
        }
      }
    }
  }

  void S2093_not_suppressed(String fileName) {
    FileReader fr = null;
    try { // WithIssue
      fr = new FileReader(fileName);
    } catch (Exception e) {
    } finally {
      if (fr != null) {
        try {
          fr.close();
        } catch (IOException e) {
        }
      }
    }
  }

  @SuppressWarnings("resource")
  void S2095(String fileName) throws FileNotFoundException {
    FileReader fr = new FileReader(fileName); // NoIssue
    fr.toString();
  }

  void S2095_not_suppressed(String fileName) throws FileNotFoundException {
    FileReader fr = new FileReader(fileName); // WithIssue
    fr.toString();
  }
}

class DeprecatedSuppress {
  @Deprecated
  class Foo { }

  @SuppressWarnings("restriction")
  class Bar extends Foo { } // NoIssue
  class BarNotSuppressed extends Foo { } // WithIssue

  @Deprecated(forRemoval = true)
  class FooRemoval { }

  @SuppressWarnings("removal")
  class BarRemoval extends FooRemoval { } // NoIssue
  class BarNotSuppressedRemoval extends FooRemoval { } // WithIssue

}

@SuppressWarnings("serial")
class SerialVersionUidCheckSuppressed implements Serializable { // NoIssue
}

class SerialVersionUidCheck implements Serializable { // WithIssue
}

class StaticSuppression {
  static class A {
    static int i = 0;
  }

  @SuppressWarnings("static-access")
  void S2696() {
    A a = new A();
    a.i++; // NoIssue
  }

  void S2696_not_suppressed() {
    A a = new A();
    a.i++; // WithIssue
  }

  @SuppressWarnings("static-method")
  private void S2325() { // NoIssue
    int i = 1;
  }

  private void S2325_not_suppressed() { // WithIssue
    int i = 1;
  }
}

class SynchronizedOverrideCheckParent {
  synchronized void foo() {
    //...
  }
}

class SynchronizedOverrideCheckChild extends SynchronizedOverrideCheckParent {
  @Override
  @SuppressWarnings("sync-override")
  public void foo () {  // NoIssue
    super.foo();
  }
}

class SynchronizedOverrideCheckChildNotSuppressed extends SynchronizedOverrideCheckParent {
  @Override
  public void foo () {  // WithIssue
    super.foo();
  }
}
