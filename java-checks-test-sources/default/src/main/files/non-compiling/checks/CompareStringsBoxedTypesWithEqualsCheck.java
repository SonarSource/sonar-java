class A {
  String str1 = "blue";
  String str2 = "blue";
  Integer boxedInt1 = 1;
  Integer boxedInt2 = 2;
  int myInt1 = 1;
  int myInt2 = 2;
  private void method() {
    if (str1 == str2) {} // Noncompliant
    if (str1 == "blue") {} // Noncompliant
    if (boxedInt1 == boxedInt2) {} // Noncompliant
    if (myInt1 == myInt2) {}
    if (myInt1 == null) {}

    if (str1 != str2) {} // Noncompliant
    if (str1 != "blue") {} // Noncompliant
    if (boxedInt1 != boxedInt2) {} // Noncompliant
    if (myInt1 != myInt2) {}
    if (myInt1 != null) {}

    if (boxedInt1 == myInt1) {} // Compliant: unboxing conversion
    if (boxedInt1 != myInt1) {} // Compliant: unboxing conversion

    if (boxedInt1 > boxedInt2) {}
    if (str1 != myInt1) {}
    if (null != str1) {}

    if (this == str2) {}
    if ((this) == str2) {}

    if (str1.equals(str2)) {}
    if (boxedInt1.equals(boxedInt2)) {}
  }
}

class B {
  String[][] strArray2 = {{"blue"}};
  private void method() {
    if(strArray2 == strArray2){}
    if(strArray2[0] == strArray2[1]){}
    if(strArray2[0][0] == strArray2[1][1]){} // Noncompliant
  }
}

