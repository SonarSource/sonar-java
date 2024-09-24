package checks;

public class SingleIfInsteadOfPatternMatchGuardCheckSample {

  // fix@qf4 {{Merge this "if" statement with the enclosing pattern match guard.}}
  // edit@qf4 [[sl=+0;el=+2;sc=9;ec=10]] {{System.out.println("even");}}
  // edit@qf4 [[sl=-1;el=-1;sc=44;ec=44]] {{ && x % 2 == 0 }}
  // edit@qf4 [[sl=-1;el=-1;sc=28;ec=28]] {{(}}
  // edit@qf4 [[sl=-1;el=-1;sc=43;ec=43]] {{)}}
  void test(Foo foo){
    switch (foo){
      case Foo(var x) when x < 0 || x > 10 -> {
        if (x % 2 == 0) { // Noncompliant [[quickfixes=qf4]]
          System.out.println("even");
        }
      }
      default -> {}
    }
  }

  record Foo (int x) {}

  // fix@qf3 {{Merge this "if" statement with the enclosing pattern match guard.}}
  // edit@qf3 [[sl=+0;el=+2;sc=9;ec=10]] {{System.out.println("two");}}
  // edit@qf3 [[sl=-2;el=-2;sc=44;ec=44]] {{ && s.length() == 2 }}
  void conditionsShouldBeMerged(Object o) {
    switch (o) {
      case String s when s.startsWith("a") -> {
        // Noncompliant@+1 [[quickfixes=qf3]]
        if (s.length() == 2) {
          System.out.println("two");
        }
      }
      default -> {
      }
    }
  }
  // fix@qf1 {{Replace this "if" statement with a pattern match guard.}}
  // edit@qf1 [[sl=+0;el=+0;sc=9;ec=32]] {{{}}}
  // edit@qf1 [[sl=-2;el=-2;sc=21;ec=21]] {{ when s.length() == 2 }}
  void quickFix(Object o) {
    switch (o) {
      case null -> {
        if (true) {
          System.out.println("null");
        }
      }
      case String s -> {
        // Noncompliant@+1 [[sl=+1;el=+1;sc=9;ec=32;quickfixes=qf1]]
        if (s.length() == 2) {}
      }
      default -> {
        if (o instanceof Integer) {
          System.out.println("many");
        }
      }
    }
  }

  // fix@qf2 {{Replace this "if" statement with a pattern match guard.}}
  // edit@qf2 [[sl=+0;el=+0;sc=9;ec=56]] {{System.out.println("two");}}
  // edit@qf2 [[sl=-2;el=-2;sc=21;ec=21]] {{ when s.length() == 2 }}
  void quickFix2(Object o) {
    switch (o) {
      case String s -> {
        // Noncompliant@+1 [[sl=+1;el=+1;sc=9;ec=56;quickfixes=qf2]]
        if (s.length() == 2) System.out.println("two");
      }
      default -> System.out.println("many");
    }
  }


  void coverage(Object o, int i) {
    switch (i) {
      case 1 -> System.out.println("one");
      default -> System.out.println("many");
    }
    switch (o) {
      case Long l when l == 1 -> System.out.println("long");
      case Double _ -> System.out.println("double");
      case String _ -> System.out.println("string");
      default -> System.out.println("many");
    }
  }

  void foo(Object o) {
    switch (o) {
      case Long l -> {
        if (l == 3) { // Compliant; cannot merge the else into a single type match guard
          System.out.println("three");
        } else {
          System.out.println("many");
        }
      }
      case Double d -> {
        if (d == 3) { // Compliant; cannot merge the else into a single type match guard
          System.out.println("three");
        } else if (d == 4) {
          System.out.println("many");
        }
      }
      case String s when s.length() == 1 -> System.out.println("one");
      case Character c -> {
        switch (c) {
          case 'a' -> System.out.println("a");
          case 'b' -> System.out.println("b");
          default -> System.out.println("many");
        }
      }
      case Integer _ -> {
      }
      case String s -> {
        if (s.length() == 2) { // Noncompliant {{Replace this "if" statement with a pattern match guard.}}
          System.out.println("two");
        }
      }
      default -> System.out.println("many");
    }
  }

  void standardSwitch(Integer i) {
    switch (i) {
      case 1, 2, 3:
        if (i < 3) {
          System.out.println("one, two");
        }
        break;

      case 0:
    }
  }

  void compliant(int i, Object o) {
    switch (i) {
      case 1 -> {
      }
      case 2, 3 -> {
      }
    }
  }

}
