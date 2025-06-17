package checks;

public class StringIsEmptyCheckSample {
  public boolean sample(String s, String t) {
    boolean b;

    // test `length() == 0` and equivalent code

    b = s.length() == 0; // Noncompliant [[quickfixes=qf1]]
    // fix@qf1 {{Replace with "isEmpty()"}}
    // edit@qf1 [[sc=11;ec=24]]{{isEmpty()}}

    b = s.length() <= 0; // Noncompliant [[quickfixes=qf2]]
    // fix@qf2 {{Replace with "isEmpty()"}}
    // edit@qf2 [[sc=11;ec=24]]{{isEmpty()}}

    b = s.length() < 1; // Noncompliant [[quickfixes=qf3]]
    // fix@qf3 {{Replace with "isEmpty()"}}
    // edit@qf3 [[sc=11;ec=23]]{{isEmpty()}}

    // test `length() != 0` and equivalent code
    b = s.length() != 0; // Noncompliant [[quickfixes=qf4]]
    // fix@qf4 {{Replace with "isEmpty()"}}
    // edit@qf4 [[sc=9;ec=9]]{{!}}
    // edit@qf4 [[sc=11;ec=24]]{{isEmpty()}}

    b = s.length() > 0; // Noncompliant [[quickfixes=qf5]]
    // fix@qf5 {{Replace with "isEmpty()"}}
    // edit@qf5 [[sc=9;ec=9]]{{!}}
    // edit@qf5 [[sc=11;ec=23]]{{isEmpty()}}

    b = s.length() >= 1; // Noncompliant [[quickfixes=qf6]]
    // fix@qf6 {{Replace with "isEmpty()"}}
    // edit@qf6 [[sc=9;ec=9]]{{!}}
    // edit@qf6 [[sc=11;ec=24]]{{isEmpty()}}

    // reversed order

    b = 0 == s.length(); // Noncompliant [[quickfixes=qf7]]
    // fix@qf7 {{Replace with "isEmpty()"}}
    // edit@qf7 [[sc=9;ec=14]]{{}}
    // edit@qf7 [[sc=16;ec=24]]{{isEmpty()}}

    b = 0 >= s.length(); // Noncompliant [[quickfixes=qf8]]
    // fix@qf8 {{Replace with "isEmpty()"}}
    // edit@qf8 [[sc=9;ec=14]]{{}}
    // edit@qf8 [[sc=16;ec=24]]{{isEmpty()}}

    b = 1 > s.length(); // Noncompliant [[quickfixes=qf9]]
    // fix@qf9 {{Replace with "isEmpty()"}}
    // edit@qf9 [[sc=9;ec=13]]{{}}
    // edit@qf9 [[sc=15;ec=23]]{{isEmpty()}}

    b = 0 != s.length(); // Noncompliant [[quickfixes=qf10]]
    // fix@qf10 {{Replace with "isEmpty()"}}
    // edit@qf10 [[sc=9;ec=14]]{{!}}
    // edit@qf10 [[sc=16;ec=24]]{{isEmpty()}}

    b = 0 < s.length(); // Noncompliant [[quickfixes=qf11]]
    // fix@qf11 {{Replace with "isEmpty()"}}
    // edit@qf11 [[sc=9;ec=13]]{{!}}
    // edit@qf11 [[sc=15;ec=23]]{{isEmpty()}}

    b = 1 <= s.length(); // Noncompliant [[quickfixes=qf12]]
    // fix@qf12 {{Replace with "isEmpty()"}}
    // edit@qf12 [[sc=9;ec=14]]{{!}}
    // edit@qf12 [[sc=16;ec=24]]{{isEmpty()}}

    // extra parentheses
    b = (s.length()) == 0; // Noncompliant [[quickfixes=qf13]]
    // fix@qf13 {{Replace with "isEmpty()"}}
    // edit@qf13 [[sc=9;ec=10]]{{}}
    // edit@qf13 [[sc=12;ec=26]]{{isEmpty()}}

    // chained method calls
    b = s.toUpperCase().length() == 0; // Noncompliant [[quickfixes=qf14]]
    // fix@qf14 {{Replace with "isEmpty()"}}
    // edit@qf14 [[sc=25;ec=38]]{{isEmpty()}}

    // problem in a nested expression
    b = "abc".equals(s) || s.length() == 0; // Noncompliant [[quickfixes=qf15]]
    // fix@qf15 {{Replace with "isEmpty()"}}
    // edit@qf15 [[sc=30;ec=43]]{{isEmpty()}}

    b = s.length() == 1;
    b = s.length() > 3;
    b = s.length() <= 10;
    b = 2 < s.length();

    b = s.trim().length() >= 8;

    b = s.length() == t.length();

    b = s.isEmpty();
    b = !s.isEmpty();

    b = 1 < 0;


    return b;
  }
}
