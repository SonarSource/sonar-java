public class UselessExtendsCheck { // Compliant

  public class Class1 implements I1 { // Compliant
  }

  public class Class2
  extends Object { // Noncompliant
  }

  public class Class3
  implements I1,
  I1 { // Noncompliant
  }

  public class Class4
  implements I1, // Noncompliant
  I2, // Noncompliant
  I3 {
  }

}

class Class3
extends java.lang.Object // Noncompliant
implements I2 {
}

class Class4 extends UnknownClass1 implements UnknownInterface1, UnknownInterface2 { // Compliant
}

class Class5 extends UnknownClass1 implements UnknownInterface,
UnknownInterface, // Noncompliant
java.io.UnknownInterface,
java.io.UnknownInterface, // Noncompliant
UnknownParametrized<Unknown>,
UnknownParametrized<Unknown> { // Noncompliant
}

interface I1 {
}

interface I2 extends UnknownInterface {
}

interface I3 extends I1, I2, UnknownInterface {
}
