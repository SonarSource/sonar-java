
class Foo {
  private Helper helper = null;

  public Helper classicCase() {
    if (helper == null)
      synchronized (this) { // Noncompliant [[sc=7;ec=19;secondary=6,8]] {{Remove this dangerous instance of double-checked locking.}}
        if (helper == null)
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
      synchronized (this) { // Noncompliant [[sc=7;ec=19;secondary=25,27]] {{Remove this dangerous instance of double-checked locking.}}
        if (helper == null)
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper memberSelectCondition2() {
    if (helper == null)
      synchronized (this) { // Noncompliant [[sc=7;ec=19;secondary=34,36]] {{Remove this dangerous instance of double-checked locking.}}
        if (this.helper == null)
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper memberSelectCondition3() {
    if (this.helper == null)
      synchronized (this) { // Noncompliant [[sc=7;ec=19;secondary=43,45]] {{Remove this dangerous instance of double-checked locking.}}
        if (this.helper == null)
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper invertedConditions() {
    if (null == this.helper)
      synchronized (this) { // Noncompliant [[sc=7;ec=19;secondary=52,54]] {{Remove this dangerous instance of double-checked locking.}}
        if (null == helper)
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper intializationViaMemberSelect2() {
    if (helper == null)
      synchronized (this) { // Noncompliant [[sc=7;ec=19;secondary=61,63]] {{Remove this dangerous instance of double-checked locking.}}
        if (helper == null)
          this.helper = new Helper();
      }
    return helper;
  }

  private AbstractHelper abstractHelper;
  private HelperInterface helperInterface;

  public HelperInterface interfaceHelper() {
    if (helperInterface == null)
      synchronized (this) { // Noncompliant [[sc=7;ec=19;secondary=73,75]] {{Remove this dangerous instance of double-checked locking.}}
        if (helperInterface == null)
          this.helperInterface = new Helper();
      }
    return helperInterface;
  }

  public AbstractHelper abstractHelper() {
    if (abstractHelper == null)
      synchronized (this) { // Noncompliant [[sc=7;ec=19;secondary=82,84]] {{Remove this dangerous instance of double-checked locking.}}
        if (abstractHelper == null)
          this.abstractHelper = new Helper();
      }
    return abstractHelper;
  }

  private UnknownType unknownType;

  public UnknownType unknownType() {
    if (unknownType == null) {
      synchronized (Helper.class) { // Noncompliant [[sc=7;ec=19;secondary=93,95]] {{Remove this dangerous instance of double-checked locking.}}
        if (unknownType == null) {
          this.unknownType = new UnknownType();
        }
      }
    }
    return unknownType;
  }
}

// after java 5 volatile keyword will guarantee correct read/write ordering with memory barriers
class VolatileFoo {
  private volatile Helper helper = null;

  public Helper classicCase() {
    if (helper == null)
      synchronized (this) { // Compliant because field is volatile
        if (helper == null)
          helper = new Helper();
      }
    return helper;
  }
}

class NestedIfs {
  private Helper helper = null;

  public Helper unrelatedNestedIfs() {
    if (null == helper) {
      if (sunIsUp) {
        doSomething();
      }
      synchronized (this) { // Noncompliant [[sc=7;ec=19;secondary=122,129]] {{Remove this dangerous instance of double-checked locking.}}
        if (sunIsDown) {
          doSomethingElse();
          if (null == helper)
            helper = new Helper();
        }
      }
    }
    return helper;
  }

  static class Helper {
    int field;
  }
}

class Compliant {

  private Helper helper = null;
  private int primitiveField = 0;

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
        if (primitiveField == null) {
          primitiveField = 42;
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

  public void synchronizedWithTwoIfs() {
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

}

class StringResource {
  final String field;
}
