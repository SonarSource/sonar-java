import java.io.Externalizable;

class A implements Externalizable { // Noncompliant [[sc=7;ec=8]] {{Add a no-arg constructor to this class.}}
  public A(String color, int weight) {}
}

class B implements Externalizable { // Compliant
  public B() {}
  public B(String color, int weight) {}
}

class C implements Externalizable { // Compliant - default constructor
  void foo() {
    Externalizable e = new Externalizable() {}; // Compliant
  }
}

interface I extends Externalizable {}

class D implements I {} // Compliant

class E implements I { // Noncompliant [[sc=7;ec=8]] {{Add a no-arg constructor to this class.}}
  public E(String color, int weight) {}
}

class F  {  // Compliant
  public F(String color, int weight) {}
}
