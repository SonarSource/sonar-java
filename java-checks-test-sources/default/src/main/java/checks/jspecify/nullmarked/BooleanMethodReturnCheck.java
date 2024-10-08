package checks.jspecify.nullmarked;

import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.jspecify.annotations.NullUnmarked;

// NullMarked at the package level
class BooleanMethodReturnCheckJSpecifySampleA {
  public Boolean myMethod() {
    return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
  }

  public Boolean myOtherMethod() {
    return Boolean.TRUE; // Compliant
  }

  BooleanMethodReturnCheckJSpecifySampleA() {
    // constructor (with null return type) are not covered by the rule
    return;
  }

  @Nullable
  public Boolean foo() {
    return null; // Compliant
  }

  @NullUnmarked
  public Boolean bar() {
    return null; // Compliant
  }

  public Boolean foobar() {
    return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
  }
  
}

// NullMarked at the package level
class BooleanMethodReturnCheckJSpecifySampleB {
  private class Boolean {
  }

  public Boolean myMethodFailing() {
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
    return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
//         ^^^^
  }

  @CheckForNull
  public java.lang.Boolean myMethod2() {
    return null; // compliant method is annotated with @CheckForNull
  }
}

// NullMarked at the package level
class BooleanMethodReturnCheckJSpecifySampleD {
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
