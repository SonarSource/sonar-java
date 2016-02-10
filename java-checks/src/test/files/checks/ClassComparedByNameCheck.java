class A {
    String foo(String s) {
      A.class.getSimpleName().equals("A"); // Noncompliant [[sc=7;ec=42]] {{Use an "instanceof" comparison instead.}}
      new A().getClass().getSimpleName().equals("A"); // Noncompliant {{Use an "instanceof" comparison instead.}}
      new A().getClass().getName().equals("A"); // Noncompliant {{Use an "instanceof" comparison instead.}}
      String name = new A().getClass().getName();
      name.equals("A"); //False negative ?
      A.class.getSimpleName().substring(0).equals("A"); // Noncompliant {{Use an "instanceof" comparison instead.}}
      foo(A.class.getSimpleName()).equals("A");

      StackTraceElement element = getElement();
      A.class.getSimpleName().equals(element.getClassName()); // Compliant
    }

    StackTraceElement getElement() {
      return null;
    }
}
