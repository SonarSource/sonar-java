package checks;

import java.util.Locale;

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

  void offerQuickFixes() {
    if (str1 == str2) {} // Noncompliant [[sc=14;ec=16;quickfixes=qf0]]
    // fix@qf0 {{Replace with boxed comparison}}
    // edit@qf0 [[sc=21;ec=21]]{{)}}
    // edit@qf0 [[sc=13;ec=17]]{{, }}
    // edit@qf0 [[sc=9;ec=9]]{{java.util.Objects.equals(}}
    if (str1 == "blue") {} // Noncompliant [[sc=14;ec=16;quickfixes=qf1]]
    // fix@qf1 {{Replace with boxed comparison}}
    // edit@qf1 [[sc=23;ec=23]]{{)}}
    // edit@qf1 [[sc=13;ec=17]]{{, }}
    // edit@qf1 [[sc=9;ec=9]]{{java.util.Objects.equals(}}
    if ("blue" == str1) {} // Noncompliant [[sc=16;ec=18;quickfixes=qf2]]
    // fix@qf2 {{Replace with boxed comparison}}
    // edit@qf2 [[sc=23;ec=23]]{{)}}
    // edit@qf2 [[sc=15;ec=19]]{{, }}
    // edit@qf2 [[sc=9;ec=9]]{{java.util.Objects.equals(}}
    if (str1 == "BLUE".toLowerCase(Locale.ROOT)) {} // Noncompliant [[sc=14;ec=16;quickfixes=qf3]]
    // fix@qf3 {{Replace with boxed comparison}}
    // edit@qf3 [[sc=48;ec=48]]{{)}}
    // edit@qf3 [[sc=13;ec=17]]{{, }}
    // edit@qf3 [[sc=9;ec=9]]{{java.util.Objects.equals(}}
    if ("BLUE".toLowerCase(Locale.ROOT) == str1) {} // Noncompliant [[sc=41;ec=43;quickfixes=qf4]]
    // fix@qf4 {{Replace with boxed comparison}}
    // edit@qf4 [[sc=48;ec=48]]{{)}}
    // edit@qf4 [[sc=40;ec=44]]{{, }}
    // edit@qf4 [[sc=9;ec=9]]{{java.util.Objects.equals(}}

    /* -- NEGATION --  */
    if (str1 != str2) {} // Noncompliant [[sc=14;ec=16;quickfixes=qf100]]
    // fix@qf100 {{Replace with boxed comparison}}
    // edit@qf100 [[sc=21;ec=21]]{{)}}
    // edit@qf100 [[sc=13;ec=17]]{{, }}
    // edit@qf100 [[sc=9;ec=9]]{{!java.util.Objects.equals(}}
    if (str1 != "blue") {} // Noncompliant [[sc=14;ec=16;quickfixes=qf101]]
    // fix@qf101 {{Replace with boxed comparison}}
    // edit@qf101 [[sc=23;ec=23]]{{)}}
    // edit@qf101 [[sc=13;ec=17]]{{, }}
    // edit@qf101 [[sc=9;ec=9]]{{!java.util.Objects.equals(}}
    if ("blue" != str1) {} // Noncompliant [[sc=16;ec=18;quickfixes=qf102]]
    // fix@qf102 {{Replace with boxed comparison}}
    // edit@qf102 [[sc=23;ec=23]]{{)}}
    // edit@qf102 [[sc=15;ec=19]]{{, }}
    // edit@qf102 [[sc=9;ec=9]]{{!java.util.Objects.equals(}}
    if (str1 != "BLUE".toLowerCase(Locale.ROOT)) {} // Noncompliant [[sc=14;ec=16;quickfixes=qf103]]
    // fix@qf103 {{Replace with boxed comparison}}
    // edit@qf103 [[sc=48;ec=48]]{{)}}
    // edit@qf103 [[sc=13;ec=17]]{{, }}
    // edit@qf103 [[sc=9;ec=9]]{{!java.util.Objects.equals(}}
    if ("BLUE".toLowerCase(Locale.ROOT) != str1) {} // Noncompliant [[sc=41;ec=43;quickfixes=qf104]]
    // fix@qf104 {{Replace with boxed comparison}}
    // edit@qf104 [[sc=48;ec=48]]{{)}}
    // edit@qf104 [[sc=40;ec=44]]{{, }}
    // edit@qf104 [[sc=9;ec=9]]{{!java.util.Objects.equals(}}
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

