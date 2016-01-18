public class Parent {

  protected int start;
  protected int foo;
  public Parent() {
  }

  public Parent(int start) {
    this.start = start;
  }
}

public class Child extends Parent {

  int stop;

  public Child(int start) {
    this.start = start;  // Noncompliant
    this.stop = 2;
    foo = 12; // Noncompliant
  }
}