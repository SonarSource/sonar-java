class Foo {

  int unusedField; // Noncompliant

  @UsedBySomeFramework
  int foo; // Noncompliant

  int usedField; // Compliant

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
