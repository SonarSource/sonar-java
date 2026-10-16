package checks;

public class StringIsBlankCheckSample {
  public boolean sample(String s, StringBuilder sb) {
    boolean b;

    // test `strip().length() == 0` and equivalent code

    b = s.strip().length() == 0; // Noncompliant {{Use "isBlank()" to check whether a "String" is blank or not.}} [[quickfixes=qf1]]
    // fix@qf1 {{Replace with "isBlank()"}}
    // edit@qf1 [[sc=11;ec=32]]{{isBlank()}}

    b = s.strip().length() <= 0; // Noncompliant [[quickfixes=qf2]]
    // fix@qf2 {{Replace with "isBlank()"}}
    // edit@qf2 [[sc=11;ec=32]]{{isBlank()}}

    b = s.strip().length() < 1; // Noncompliant [[quickfixes=qf3]]
    // fix@qf3 {{Replace with "isBlank()"}}
    // edit@qf3 [[sc=11;ec=31]]{{isBlank()}}

    // test `strip().length() != 0` and equivalent code

    b = s.strip().length() != 0; // Noncompliant [[quickfixes=qf4]]
    // fix@qf4 {{Replace with "isBlank()"}}
    // edit@qf4 [[sc=9;ec=9]]{{!}}
    // edit@qf4 [[sc=11;ec=32]]{{isBlank()}}

    b = s.strip().length() > 0; // Noncompliant [[quickfixes=qf5]]
    // fix@qf5 {{Replace with "isBlank()"}}
    // edit@qf5 [[sc=9;ec=9]]{{!}}
    // edit@qf5 [[sc=11;ec=31]]{{isBlank()}}

    b = s.strip().length() >= 1; // Noncompliant [[quickfixes=qf6]]
    // fix@qf6 {{Replace with "isBlank()"}}
    // edit@qf6 [[sc=9;ec=9]]{{!}}
    // edit@qf6 [[sc=11;ec=32]]{{isBlank()}}

    // reversed order

    b = 0 == s.strip().length(); // Noncompliant [[quickfixes=qf7]]
    // fix@qf7 {{Replace with "isBlank()"}}
    // edit@qf7 [[sc=9;ec=14]]{{}}
    // edit@qf7 [[sc=16;ec=32]]{{isBlank()}}

    b = 0 != s.strip().length(); // Noncompliant [[quickfixes=qf8]]
    // fix@qf8 {{Replace with "isBlank()"}}
    // edit@qf8 [[sc=9;ec=14]]{{!}}
    // edit@qf8 [[sc=16;ec=32]]{{isBlank()}}

    // extra parentheses

    b = (s.strip().length()) == 0; // Noncompliant [[quickfixes=qf9]]
    // fix@qf9 {{Replace with "isBlank()"}}
    // edit@qf9 [[sc=9;ec=10]]{{}}
    // edit@qf9 [[sc=12;ec=34]]{{isBlank()}}

    // the `strip()` call is not directly chained, so only emptiness can be simplified
    b = (s.strip()).length() == 0; // Noncompliant {{Use "isEmpty()" to check whether a "String" is empty or not.}} [[quickfixes=qf10]]
    // fix@qf10 {{Replace with "isEmpty()"}}
    // edit@qf10 [[sc=21;ec=34]]{{isEmpty()}}

    // chained method calls

    b = s.toUpperCase().strip().length() == 0; // Noncompliant [[quickfixes=qf11]]
    // fix@qf11 {{Replace with "isBlank()"}}
    // edit@qf11 [[sc=25;ec=46]]{{isBlank()}}

    b = sb.toString().strip().length() == 0; // Noncompliant [[quickfixes=qf12]]
    // fix@qf12 {{Replace with "isBlank()"}}
    // edit@qf12 [[sc=23;ec=44]]{{isBlank()}}

    // `trim()` does not remove the same characters as `isBlank()` considers white space,
    // so the message and the quick fix spell out what changes

    b = s.trim().length() == 0; // Noncompliant {{Use "isBlank()" to check whether a "String" is blank or not. Use this if Unicode whitespace should be treated as blank and control characters as non-blank.}} [[quickfixes=qf13]]
    // fix@qf13 {{Replace with "isBlank()" (Unicode whitespace is treated as blank, control characters as non-blank)}}
    // edit@qf13 [[sc=11;ec=31]]{{isBlank()}}

    b = s.trim().length() > 0; // Noncompliant [[quickfixes=qf14]]
    // fix@qf14 {{Replace with "isBlank()" (Unicode whitespace is treated as blank, control characters as non-blank)}}
    // edit@qf14 [[sc=9;ec=9]]{{!}}
    // edit@qf14 [[sc=11;ec=30]]{{isBlank()}}

    b = 0 == s.trim().length(); // Noncompliant [[quickfixes=qf15]]
    // fix@qf15 {{Replace with "isBlank()" (Unicode whitespace is treated as blank, control characters as non-blank)}}
    // edit@qf15 [[sc=9;ec=14]]{{}}
    // edit@qf15 [[sc=16;ec=31]]{{isBlank()}}

    b = s.toLowerCase().trim().length() == 0; // Noncompliant [[quickfixes=qf16]]
    // fix@qf16 {{Replace with "isBlank()" (Unicode whitespace is treated as blank, control characters as non-blank)}}
    // edit@qf16 [[sc=25;ec=45]]{{isBlank()}}

    b = s.trim().length() == 1;
    b = s.trim().length() >= 8;

    b = s.strip().length() == 1;
    b = s.strip().length() > 3;
    b = s.strip().length() >= 8;
    b = s.strip().length() == sb.length();

    b = s.isBlank();
    b = !s.isBlank();

    return b;
  }
}
