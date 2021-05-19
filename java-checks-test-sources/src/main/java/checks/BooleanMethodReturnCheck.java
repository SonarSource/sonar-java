package checks;

import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

class BooleanMethodReturnCheckA {
  public Boolean myMethod() {
    return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
  }

  public Boolean myOtherMethod() {
    return Boolean.TRUE; // Compliant
  }

  BooleanMethodReturnCheckA() {
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

class BooleanMethodReturnCheckB {
  private class Boolean {
  }

  public Boolean myMethod() {
    return null; // Compliant
  }

  public java.lang.Boolean myOtherMethod() {
    class BooleanMethodReturnCheckC {
      private java.lang.Boolean myInnerMethod() {
        return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
      }
      private BooleanMethodReturnCheckC foo() {
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

class BooleanMethodReturnCheckD {
  public Boolean foo() {
    class BooleanMethodReturnCheckE {
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
