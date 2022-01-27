package checks.unused;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

class UnusedLocalVariableCheck {

  int unusedField;

  {
    int unused = 42; // Noncompliant
    int used = 23; // Compliant
    System.out.println(used);
  }

  static {
    int unused = 42; // Noncompliant
    int used = 23; // Compliant
    System.out.println(used);
  }

  private UnusedLocalVariableCheck(int unusedParameter) { // Compliant
    int unused = 42; // Noncompliant
    int used = 23; // Compliant
    System.out.println(used);
  }

  public void f(int unusedParameter, Object o) {
    int unusedLocalVariable; // Noncompliant [[sc=9;=ec=28]] {{Remove this unused "unusedLocalVariable" local variable.}}

    int usedLocalVariable = 42;
    System.out.println(usedLocalVariable);

    try {
    } catch (Exception e) {
    }

    try (Stream foo = Stream.of()) { // Compliant
    }

    for (int a : new int[]{0, 1, 2}) { // Noncompliant
    }

    for (int i = 0; condition(); i++) { // Noncompliant
    }

    for (int j = 0; j < 10; j++) {
    }

    try (Stream foo2 = Stream.of()) {
      int x = 42; // Noncompliant
      foo2.findFirst();
    }

    int notReadLocalVariable = 0; // Noncompliant
    notReadLocalVariable = 1;
    notReadLocalVariable += 1;
    notReadLocalVariable++;
    (notReadLocalVariable)++;

    int readLocalVariable = 0;
    notReadLocalVariable = readLocalVariable++;

    int readLocalVariable2 = 0;
    int unreadLocalVariable2 = 1; // Noncompliant
    unreadLocalVariable2 = readLocalVariable2;

    java.util.stream.Stream<Object> s = Stream.of();
    s.map(v -> "");

    try (Stream foo3 = Stream.of()) {
      foo3.findFirst();
    }
    try (Stream foo3 = Stream.of()) {
      foo3.findFirst();
    }

    if (o instanceof String usedPatternVar) {
      System.out.println(usedPatternVar);
    }

    if (o instanceof String unusedPatternVar) { // Noncompliant [[sc=29;=ec=45]]

    }
  }

  private boolean condition() {
    return false;
  }

  public BinaryOperator<UnaryOperator<Object>> foo() {
    return (a, b) -> input -> {
      Object o = a.apply(input); // Compliant, lambda expression correctly handled
      o.toString();
      return o;
    };
  }

  class QuickFixes {
    private void doSomething(Object parameter) {
      int unusedAndAlone; // Noncompliant [[sc=11;ec=25;quickfixes=qfzero]]
      // fix@qfzero {{Remove unused local variable}}
      // edit@qfzero [[sc=7;ec=26]]{{}}
      int unusedFirst = 42, used = 1; // Noncompliant [[sc=11;ec=22;quickfixes=qf0]]
      // fix@qf0 {{Remove unused local variable}}
      // edit@qf0 [[sc=11;ec=29]]{{}}

      int first = 0, unusedSecond, third = 1; // Noncompliant [[sc=22;ec=34;quickfixes=qf1]]
      // fix@qf1 {{Remove unused local variable}}
      // edit@qf1 [[sc=22;ec=36]]{{}}

      int alpha = 0, beta = 1, unusedThird; // Noncompliant [[sc=32;ec=43;quickfixes=qf2]]
      // fix@qf2 {{Remove unused local variable}}
      // edit@qf2 [[sc=30;ec=43]]{{}}

      int initializedButNotRead = used + first + third + alpha + beta; // Noncompliant [[sc=11;ec=32;quickfixes=qf3]]
      // fix@qf3 {{Remove unused local variable}}
      // edit@qf3 [[sc=7;ec=71]]{{}}

      String unitializedAndUnused; // Noncompliant [[sc=14;ec=34;quickfixes=qf4]]
      // fix@qf4 {{Remove unused local variable}}
      // edit@qf4 [[sc=7;ec=35]]{{}}

      if (parameter instanceof String unusedMatch) { // Noncompliant [[sc=39;ec=50;quickfixes=qf5]]
        // fix@qf5 {{Remove unused local variable}}
        // edit@qf5 [[sc=39;ec=50]]{{}}
      }

      for (int i, j = 0; j < 10; j++) { // Noncompliant [[sc=16;ec=17;quickfixes=qf6]]
        // fix@qf6 {{Remove unused local variable}}
        // edit@qf6 [[sc=16;ec=18]] {{}}
        System.out.println(j);
      }
      for (int i = 0, j; i < 10; i++) { // Noncompliant [[sc=23;ec=24;quickfixes=qf7]]
        // fix@qf7 {{Remove unused local variable}}
        // edit@qf7 [[sc=21;ec=24]] {{}}
        System.out.println(i);
      }

      for (int i = 0; condition();) { // Noncompliant [[sc=16;ec=17;quickfixes=qf8]]
        // fix@qf8 {{Remove unused local variable}}
        // edit@qf8 [[sc=12;ec=21]] {{}}
      }

      final String unusedFinalAlone = "Hello"; // Noncompliant [[sc=20;ec=36;quickfixes=qf9]]
      // fix@qf9 {{Remove unused local variable}}
      // edit@qf9 [[sc=7;ec=47]] {{}}

      final String unusedFinalFirst, usedFinalLast = "Bye!"; // Noncompliant [[sc=20;ec=36;quickfixes=qf10]]
      // fix@qf10 {{Remove unused local variable}}
      // edit@qf10 [[sc=20;ec=38]] {{}}
      System.out.println(usedFinalLast);

      final String usedFinalFirst = "Hello!", unusedFinalLast; // Noncompliant [[sc=47;ec=62;quickfixes=qf11]]
      // fix@qf11 {{Remove unused local variable}}
      // edit@qf11 [[sc=45;ec=62]] {{}}
      System.out.println(usedFinalFirst);

      var unusedInferredAlone = 42; // Noncompliant [[sc=11;ec=30;quickfixes=qf12]]
      // fix@qf12 {{Remove unused local variable}}
      // edit@qf12 [[sc=7;ec=36]] {{}}
    }

    {
      int unusedAndAlone; // Noncompliant [[sc=11;ec=25;quickfixes=qfzero]]
      int unusedFirst = 42, used = 1; // Noncompliant [[sc=11;ec=22;quickfixes=qf0]]
      int first = 0, unusedSecond, third = 1; // Noncompliant [[sc=22;ec=34;quickfixes=qf1]]
      int alpha = 0, beta = 1, unusedThird; // Noncompliant [[sc=32;ec=43;quickfixes=qf2]]
      int initializedButNotRead = used + first + third + alpha + beta; // Noncompliant [[sc=11;ec=32;quickfixes=qf3]]
      String unitializedAndUnused; // Noncompliant [[sc=14;ec=34;quickfixes=qf4]]
    }

    static {
      int unusedAndAlone; // Noncompliant [[sc=11;ec=25;quickfixes=qfzero]]
      int unusedFirst = 42, used = 1; // Noncompliant [[sc=11;ec=22;quickfixes=qf0]]
      int first = 0, unusedSecond, third = 1; // Noncompliant [[sc=22;ec=34;quickfixes=qf1]]
      int alpha = 0, beta = 1, unusedThird; // Noncompliant [[sc=32;ec=43;quickfixes=qf2]]
      int initializedButNotRead = used + first + third + alpha + beta; // Noncompliant [[sc=11;ec=32;quickfixes=qf3]]
      String unitializedAndUnused; // Noncompliant [[sc=14;ec=34;quickfixes=qf4]]
    }

    private void doNotOfferQuickFixes() {
      int unusedButIncremented = 0; // Noncompliant [[sc=11;ec=31;quickfixes=!]]
      unusedButIncremented++;

      for (int counter = 0; condition(); counter++) { // Noncompliant [[sc=16;ec=23;quickfixes=!]]
      }
    }

    void test() {
      record Bar(int used) { } // Compliant
      System.out.println(new Bar(42).used);
    }
  }
}
