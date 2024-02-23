package checks;

public class SingleIfInsteadOfPatternMatchGuardCheckSample {

  void coverage(Object o, int i) {
    switch (i) {
      case 1 -> System.out.println("one");
      default -> System.out.println("many");
    }
    switch (o) {
      case Long l when l == 1 -> System.out.println("long");
      case Double d -> System.out.println("double");
      case String s -> System.out.println("string");
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
      case Integer i -> {
      }
      case String s -> {
        if (s.length() == 2) { // Noncompliant {Replace this "if" statement with a pattern match guard.}
          System.out.println("two");
        }
      }
      default -> System.out.println("many");
    }
  }

  void standardSwitch(Integer i) {
    switch (i) {
      case 1, 2, 3:
        System.out.println("one, two or three");
        break;
    }
  }

  void compliant(int i) {
    switch (i) {
      case 1 -> {
      }
      case 2, 3 -> {
      }
    }
  }

}
