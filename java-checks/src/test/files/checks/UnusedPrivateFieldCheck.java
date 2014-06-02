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
