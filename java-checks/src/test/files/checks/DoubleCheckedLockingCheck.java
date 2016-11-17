
class Foo {
  private Helper helper = null;

  public Helper classicCase() {
    if (helper == null) // Noncompliant [[sc=5;ec=7;secondary=8]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (helper == null)
          helper = new Helper();
      }
    return helper;
  }

  public Helper fieldIsNotActuallyInitializedHere() {
    if (helper == null) // Compliant - field is not initialized in this method actually
      synchronized (this) {
        if (helper == null) {
          System.out.println("haha!"); // Nelson
        }
      }
    return helper;
  }

  public Helper memberSelectCondition() {
    if (this.helper == null) // Noncompliant [[sc=5;ec=7;secondary=27]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (helper == null)
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper memberSelectCondition2() {
    if (helper == null) // Noncompliant [[sc=5;ec=7;secondary=36]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (this.helper == null)
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper memberSelectCondition3() {
    if (this.helper == null) // Noncompliant [[sc=5;ec=7;secondary=45]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (this.helper == null)
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper invertedConditions() {
    if (null == this.helper) // Noncompliant [[sc=5;ec=7;secondary=54]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (null == helper)
          this.helper = new Helper();
      }
    return helper;
  }

  public Helper intializationViaMemberSelect2() {
    if (helper == null) // Noncompliant [[sc=5;ec=7;secondary=63]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (helper == null)
          this.helper = new Helper();
      }
    return helper;
  }

  private AbstractHelper abstractHelper;
  private HelperInterface helperInterface;

  public HelperInterface interfaceHelper() {
    if (helperInterface == null) // Noncompliant [[sc=5;ec=7;secondary=75]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (helperInterface == null)
          this.helperInterface = new Helper();
      }
    return helperInterface;
  }

  public AbstractHelper abstractHelper() {
    if (abstractHelper == null) // Noncompliant [[sc=5;ec=7;secondary=84]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (this) {
        if (abstractHelper == null)
          this.abstractHelper = new Helper();
      }
    return abstractHelper;
  }

  private UnknownType unknownType;

  public UnknownType unknownType() {
    if (unknownType == null) { // Noncompliant [[sc=5;ec=7;secondary=95]] {{Remove this dangerous instance of double-checked locking.}}
      synchronized (Helper.class) {
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
    if (helper == null) // Compliant because field is volatile
      synchronized (this) {
        if (helper == null)
          helper = new Helper();
      }
    return helper;
  }
}

class ImmutableFoo {
  private ImmutableHelper helper = null;

  public ImmutableHelper classicCase() {
    if (helper == null) // Compliant since Helper is effectively immutable
      synchronized (this) {
        if (helper == null)
          helper = new ImmutableHelper();
      }
    return helper;
  }
}

class NestedIfs {
  private Helper helper = null;

  public Helper unrelatedNestedIfs() {
    if (null == helper) { // Noncompliant [[sc=5;ec=7;secondary=142]] {{Remove this dangerous instance of double-checked locking.}}
      if (sunIsUp) {
        doSomething();
      }
      synchronized (this) {
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

class Helper {

  int mutableField;

}

class ImmutableHelper {
  final int field;
}

interface HelperInterface {

}

abstract class AbstractHelper {

}

class Compliant {

  private Helper helper = null;
  private int primitiveField = 0;

  public void notTheSameTest() {
    if (sunIsUp) {
      synchronized (this) {
        if (helper == null)
          helper = new Helper();
      }
    }
  }

  public void notTheField() {
    Helper helper = null;
    if (helper == null) {
      synchronized (this) {
        if (helper == null) {
          helper = new Helper(); // Compliant
        }
      }
    }
  }

  public void primitiveField() {
    if (primitiveField == null) {
      synchronized (this) {
        if (primitiveField == null) {
          primitiveField = 42; // Compliant
        }
      }
    }
  }

  public void otherField() {
    if (helper == null) {
      synchronized (this) {
        if (primitiveField == null) {
          primitiveField = 42; // Compliant
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

  public void notTheField() {
    Object a;
    if (a == a) {
      synchronized (this) {
        if (null == null) {
          a = new Object(); // Compliant
        }
      }
    }
  }

  public void notTheField() {
    Object a;
    if (a == a) {
      synchronized (this) {
        if (a == null) {
          a = new Object(); // Compliant
        }
      }
    }
  }

  public void notAVariable() {
    if (MyClass.class == null) {
      synchronized (this) { // Compliant
        if (MyClass.class == null) {

        }
      }
    }
  }

  class StringResource {
    final String field;
  }


}
