public class UselessExtendsCheck { // Compliant

  public class Class1 { // Compliant
  }

  public class Class2 extends Object { // Noncompliant
  }

}

class Class3 extends java.lang.Object { // Noncompliant
}

class Class4 extends UnknownClass { // Compliant
}
