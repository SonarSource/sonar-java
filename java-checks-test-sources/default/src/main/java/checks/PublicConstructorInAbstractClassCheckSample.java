package checks;

abstract class AbstractClass1 {
//  ^^^<
  public AbstractClass1 () { // Noncompliant {{Change the visibility of this constructor to "protected".}}
//^^^^^^
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
