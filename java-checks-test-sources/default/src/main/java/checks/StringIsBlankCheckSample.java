package checks;

public class StringIsBlankCheckSample {
  public boolean sample(String s, StringBuilder sb) {
    boolean b;

    // test `trim().length() == 0` and equivalent code

    b = s.trim().length() == 0; // Noncompliant {{Use "isBlank()" to check whether a "String" is blank or not.}} [[quickfixes=qf1]]
    // fix@qf1 {{Replace with "isBlank()"}}
    // edit@qf1 [[sc=11;ec=31]]{{isBlank()}}

    b = s.trim().length() <= 0; // Noncompliant [[quickfixes=qf2]]
    // fix@qf2 {{Replace with "isBlank()"}}
    // edit@qf2 [[sc=11;ec=31]]{{isBlank()}}

    b = s.trim().length() < 1; // Noncompliant [[quickfixes=qf3]]
    // fix@qf3 {{Replace with "isBlank()"}}
    // edit@qf3 [[sc=11;ec=30]]{{isBlank()}}

    // test `trim().length() != 0` and equivalent code

    b = s.trim().length() != 0; // Noncompliant [[quickfixes=qf4]]
    // fix@qf4 {{Replace with "isBlank()"}}
    // edit@qf4 [[sc=9;ec=9]]{{!}}
    // edit@qf4 [[sc=11;ec=31]]{{isBlank()}}

    b = s.trim().length() > 0; // Noncompliant [[quickfixes=qf5]]
    // fix@qf5 {{Replace with "isBlank()"}}
    // edit@qf5 [[sc=9;ec=9]]{{!}}
    // edit@qf5 [[sc=11;ec=30]]{{isBlank()}}

    b = s.trim().length() >= 1; // Noncompliant [[quickfixes=qf6]]
    // fix@qf6 {{Replace with "isBlank()"}}
    // edit@qf6 [[sc=9;ec=9]]{{!}}
    // edit@qf6 [[sc=11;ec=31]]{{isBlank()}}

    // reversed order

    b = 0 == s.trim().length(); // Noncompliant [[quickfixes=qf7]]
    // fix@qf7 {{Replace with "isBlank()"}}
    // edit@qf7 [[sc=9;ec=14]]{{}}
    // edit@qf7 [[sc=16;ec=31]]{{isBlank()}}

    b = 0 != s.trim().length(); // Noncompliant [[quickfixes=qf8]]
    // fix@qf8 {{Replace with "isBlank()"}}
    // edit@qf8 [[sc=9;ec=14]]{{!}}
    // edit@qf8 [[sc=16;ec=31]]{{isBlank()}}

    // extra parentheses

    b = (s.trim().length()) == 0; // Noncompliant [[quickfixes=qf9]]
    // fix@qf9 {{Replace with "isBlank()"}}
    // edit@qf9 [[sc=9;ec=10]]{{}}
    // edit@qf9 [[sc=12;ec=33]]{{isBlank()}}

    // the `trim()` call is not directly chained, so only emptiness can be simplified
    b = (s.trim()).length() == 0; // Noncompliant {{Use "isEmpty()" to check whether a "String" is empty or not.}} [[quickfixes=qf10]]
    // fix@qf10 {{Replace with "isEmpty()"}}
    // edit@qf10 [[sc=20;ec=33]]{{isEmpty()}}

    // chained method calls

    b = s.toUpperCase().trim().length() == 0; // Noncompliant [[quickfixes=qf11]]
    // fix@qf11 {{Replace with "isBlank()"}}
    // edit@qf11 [[sc=25;ec=45]]{{isBlank()}}

    b = sb.toString().trim().length() == 0; // Noncompliant [[quickfixes=qf12]]
    // fix@qf12 {{Replace with "isBlank()"}}
    // edit@qf12 [[sc=23;ec=43]]{{isBlank()}}

    // only `trim()` is handled
    b = s.strip().length() == 0; // Noncompliant {{Use "isEmpty()" to check whether a "String" is empty or not.}} [[quickfixes=qf13]]
    // fix@qf13 {{Replace with "isEmpty()"}}
    // edit@qf13 [[sc=19;ec=32]]{{isEmpty()}}

    b = s.trim().length() == 1;
    b = s.trim().length() > 3;
    b = s.trim().length() >= 8;
    b = s.trim().length() == sb.length();

    b = s.isBlank();
    b = !s.isBlank();

    return b;
  }
}