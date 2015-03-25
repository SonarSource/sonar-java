public class UselessExtendsCheck { // Compliant

  public class Class1 implements I1 { // Compliant
  }

  public class Class2
  extends Object // Noncompliant
  implements I1,
  I1, // Noncompliant
  I2 { // Noncompliant
  }

}

class Class3 extends java.lang.Object implements I2 { // Noncompliant
}

class Class4 extends UnknownClass implements UnknownInterface { // Compliant
}

interface I1 {
}

interface I2 extends I1 {
}