package checks;

abstract class AbstractClass1 {
  public AbstractClass1 () { // Noncompliant[[sc=3;ec=9;secondary=3]]{{Change the visibility of this constructor to "protected".}}
    //do sth here
  }
}

abstract class AbstractClass2 {
  protected AbstractClass2 () { // Compliant
    //do sth here
  }

  private abstract class NestedAbstractClass {
    public NestedAbstractClass() { // Compliant, the class is itself private

    }
  }
}
