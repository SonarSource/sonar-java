package checks;

public class StringToString {

  public static class InnerClass {
    String field;

    InnerClass inner() {
      return null;
    }
  }

  String[] array;

  InnerClass inner;

  String string = "hello".toString(); // Noncompliant [[sc=19;ec=26]] {{there's no need to call "toString()" on a string literal.}}

  public void method() {
    Object object = new Object();

    object.toString(); // Compliant
    new Object().toString(); // Compliant
    ((String) object).toString(); // Noncompliant [[sc=15;ec=21]] {{"object" is already a string, there's no need to call "toString()" on it.}}
    array[0].toString(); // Noncompliant [[sc=5;ec=10]] {{"array" is an array of strings, there's no need to call "toString()".}}
    string.toString(); // Noncompliant {{"string" is already a string, there's no need to call "toString()" on it.}}
    string.toUpperCase().toString(); // Noncompliant {{"toUpperCase" returns a string, there's no need to call "toString()".}}

    this.inner.field.toString(); // Noncompliant {{"field" is already a string, there's no need to call "toString()" on it.}}

    toString(); // Compliant
    foo()[0].toString(); // Noncompliant [[sc=5;ec=10]] {{There's no need to call "toString()" on an array of String.}}
    bar()[0][0].toString(); // Noncompliant {{There's no need to call "toString()" on an array of String.}}

    (object.equals("") ? "a" : "b").toString(); // Compliant, FN, report only clear issues
  }

  String[] foo() {return null;}
  String[][] bar() {return null;}

  void quickFixes() {
    String string = "hello".toString(); // Noncompliant [[sc=21;ec=28;quickfixes=qf1]]
    // fix@qf1 {{Remove "toString()"}}
    // edit@qf1 [[sc=28;ec=39]] {{}}

    string.toUpperCase().toString(); // Noncompliant [[sc=12;ec=23;quickfixes=qf2]]
    // fix@qf2 {{Remove "toString()"}}
    // edit@qf2 [[sc=25;ec=36]] {{}}

    string = string.toString().toUpperCase(); // Noncompliant [[sc=14;ec=20;quickfixes=qf3]]
    // fix@qf3 {{Remove "toString()"}}
    // edit@qf3 [[sc=20;ec=31]] {{}}

    foo()[0].toString(); // Noncompliant [[sc=5;ec=10;quickfixes=qf4]]
    // fix@qf4 {{Remove "toString()"}}
    // edit@qf4 [[sc=13;ec=24]] {{}}
  }

}
