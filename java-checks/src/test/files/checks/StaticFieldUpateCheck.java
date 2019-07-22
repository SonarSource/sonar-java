class A {
  public static int staticValue = 0;
  private static final int CONST = staticValue++; // Compliant
  private int value;
  private static int[] staticArray;

  public void nonCompliantAssignments() {
    staticValue = value + 1; // Noncompliant [[sc=5;ec=16]] {{Make the enclosing method "static" or remove this set.}}
    staticValue += value; // Noncompliant {{Make the enclosing method "static" or remove this set.}}
    staticValue++; // Noncompliant {{Make the enclosing method "static" or remove this set.}}
    ++staticValue; // Noncompliant {{Make the enclosing method "static" or remove this set.}}
    A.staticValue++; // Noncompliant {{Make the enclosing method "static" or remove this set.}}
    this.staticValue--; // Noncompliant {{Make the enclosing method "static" or remove this set.}}
    A myA = new A();
    myA.staticValue = 1; // Noncompliant {{Make the enclosing method "static" or remove this set.}}
    myA.staticArray[0] = 1; // Noncompliant {{Make the enclosing method "static" or remove this set.}}
    myA.toString();

    class InnerClass {
      InnerClass() {
        staticValue++; // Noncompliant {{Make the enclosing method "static" or remove this set.}}
      }
    }
  }

  private class InnerClassWithNonCompliantAssignment {
    void foo() {
      staticValue++; // Noncompliant {{Make the enclosing method "static" or remove this set.}}
    }
  }

  public synchronized void synchronizedMethod() {
    staticValue++; // Compliant
  }

  public static void compliantStaticMethod() {
    staticValue++; // Compliant
  }

  public void compliantCode() {
    value++; // Compliant

    A myA = new A();
    myA.value = value++; // Compliant

    int variable;
    variable = staticValue; // Compliant

    synchronized (new Object()) {
      staticValue++; // Compliant
      staticValue = value + 1; // Compliant
    }

    MyUnknownClass.staticField = value; // Compliant
  }
}

class B {
  private static int value;

  private static Comparable<Object> myComparator;

  static {
    myComparator = new Comparable<Object>() { // Compliant

      @Override
      public int compareTo(Object o) {
        value = 0; // Compliant
        return 0;
      }
    };
  }

  private synchronized Comparable<Object> foo1() {
    return new Comparable<Object>() {
      @Override
      public int compareTo(Object o) {
        value = 0; // FN: "value" may not be assigned inside a static method
        return 0;
      }
    };
  }

  private static synchronized Consumer<Object> foo2() {
    return s -> {
      value = 0; // FN: we are not guaranteed to be inside a static method; the rule check if any parent is synchronized.
    };
  }

  private void foo() {
    synchronized (new Object()) {
      Comparable<Object> cmp = new Comparable<Object>() {

        @Override
        public int compareTo(Object o) {
          value = 0; // Compliant
          return 0;
        }
      };
    }

  }
}
