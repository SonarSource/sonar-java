package checks;

import javax.annotation.Nonnull;

public class EqualsParametersMarkedNonNullCheck {

  static class A {
    public boolean equals(@Nonnull Object obj) { // Noncompliant [[sc=27;ec=35;quickfixes=qf1]] {{"equals" method parameters should not be marked "@Nonnull".}}
      // fix@qf1 {{Remove "@Nonnull"}}
      // edit@qf1 [[sc=27;ec=36]] {{}}
      return true;
    }
  }

  static class B {
    public boolean equals(Object obj) { // Compliant
      return true;
    }
  }

  static class C {
    public boolean equal() {
      return true;
    } // Compliant

    public boolean equals(Object a, Object b) { // Compliant
      return true;
    }
  }

  static class D {
    public boolean equals(C c) { // Compliant
      return false;
    }
  }

  static class E {
    public boolean equals(@Nonnull C c) { // Compliant
      return false;
    }
  }

  static class F {
    public boolean equals(
      @javax.validation.constraints.NotNull // Noncompliant [[sc=7;ec=44;el=+0;quickfixes=qf2]] {{"equals" method parameters should not be marked "@NotNull".}}
      java.lang.Object object) {
      // fix@qf2 {{Remove "@NotNull"}}
      // edit@qf2 [[sc=7;ec=7;el=+1]] {{}}
      return false;
    }
  }

  @org.eclipse.jdt.annotation.NonNullByDefault
  static class G {
    public boolean equals(Object object) { // Compliant
      return false;
    }
  }

}
