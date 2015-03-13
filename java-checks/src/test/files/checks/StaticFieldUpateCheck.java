abstract class A {
  private static final int CONST = 0;
  private static int staticValue = 0;
  private int value;
  private static int[] staticArray;

  public void nonCompliantAssignments() {
    staticValue = value + 1; // Noncompliant
    staticValue += value; // Noncompliant
    staticValue++; // Noncompliant
    ++staticValue; // Noncompliant
    A.staticValue++; // Noncompliant
    this.staticValue--; // Noncompliant
    A myA = new B();
    myA.staticValue = 1; // Noncompliant
    staticArray[0] = 1; // Noncompliant

    class InnerClass3 {
      InnerClass3() {
        staticValue++; // Noncompliant
      }
    }
  }

  private class InnerClassWithNonCompliantAssignment {
    InnerClassWithNonCompliantAssignment() {
      staticValue++; // Noncompliant
    }
  }

  public static void compliantStaticMethod() {
    staticValue++; // Compliant
  }

  public void compliantCode() {
    value++; // Compliant
    setValue(-staticValue); // Compliant

    A myA = new B();
    myA.value = value++; // Compliant
    
    getValue()++; // Compliant
  }

  private class CompliantInnerClass {
    private int value;

    CompliantInnerClass() {
      value = 1; // Compliant
    }
  }

  private interface InnerInterface {
  }

  public abstract void bar();

  public void setValue(int a) {
    value = a;
  }

  public void getValue() {
    return value;
  }
}

class B extends A {

  @Override
  public void bar() {
  }
}

static class C {
  private static int value;
  
  vodi foo() {
    value++; // Compliant
  }
}
