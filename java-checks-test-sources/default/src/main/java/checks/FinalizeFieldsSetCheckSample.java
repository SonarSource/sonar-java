package checks;

class FinalizeFieldsSetCheckSample {
  class A {
    private String myString;
    private Integer myInteger;
    private A myObject;

    @Override
    protected void finalize() {
      myString = null; // Noncompliant {{Remove this nullification of "myString".}}
//               ^^^^
      myInteger = null; // Noncompliant
      myObject = null; // Noncompliant

      String otherString;
      otherString = null; // Compliant
      myString = ""; // Compliant
      myInteger = 42; // Compliant
      myObject = new A(); // Compliant

      this.myString = null; // Noncompliant
      this.myInteger = null; // Noncompliant
      this.myObject = null; // Noncompliant
    }

    void finalize(Long value) {
      value = null; // Compliant
    }
  }

  class B {
    protected String myString;

    protected void finalize() {
      myString = null; // Noncompliant
    }
  }

  class C extends B {
    private Integer myInteger;
    private String[] myArrayOfStrings = new String[3];

    @Override
    protected void finalize() {
      myString = null; // Noncompliant
      myInteger = null; // Noncompliant

      myArrayOfStrings[0] = null; // Compliant
      myArrayOfStrings = null; // Noncompliant

      class MyInnerClass {
        private Object myObject;

        public void finalize() {
          myString = null; // Noncompliant
          myObject = null; // Noncompliant
          myInteger = null; // Noncompliant
          myArrayOfStrings = null; // Noncompliant
        }
      }
    }
  }

  class D {
    Integer myInteger;

    private boolean myMethod() {

      class MyInnerClass {
        public void finalize() {
          myInteger = null; // Noncompliant
          new D().myInteger = null; // Compliant
          D d = new D();
          d.myInteger = null; // Compliant
        }
      }

      return false;
    }

    public void finalize(boolean param) {
    }
  }

  class E extends D {
    @Override
    public void finalize(boolean param) {
    }
  }
}
