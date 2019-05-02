class A {
    String foo(String s) {
      A.class.getSimpleName().equals("A"); // Noncompliant [[sc=7;ec=42]] {{Use an "instanceof" comparison instead.}}
      new A().getClass().getSimpleName().equals("A"); // Noncompliant {{Use an "instanceof" comparison instead.}}
      new A().getClass().getName().equals("A"); // Noncompliant {{Use an "instanceof" comparison instead.}}
      String name = new A().getClass().getName();
      name.equals("A"); //False negative ?
      A.class.getSimpleName().substring(0).equals(name); // Compliant - ref: SONARJAVA-2603
      A a = new A();
      "A".equals(a.getClass().getSimpleName()); // Noncompliant
      foo(A.class.getSimpleName()).equals("A");

      StackTraceElement element = getElement();
      A.class.getSimpleName().equals(element.getClassName()); // Compliant
      Class valueClass;
      boolean b = (List.class.getName().equals(valueClass.getName())); // Noncompliant {{Use "isAssignableFrom" instead.}}
      A.class.getSimpleName().equals(foo("A")); // Compliant
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

public abstract class B {
  boolean foo(Iterable<String> argTypes) {
    for (String argType : argTypes) {
      if (Object.class.getName().equals(argType)) {
        return true;
      }
    }
    return false;
  }

  boolean bar() {
    String argType = getParameterTypeFromName("MY_PARAM");
    return Object.class.getName().equals(argType);

  }

  abstract String getParameterTypeFromName(String s);
}
