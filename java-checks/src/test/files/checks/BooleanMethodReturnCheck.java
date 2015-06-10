import org.apache.commons.lang.BooleanUtils;

class A {
  public Boolean myMethod() {
    return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
  }

  public Boolean myOtherMethod() {
    return Boolean.TRUE; // Compliant
  }
}

class B {
  private class Boolean {
  }

  public Boolean myMethod() {
    return null; // Compliant
  }
  
  public java.lang.Boolean myOtherMethod() {
    private class C {
      private java.lang.Boolean myInnerMethod() {
        return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
      }
      private C foo() {
        return null; // Compliant
      }
    }
    return null; // Noncompliant {{Null is returned but a "Boolean" is expected.}}
  }
}
