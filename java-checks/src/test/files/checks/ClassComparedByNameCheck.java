class A {
    String foo(String s) {
      A.class.getSimpleName().equals("A"); // Noncompliant [[sc=7;ec=42]] {{Use an "instanceof" comparison instead.}}
      new A().getClass().getSimpleName().equals("A"); // Noncompliant {{Use an "instanceof" comparison instead.}}
      new A().getClass().getName().equals("A"); // Noncompliant {{Use an "instanceof" comparison instead.}}
      String name = new A().getClass().getName();
      name.equals("A"); //False negative ?
      A.class.getSimpleName().substring(0).equals(name); // Noncompliant {{Use an "instanceof" comparison instead.}}
      foo(A.class.getSimpleName()).equals("A");

      StackTraceElement element = getElement();
      A.class.getSimpleName().equals(element.getClassName()); // Compliant
      Class valueClass;
      if (List.class.getName().equals(valueClass.getName())); // Noncompliant {{Use "isAssignableFrom" instead.}}
    }

    StackTraceElement getElement() {
      return null;
    }

  Object foo(String realType) {
    if (Integer.class.getName().equals(realType)) { // compliant
      return null;
    }
    return null;
  }
}
