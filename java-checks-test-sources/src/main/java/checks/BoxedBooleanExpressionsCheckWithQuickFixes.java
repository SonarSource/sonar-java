package checks;

import java.util.Optional;

public class BoxedBooleanExpressionsCheckWithQuickFixes {

  void boxedFor1(Boolean B) {
    if (B) { // Noncompliant [[sc=9;ec=10;quickfixes=qf1]]
      // fix@qf1 {{Use the primitive boolean expression}}
      // edit@qf1 [[sc=9;ec=9]] {{Boolean.TRUE.equals(}}
      // edit@qf1 [[sc=10;ec=10]] {{)}}
      foo();
    }

    if (!B) { // Noncompliant [[sc=10;ec=11;quickfixes=qf2]]
      // fix@qf2 {{Use the primitive boolean expression}}
      // edit@qf2 [[sc=9;ec=10]] {{Boolean.FALSE.equals(}}
      // edit@qf2 [[sc=11;ec=11]] {{)}}
      foo();
    }

    for (;B;) { // Noncompliant [[sc=11;ec=12;quickfixes=qf3]]
      // fix@qf3 {{Use the primitive boolean expression}}
      // edit@qf3 [[sc=11;ec=11]] {{Boolean.TRUE.equals(}}
      // edit@qf3 [[sc=12;ec=12]] {{)}}
      foo();
    }

    if (True()) { // Noncompliant [[sc=9;ec=15;quickfixes=qf4]]
      // fix@qf4 {{Use the primitive boolean expression}}
      // edit@qf4 [[sc=9;ec=9]] {{Boolean.TRUE.equals(}}
      // edit@qf4 [[sc=15;ec=15]] {{)}}
      foo();
    }

    if (Optional.of(True()).orElse(null)) { // Noncompliant [[sc=9;ec=41;quickfixes=!]]
      foo();
    }
  }

  private Boolean True() {
    return Boolean.TRUE;
  }

  private void foo() {
  }

  private void bar() {
  }


}
