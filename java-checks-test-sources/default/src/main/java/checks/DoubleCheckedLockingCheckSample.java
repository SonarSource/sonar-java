package checks;
class DoubleCheckedLockingCheckSample {
  private Helper helper = null;

  public Helper classicCase() {
    if (helper == null)
//      ^^^^^^^^^^^^^^>
      synchronized (this) { // Noncompliant {{Remove this dangerous instance of double-checked locking.}}
//    ^^^^^^^^^^^^
        if (helper == null)
//          ^^^^^^^^^^^^^^<
          helper = new Helper();
      }
    return helper;
  }

  public Helper fieldIsNotActuallyInitializedHere() {
    if (helper == null)
      synchronized (this) { // Compliant - field is not initialized in this method actually
        if (helper == null) {
          System.out.println("haha!"); // Nelson
        }
      }
    return helper;
  }

  public Helper memberSelectCondition() {
    if (this.helper == null)
//      ^^^^^^^^^^^^^^^^^^^>
      synchronized (this) { // Noncompliant {{Remove this dangerous instance of double-checked locking.}}
//    ^^^^^^^^^^^^
        if (helper == null)
//          ^^^^^^^^^^^^^^<
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper memberSelectCondition2() {
    if (helper == null)
//      ^^^^^^^^^^^^^^>
      synchronized (this) { // Noncompliant {{Remove this dangerous instance of double-checked locking.}}
//    ^^^^^^^^^^^^
        if (this.helper == null)
//          ^^^^^^^^^^^^^^^^^^^<
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper memberSelectCondition3() {
    if (this.helper == null)
//      ^^^^^^^^^^^^^^^^^^^>
      synchronized (this) { // Noncompliant {{Remove this dangerous instance of double-checked locking.}}
//    ^^^^^^^^^^^^
        if (this.helper == null)
//          ^^^^^^^^^^^^^^^^^^^<
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper invertedConditions() {
    if (null == this.helper)
//      ^^^^^^^^^^^^^^^^^^^>
      synchronized (this) { // Noncompliant {{Remove this dangerous instance of double-checked locking.}}
//    ^^^^^^^^^^^^
        if (null == helper)
//          ^^^^^^^^^^^^^^<
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper intializationViaMemberSelect2() {
    if (helper == null)
//      ^^^^^^^^^^^^^^>
      synchronized (this) { // Noncompliant {{Remove this dangerous instance of double-checked locking.}}
//    ^^^^^^^^^^^^
        if (helper == null)
//          ^^^^^^^^^^^^^^<
          this.helper = new Helper();
      }
    return helper;
  }

  private AbstractHelper abstractHelper;
  private HelperInterface helperInterface;

  public HelperInterface interfaceHelper() {
    if (helperInterface == null)
//      ^^^^^^^^^^^^^^^^^^^^^^^>
      synchronized (this) { // Noncompliant {{Remove this dangerous instance of double-checked locking.}}
//    ^^^^^^^^^^^^
        if (helperInterface == null)
//          ^^^^^^^^^^^^^^^^^^^^^^^<
          this.helperInterface = new Helper();
      }
    return helperInterface;
  }

  public AbstractHelper abstractHelper() {
    if (abstractHelper == null)
//      ^^^^^^^^^^^^^^^^^^^^^^>
      synchronized (this) { // Noncompliant {{Remove this dangerous instance of double-checked locking.}}
//    ^^^^^^^^^^^^
        if (abstractHelper == null)
//          ^^^^^^^^^^^^^^^^^^^^^^<
          this.abstractHelper = new Helper();
      }
    return abstractHelper;
  }

  static class Helper extends AbstractHelper implements HelperInterface {  int field; }
  abstract static class AbstractHelper { int field; }
  interface HelperInterface { }
}

// after java 5 volatile keyword will guarantee correct read/write ordering with memory barriers
class DoubleCheckedLockingCheckSampleVolatile {
  private volatile Helper helper = null;

  public Helper classicCase() {
    if (helper == null)
      synchronized (this) { // Compliant because field is volatile
        if (helper == null)
          helper = new Helper();
      }
    return helper;
  }

  static class Helper { }
}

class DoubleCheckedLockingCheckSampleNestedIfs {
  private Helper helper = null;
  private boolean sunIsUp, sunIsDown;

  public Helper unrelatedNestedIfs() {
    if (null == helper) {
//      ^^^^^^^^^^^^^^>
      if (sunIsUp) {
        doSomething();
      }
      synchronized (this) { // Noncompliant {{Remove this dangerous instance of double-checked locking.}}
//    ^^^^^^^^^^^^
        if (sunIsDown) {
          doSomethingElse();
          if (null == helper)
//            ^^^^^^^^^^^^^^<
            helper = new Helper();
        }
      }
    }
    return helper;
  }

  static class Helper {
    int field;
  }

  void doSomething() { }
  void doSomethingElse() { }
}

class DoubleCheckedLockingCheckSampleCompliant {

  private Helper helper = null;
  private int primitiveField = 0;
  private Integer field;
  private boolean sunIsUp;

  public void notTheSameTest() {
    if (sunIsUp) {
      synchronized (this) { // Compliant
        if (helper == null)
          helper = new Helper();
      }
    }
  }

  public void notTheField() {
    Helper helper = null;
    if (helper == null) {
      synchronized (this) { // Compliant
        if (helper == null) {
          helper = new Helper();
        }
      }
    }
  }

  public void notTheField2() {
    Helper helper = null;
    if (null == null) {
      synchronized (this) { // Compliant
        if (helper == helper) {
          helper = new Helper();
        }
      }
    }
  }


  public void otherField() {
    if (helper == null) {
      synchronized (this) { // Compliant
        if (field == null) {
          field = 42;
        }
      }
    }
  }

  public void otherField2() {
    if (helper == null) {
      synchronized (this) {
        if (helper == null) {
          primitiveField = 42; // Compliant
        }
      }
    }
  }

  public Helper synchronizedWithTwoIfs() {
    synchronized (this) { // Compliant
      if (helper == null)
        if (helper == null)
          helper = new Helper();
    }
    return helper;
  }

  public void notAVariable() {
    if (MyClass.class == null) {
      synchronized (this) { // Compliant
        if (MyClass.class == null) {

        }
      }
    }
  }

  public synchronized Helper synchronizedClassicCase() {
    if (helper == null)
      synchronized (this) { // Compliant
        if (helper == null)
          helper = new Helper();
      }
    return helper;
  }

  static class Helper { }
}

class DoubleCheckedLockingCheckSampleStringResource {
  final String field = "";
}
