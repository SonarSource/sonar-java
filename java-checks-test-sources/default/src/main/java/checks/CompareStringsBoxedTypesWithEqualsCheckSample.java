package checks;

import java.util.Locale;

class CompareStringsBoxedTypesWithEqualsCheckSample {
  class Boxed {
    String str1 = "blue";
    String str2 = "blue";
    Integer boxedInt1 = 1;
    Integer boxedInt2 = 2;
    int myInt1 = 1;
    int myInt2 = 2;

    void offerQuickFixes() {
      if (str1 == str2) {} // Noncompliant [[sc=16;ec=18;quickfixes=qf0]] {{Strings and Boxed types should be compared using "equals()".}}
      // fix@qf0 {{Replace with boxed comparison}}
      // edit@qf0 [[sc=23;ec=23]]{{)}}
      // edit@qf0 [[sc=15;ec=19]]{{, }}
      // edit@qf0 [[sc=11;ec=11]]{{Objects.equals(}}
      // edit@qf0 [[sl=3;sc=25;el=3;ec=25]]{{\nimport java.util.Objects;}}
      if (str1 == "blue") {} // Noncompliant [[sc=16;ec=18;quickfixes=qf1]]
      // fix@qf1 {{Replace with boxed comparison}}
      // edit@qf1 [[sc=15;ec=25]]{{)}}
      // edit@qf1 [[sc=11;ec=11]]{{"blue".equals(}}
      if ("blue" == str1) {} // Noncompliant [[sc=18;ec=20;quickfixes=qf2]]
      // fix@qf2 {{Replace with boxed comparison}}
      // edit@qf2 [[sc=25;ec=25]]{{)}}
      // edit@qf2 [[sc=17;ec=21]]{{.equals(}}
      if (str1 == "BLUE".toLowerCase(Locale.ROOT)) {} // Noncompliant [[sc=16;ec=18;quickfixes=qf3]]
      // fix@qf3 {{Replace with boxed comparison}}
      // edit@qf3 [[sc=50;ec=50]]{{)}}
      // edit@qf3 [[sc=15;ec=19]]{{, }}
      // edit@qf3 [[sc=11;ec=11]]{{Objects.equals(}}
      // edit@qf3 [[sl=3;sc=25;el=3;ec=25]]{{\nimport java.util.Objects;}}
      if ("BLUE".toLowerCase(Locale.ROOT) == str1) {} // Noncompliant [[sc=43;ec=45;quickfixes=qf4]]
      // fix@qf4 {{Replace with boxed comparison}}
      // edit@qf4 [[sc=50;ec=50]]{{)}}
      // edit@qf4 [[sc=42;ec=46]]{{, }}
      // edit@qf4 [[sc=11;ec=11]]{{Objects.equals(}}
      // edit@qf4 [[sl=3;sc=25;el=3;ec=25]]{{\nimport java.util.Objects;}}

      /* -- NEGATION --  */
      if (str1 != str2) {} // Noncompliant [[sc=16;ec=18;quickfixes=qf100]]
      // fix@qf100 {{Replace with boxed comparison}}
      // edit@qf100 [[sc=23;ec=23]]{{)}}
      // edit@qf100 [[sc=15;ec=19]]{{, }}
      // edit@qf100 [[sc=11;ec=11]]{{!Objects.equals(}}
      // edit@qf100 [[sl=3;sc=25;el=3;ec=25]]{{\nimport java.util.Objects;}}
      if (str1 != "blue") {} // Noncompliant [[sc=16;ec=18;quickfixes=qf101]]
      // fix@qf101 {{Replace with boxed comparison}}
      // edit@qf101 [[sc=15;ec=25]]{{)}}
      // edit@qf101 [[sc=11;ec=11]]{{!"blue".equals(}}
      if ("blue" != str1) {} // Noncompliant [[sc=18;ec=20;quickfixes=qf102]]
      // fix@qf102 {{Replace with boxed comparison}}
      // edit@qf102 [[sc=25;ec=25]]{{)}}
      // edit@qf102 [[sc=17;ec=21]]{{.equals(}}
      // edit@qf102 [[sc=11;ec=11]]{{!}}
      if (str1 != "BLUE".toLowerCase(Locale.ROOT)) {} // Noncompliant [[sc=16;ec=18;quickfixes=qf103]]
      // fix@qf103 {{Replace with boxed comparison}}
      // edit@qf103 [[sc=50;ec=50]]{{)}}
      // edit@qf103 [[sc=15;ec=19]]{{, }}
      // edit@qf103 [[sc=11;ec=11]]{{!Objects.equals(}}
      // edit@qf103 [[sl=3;sc=25;el=3;ec=25]]{{\nimport java.util.Objects;}}
      if ("BLUE".toLowerCase(Locale.ROOT) != str1) {} // Noncompliant [[sc=43;ec=45;quickfixes=qf104]]
      // fix@qf104 {{Replace with boxed comparison}}
      // edit@qf104 [[sc=50;ec=50]]{{)}}
      // edit@qf104 [[sc=42;ec=46]]{{, }}
      // edit@qf104 [[sc=11;ec=11]]{{!Objects.equals(}}
      // edit@qf104 [[sl=3;sc=25;el=3;ec=25]]{{\nimport java.util.Objects;}}
    }

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

    class Boxed2 {
      String[][] strArray2 = {{"blue"}};
      private void method() {
        if(strArray2 == strArray2){}
        if(strArray2[0] == strArray2[1]){}
        if(strArray2[0][0] == strArray2[1][1]){} // Noncompliant
      }
    }
  }
}

