class A {
  String str1 = "blue";
  String str2 = "blue";
  String[] strArray1 = {"blue"};
  String[] strArray2 = {"blue"};
  private void method() {
    if (this == str2) {} // Compliant
    if ((this) == str2) {} // Compliant
    if (str1 == str2) {} // Compliant (handled by S4973)
    if(str1 == "green") {} // Compliant (handled by S4973)
    if (str1.equals(str2)) {}
    if(strArray1 == strArray2) {}
    if(null == str1){ }
    if(str1 == null){ }
  }
}

class B {
  String[][] strArray2 = {{"blue"}};
  private void method() {
    if(strArray2 == strArray2){}
    if(strArray2[0] == strArray2[1]){}
    if(strArray2[0][0] == strArray2[1][1]){} // Compliant (handled by S4973)
    byte[] bits;
    if(bits [0] == bits [1]) {}
    if(Foo.FOO == Foo.BAR) {}
  }
  enum Foo {
    FOO,
    BAR;
  }
}
class C{
  String str1 = "blue";
  String str2 = "blue";
  String[] strArray1 = {"blue"};
  String[] strArray2 = {"blue"};
  private void method() {
    if (str1 != str2) {} // Compliant (handled by S4973)
    if(str1 != "green") {} // Compliant (handled by S4973)
    if (str1.equals(str2)) {}
    if(strArray1 != strArray2) {}
    if(null != str1){ }
    if(str1 != null){ }
  }

  public boolean equals(Object obj) {
    if (str1 != str2) {}
    if (str1 == str2) {}
    return super.equals(obj);
  }
  void meth(){
    Integer i;
    char c = 'c';
    if(i == (byte)0){}
    if(i == c){}
    if(i == (short)1){}
    if(i == 1){}
    if(i == 1L){}
    if(i == 1F){}
    if(i == 1D){}
  }

  Class<?> c1;
  Class<?> c2;
  void foo() {
    if(c1 == c2);
  }
  enum MyEnum{
    Value;
  }
  class MyClass<T> {
    void foo() {
      if(myMethod(this) == MyEnum.Value) {} // Compliant
      java.util.List<MyEnum> myEnumList;
      if(myEnumList.get(0) == MyEnum.Value) {} // Compliant
      java.util.List list;
      if(list.get(0) == MyEnum.Value) {} // Compliant
      java.util.List<MyClass> myClassList;
      if(myClassList.get(0) == MyEnum.Value) {} // Noncompliant
      if (myClassList.get(0) == this) {} // Compliant
    }

    T myMethod(MyClass<T> instance){}

    @Override
    public boolean equals(Object obj) {
      return true;
    }
  }
}

class NoIssueWhenNoEqualsOverride {

  private void foo(ClassWithEquals a1, ClassWithEquals a2, ClassWithoutEquals b1, ClassWithoutEquals b2) {
    if (a1 == b1) {} // Compliant
    if (b1 != a1) {} // Compliant

    if (a1 != a2) {} // Noncompliant
    if (b1 == b2) {} // Compliant
  }

  private static class CommonSuperClass {}
  private static class ClassWithoutEquals extends CommonSuperClass {
    boolean equals;
    boolean equals(Object o1, Object o2) {
      return o1 == o2; /// Compliant
    }
    boolean equals(int i) {
      return i > 42;
    }
  }
  private static class ClassWithEquals extends CommonSuperClass {
    @Override
    public boolean equals(Object obj) { return true; }
  }

  private void foo(ClassWithEquals2 a1, ClassWithEquals2 a2, ClassWithoutEquals2 b1, ClassWithoutEquals2 b2) {
    if (a1 == b1) {} // Noncompliant
    if (b1 != a1) {} // Noncompliant

    if (a1 != a2) {} // Noncompliant
    if (b1 == b2) {} // Noncompliant
  }

  private static class CommonSuperClassWithEquals {
    @Override
    public boolean equals(Object obj) { return true; }
  }
  private static class ClassWithoutEquals2 extends CommonSuperClassWithEquals {}
  private static class ClassWithEquals2 extends CommonSuperClassWithEquals {
    @Override
    public boolean equals(Object obj) { return true; }
  }

  private void foo(ClassWithEquals2 a1, ClassWithoutEquals3 b1, ClassWithoutEquals3 b2) {
    if (a1 == b1) {} // Compliant
    if (b1 != a1) {} // Compliant

    if (b1 == b2) {} // Compliant - no idea if there is an equals method in the hierarchy
  }

  private static class ClassWithoutEquals3 extends UnknownSuperClass {}
}

class FF {


  public static final FF A = new FF();
  public static final FF B = new FF();
  private final FF C = new FF();
  private final FF D = new FF();

  enum Unrelated { UNRELATED }

  void testPublicStaticFinal() {
    if (A == B) {} // Compliant
    if (this.A == new FF()) {} // Compliant
    if (new FF() == A) {} // Compliant
    if (A == Unrelated.UNRELATED) {} // Noncompliant
  }

  void testPrivateFinal() {
    if (C == A) {} // Compliant
    if (A == C) {} // Compliant
    if (C == D) {} // Compliant
    if (D == C) {} // Compliant
    if (this.C == new FF()) {} // Compliant
    if (new FF() == C) {} // Compliant
  }

  public boolean equals(Object o) {
    return this == o;
  }


}
