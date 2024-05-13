package checks;

import java.util.Optional;

public class BoxedBooleanExpressionsCheckWithQuickFixes {

  void boxedFor1(Boolean B, boolean b) {
    if (B) { // Noncompliant [[quickfixes=qf1]]
//      ^
      // fix@qf1 {{Use a primitive boolean expression}}
      // edit@qf1 [[sc=9;ec=9]] {{Boolean.TRUE.equals(}}
      // edit@qf1 [[sc=10;ec=10]] {{)}}
      foo();
    }

    if (!B) { // Noncompliant [[quickfixes=qf2]]
//       ^
      // fix@qf2 {{Use a primitive boolean expression}}
      // edit@qf2 [[sc=9;ec=10]] {{Boolean.FALSE.equals(}}
      // edit@qf2 [[sc=11;ec=11]] {{)}}
      foo();
    }

    for (;B;) { // Noncompliant [[quickfixes=qf3]]
//        ^
      // fix@qf3 {{Use a primitive boolean expression}}
      // edit@qf3 [[sc=11;ec=11]] {{Boolean.TRUE.equals(}}
      // edit@qf3 [[sc=12;ec=12]] {{)}}
      foo();
    }

    if (True()) { // Noncompliant [[quickfixes=qf4]]
//      ^^^^^^
      // fix@qf4 {{Use a primitive boolean expression}}
      // edit@qf4 [[sc=9;ec=9]] {{Boolean.TRUE.equals(}}
      // edit@qf4 [[sc=15;ec=15]] {{)}}
      foo();
    }

    if (B == b) { // Noncompliant [[quickfixes=qf5]]
//      ^
      // fix@qf5 {{Use a primitive boolean expression}}
      // edit@qf5 [[sc=9;ec=9]] {{Boolean.TRUE.equals(}}
      // edit@qf5 [[sc=10;ec=10]] {{)}}
      foo();
    }

    if (b == B) { // Noncompliant [[quickfixes=qf6]]
//           ^
      // fix@qf6 {{Use a primitive boolean expression}}
      // edit@qf6 [[sc=14;ec=14]] {{Boolean.TRUE.equals(}}
      // edit@qf6 [[sc=15;ec=15]] {{)}}
      foo();
    }

    if (Optional.of(True()).orElse(null)) { // Noncompliant [[quickfixes=!]]
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
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
