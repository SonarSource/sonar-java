package checks;

public class StringLiteralInsideEqualsCheckSample {

  public String foo(String param, String foo, String bar) {
    foo.equals("qux");          // Noncompliant [[sc=16;ec=21]] {{Move the "qux" string literal on the left side of this string comparison.}}
    "foo".equals(bar);
    foo.equals("bar".length());
    foo.equalsIgnoreCase(""); // Noncompliant {{Move the "" string literal on the left side of this string comparison.}}

    foo("","","")
    .equals
    ("");                       // Noncompliant

    //coverage
    "foo".equals("bar");        // Compliant
    param.compareTo("");
    equals("");
    Covrg cov = new Covrg();
    cov.equals(0);
    cov.equals(bar, 0);
    return "";
  }

  private boolean equals(String s) {
    return false;
  }
  class Covrg {
    public boolean equals(int i) { return true; }
    public boolean equals(String s, int i) { return true; }
  }
}

class StringLiteralInsideEqualsCheckSampleQF{
  void goo(String foooo) {
    foooo.equals("bar"); // Noncompliant [[sc=18;ec=23;quickfixes=qf1]]
    // fix@qf1 {{Move "bar" on the left side of .equals}}
    // edit@qf1 [[sc=18;ec=23]] {{foooo}}
    // edit@qf1 [[sc=5;ec=10]] {{"bar"}}


    foooo.equals("probably too long to put in context menu"); // Noncompliant [[sc=18;ec=60;quickfixes=qf2]]
    // fix@qf2 {{Move "probably "... on the left side of .equals}}
    // edit@qf2 [[sc=18;ec=60]] {{foooo}}
    // edit@qf2 [[sc=5;ec=10]] {{"probably too long to put in context menu"}}

    foooo.equalsIgnoreCase(""); // Noncompliant [[sc=28;ec=30;quickfixes=qf3]]
    // fix@qf3 {{Move "" on the left side of .equals}}
    // edit@qf3 [[sc=28;ec=30]] {{foooo}}
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

  Fooo foo() {
    return new Fooo();
  }

  class Fooo{
    public String bar() {
      return "";
    }
  }

}