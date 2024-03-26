package checks;

import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

class BooleanMethodReturnCheckSampleA {
  public Boolean myMethod() {
    return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
  }

  public Boolean myOtherMethod() {
    return Boolean.TRUE; // Compliant
  }

  BooleanMethodReturnCheckSampleA() {
    // constructor (with null return type) are not covered by the rule
    return;
  }

  @Nullable
  public Boolean foo() {
    return null; // Compliant
  }

  @org.jetbrains.annotations.Nullable
  public Boolean bar() {
    return null; // Compliant
  }
}

class BooleanMethodReturnCheckSampleB {
  private class Boolean {
  }

  public Boolean myMethod() {
    return null; // Compliant
  }

  public java.lang.Boolean myOtherMethod() {
    class BooleanMethodReturnCheckSampleC {
      private java.lang.Boolean myInnerMethod() {
        return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
      }
      private BooleanMethodReturnCheckSampleC foo() {
        return null; // Compliant
      }
    }
    return null; // Noncompliant [[sc=12;ec=16]] {{Null is returned but a "Boolean" is expected.}}
  }

  @CheckForNull
  public java.lang.Boolean myMethod2() {
    return null; // compliant method is annotated with @CheckForNull
  }
}

class BooleanMethodReturnCheckSampleD {
  public Boolean foo() {
    class BooleanMethodReturnCheckSampleE {
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
