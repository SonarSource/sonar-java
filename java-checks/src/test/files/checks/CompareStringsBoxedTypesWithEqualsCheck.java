class A {
  String str1 = "blue";
  String str2 = "blue";
  Integer boxedInt1 = 1;
  Integer boxedInt2 = 2;
  int myInt1 = 1;
  int myInt2 = 2;
  private void method() {
    if (str1 == str2) {} // Noncompliant
    if (boxedInt1 == boxedInt2) {} // Noncompliant
    if (myInt1 == myInt2) {}
    if (myInt1 == null) {}

    if (str1 != str2) {} // Noncompliant
    if (boxedInt1 != boxedInt2) {} // Noncompliant
    if (myInt1 != myInt2) {}
    if (myInt1 != null) {}

    if (boxedInt1 == myInt1) {} // Compliant: unboxing conversion
    if (boxedInt1 != myInt1) {} // Compliant: unboxing conversion

    if (boxedInt1 > boxedInt2) {}
    if (str1 != myInt1) {}
    if (null != str1) {}
  }
}
