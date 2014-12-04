class FooClass {

  private int unusedField; // Noncompliant

  @UsedBySomeFramework
  private int foo; // Noncompliant

  int usedField; // Compliant

  public int foo; // Compliant

  private static final long serialVersionUID = 4858622370623524688L; // Compliant

  public void f(int unusedParameter) {
    int unusedLocalVariable;

    int usedLocalVariable = 42 + usedField;
    System.out.println(usedLocalVariable);

    try {
    } catch (Exception e) { // Compliant
    }

    try (Stream foo = new Stream()) { // Noncompliant
    }

    for (int a: new int[]{ 0, 1, 2 }) { // Noncompliant
    }
  }

}

enum FooEnum {

  FOO;

}

interface FooInterface {

  int FOO = 0; // Compliant

}

class Lombok1 {
  @lombok.Getter
  private int FOO = 0; // Compliant
}

@lombok.Getter
class Lombok2 {
  private int FOO = 0; // Compliant
}

@lombok.Data
class Lombok3 {
  private int FOO = 0; // Compliant
}

@lombok.Value
class Lombok4 {
  private int FOO = 0; // Compliant
}

@lombok.Setter
class Lombok5 {
  private int FOO = 0; // Compliant (no escape analysis)
}

class Lombok6 {
  @lombok.NonNull
  private int FOO = 0; // Compliant (no escape analysis)
}

@lombok.AllArgsConstructor
class Lombok7 {
  private int FOO = 0; // Compliant (no escape analysis)
}
