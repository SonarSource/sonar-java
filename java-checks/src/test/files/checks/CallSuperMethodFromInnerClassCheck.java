public class Parent {
  public void foo() {  }
}

public class Outer {

  public void foo() {  }

  public class Inner extends Parent {

    public void doTheThing() {
      foo();  // Noncompliant; was Outer.this.foo() intended instead?
      super.foo(); //Compliant: unambiguous
      Outer.this.foo(); //Compliant: unambiguous
      bar();//Compliant : symbol is unresolved.
      // ...
      doTheThing();//Compliant not from super type
    }
  }
}