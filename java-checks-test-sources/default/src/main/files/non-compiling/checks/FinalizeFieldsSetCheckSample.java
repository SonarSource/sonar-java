package checks;

class FinalizeFieldsSetCheckSample {
  Integer myInteger;

  private boolean myMethod() {

    class MyInnerClass {
      public void finalize() {
        myInteger = null; // Noncompliant
        new D().myInteger = null; // Compliant
        D d = new D();
        d.myInteger = null; // Compliant
        unknownVariable = null; // Compliant
      }
    }

    return false;
  }

  public void finalize(boolean param) {
  }
}
