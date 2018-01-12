import org.apache.commons.lang.BooleanUtils;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

class A {
  public Boolean myMethod() {
    return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
  }

  public Boolean myOtherMethod() {
    return Boolean.TRUE; // Compliant
  }

  A() {
    // constructor (with null return type) are not covered by the rule
    return;
  }

  @Nullable
  public Boolean foo() {
    return null; // Compliant
  }
}

class B {
  private class Boolean {
  }

  public Boolean myMethod() {
    return null; // Compliant
  }

  public java.lang.Boolean myOtherMethod() {
    private class C {
      private java.lang.Boolean myInnerMethod() {
        return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
      }
      private C foo() {
        return null; // Compliant
      }
    }
    return null; // Noncompliant [[sc=12;ec=16]] {{Null is returned but a "Boolean" is expected.}}
  }

  @CheckForNull
  public java.lang.Boolean myMethod() {
    return null; // compliant method is annotated with @CheckForNull
  }
}

class D {
  public Boolean foo() {
    class E {
      void bar() {
        return;
      }
    }
    Stream.of("A").forEach(a -> {
      return; // Compliant
    });
    return true;
  }
}
