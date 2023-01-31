class Foo {
  void foo() {
    "foo".equals("bar");        // Noncompliant {{Move the "bar" string literal on the left side of this string comparison.}}
    foo.equals("qux");          // Noncompliant [[sc=16;ec=21]] {{Move the "qux" string literal on the left side of this string comparison.}}
    "foo".equals(bar);
    "foo".foo("bar");
    "foo".equals();
    foo.equals("bar".length());
    int a = foo.equals;
    "foo".equalsIgnoreCase(""); // Noncompliant {{Move the "" string literal on the left side of this string comparison.}}
    StringUtils.equals("", "");

    foo()
    .bar().
    equals
    ("");                       // Noncompliant
  }
}

class QuickFixes{
  void foo() {
    "foo".equals("bar"); // Noncompliant [[sc=18;ec=23;quickfixes=qf1]]
    // fix@qf1 {{Move "bar" on the left side of .equals}}
    // edit@qf1 [[sc=18;ec=23]] {{"foo"}}
    // edit@qf1 [[sc=5;ec=10]] {{"bar"}}


    "foo".equals("probably too long to put in context menu"); // Noncompliant [[sc=18;ec=60;quickfixes=qf2]]
    // fix@qf2 {{Move "probably "... on the left side of .equals}}
    // edit@qf2 [[sc=18;ec=60]] {{"foo"}}
    // edit@qf2 [[sc=5;ec=10]] {{"probably too long to put in context menu"}}

    "boo".equalsIgnoreCase(""); // Noncompliant [[sc=28;ec=30;quickfixes=qf3]]
    // fix@qf3 {{Move "" on the left side of .equals}}
    // edit@qf3 [[sc=28;ec=30]] {{"boo"}}
    // edit@qf3 [[sc=5;ec=10]] {{""}}

    foo()
    .bar().
    equals
    (""); // Noncompliant [[sc=6;ec=8;quickfixes=qf4]]
    // fix@qf4 {{Move "" on the left side of .equals}}
    // edit@qf4 [[sc=6;ec=8]] {{foo()\n    .bar()}}
    // edit@qf4 [[sl=-3;el=-2;sc=5;ec=11]] {{""}}

  }

  boolean doesNotHaveWings(String s, boolean isMammal, boolean exp) {
    return (isMammal && s != null && s.equals("bat")) ? false : exp; // Noncompliant [[sc=47;ec=52;quickfixes=qf5]]
    // fix@qf5 {{Move "bat" on the left side of .equals}}
    // edit@qf5 [[sc=47;ec=52]] {{s}}
    // edit@qf5 [[sc=38;ec=39]] {{"bat"}}
  }

}