class A {
  public static int staticValue = 0;
  private static final int CONST = staticValue++; // Compliant
  private int value;
  private static int[] staticArray;

  public void nonCompliantAssignments() {
    staticValue = value + 1; // Noncompliant
    staticValue += value; // Noncompliant
    staticValue++; // Noncompliant
    ++staticValue; // Noncompliant
    A.staticValue++; // Noncompliant
    this.staticValue--; // Noncompliant
    A myA = new A();
    myA.staticValue = 1; // Noncompliant
    myA.staticArray[0] = 1; // Noncompliant
    myA.toString();

    class InnerClass {
      InnerClass() {
        staticValue++; // Noncompliant
      }
    }
  }

  private class InnerClassWithNonCompliantAssignment {
    void foo() {
      staticValue++; // Noncompliant
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