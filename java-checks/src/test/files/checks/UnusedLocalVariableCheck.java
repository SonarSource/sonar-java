class Foo {

  int unusedField;

  public void f(int unusedParameter) {
    int unusedLocalVariable;

    int usedLocalVariable = 42;
    System.out.println(usedLocalVariable);

    try {
    } catch (Exception e) { // Compliant
    }

    try (Stream foo = new Stream()) { // Compliant
    }
  }

}
