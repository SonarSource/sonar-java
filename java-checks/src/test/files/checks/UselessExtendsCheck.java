public class UselessExtendsCheck { // Compliant

  public class Class1 implements I1 { // Compliant
  }

  public class Class2
  extends Object { // Noncompliant [[sc=11;ec=17]] {{"Object" should not be explicitly extended.}}
  }

  public class Class3
  implements I1,
  I1 { // Noncompliant [[sc=3;ec=5]] {{"I1" is listed multiple times.}}
  }

  public class Class4
  implements I1, // Noncompliant [[sc=14;ec=16]] {{"I3" is a "I1" so "I1" can be removed from the extension list.}}
  I2, // Noncompliant {{"I3" is a "I2" so "I2" can be removed from the extension list.}}
  I3 {
  }

}

class Class3
extends java.lang.Object // Noncompliant {{"Object" should not be explicitly extended.}}
implements I2 {
}

class Class4 extends UnknownClass1 implements UnknownInterface1, UnknownInterface2 { // Compliant
}

class Class5 extends UnknownClass1 implements UnknownInterface, // Noncompliant
UnknownInterface, // Noncompliant
java.io.UnknownInterface, // Noncompliant
java.io.UnknownInterface, // Noncompliant [[sc=1;ec=25]] {{"UnknownInterface" is listed multiple times.}}
UnknownParametrized<Unknown>, // Noncompliant
UnknownParametrized<Unknown> { // Noncompliant
}

interface I1 {
}

interface I2 extends UnknownInterface {
}

interface I3 extends I1, I2, UnknownInterface {
}
