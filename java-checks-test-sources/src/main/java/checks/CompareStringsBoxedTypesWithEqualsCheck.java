package checks;

class Boxed {
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
    if (boxedInt1 == null) {}

    if (str1 != str2) {} // Noncompliant
    if (str1 != "blue") {} // Noncompliant
    if (boxedInt1 != boxedInt2) {} // Noncompliant
    if (myInt1 != myInt2) {}
    if (boxedInt1 != null) {}

    if (boxedInt1 == myInt1) {} // Compliant: unboxing conversion
    if (boxedInt1 != myInt1) {} // Compliant: unboxing conversion

    if (boxedInt1 > boxedInt2) {}
    if (null != str1) {}

    if (str1.equals(str2)) {}
    if (boxedInt1.equals(boxedInt2)) {}
    
    Boolean b = null;
    Boolean b1 = true;
    Boolean b2 = false;
    Boolean b3 = Boolean.TRUE;
    Boolean b4 = Boolean.FALSE;
    Boolean b5 = new Boolean(true);
    Boolean b6 = new Boolean(false);
    if (b == Boolean.FALSE) {} // Compliant
    if (b1 == Boolean.TRUE) {} // Compliant
    if (b2 == Boolean.TRUE) {} // Compliant
    if (b3 == Boolean.FALSE) {} // Compliant
    if (b4 == Boolean.TRUE) {} // Compliant
    if (Boolean.TRUE == b4) {} // Compliant
    if (b5 == Boolean.TRUE) {} // FN
    if (b5 == b6) {} // Noncompliant
  }
}

class Boxed2 {
  String[][] strArray2 = {{"blue"}};
  private void method() {
    if(strArray2 == strArray2){}
    if(strArray2[0] == strArray2[1]){}
    if(strArray2[0][0] == strArray2[1][1]){} // Noncompliant
  }
}

