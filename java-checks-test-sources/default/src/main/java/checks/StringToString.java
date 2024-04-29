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

  String string = "hello".toString(); // Noncompliant {{there's no need to call "toString()" on a string literal.}}
//                ^^^^^^^
  Object object = new Object();

  public void method() {

    object.toString(); // Compliant
    new Object().toString(); // Compliant
    ((String) object).toString(); // Noncompliant {{"object" is already a string, there's no need to call "toString()" on it.}}
//            ^^^^^^
    array[0].toString(); // Noncompliant {{"array" is an array of strings, there's no need to call "toString()".}}
//  ^^^^^
    string.toString(); // Noncompliant {{"string" is already a string, there's no need to call "toString()" on it.}}
    string.toUpperCase().toString(); // Noncompliant {{"toUpperCase" returns a string, there's no need to call "toString()".}}

    this.inner.field.toString(); // Noncompliant {{"field" is already a string, there's no need to call "toString()" on it.}}

    toString(); // Compliant
    foo()[0].toString(); // Noncompliant {{There's no need to call "toString()" on an array of String.}}
//  ^^^^^
    bar()[0][0].toString(); // Noncompliant {{There's no need to call "toString()" on an array of String.}}

    (object.equals("") ? "a" : "b").toString(); // Compliant, FN, report only clear issues
  }

  String[] foo() {return null;}
  String[][] bar() {return null;}

  void quickFixes() {
    String string = "hello".toString(); // Noncompliant [[quickfixes=qf1]]
//                  ^^^^^^^
    // fix@qf1 {{Remove "toString()"}}
    // edit@qf1 [[sc=28;ec=39]] {{}}

    string.toUpperCase().toString(); // Noncompliant [[quickfixes=qf2]]
//         ^^^^^^^^^^^
    // fix@qf2 {{Remove "toString()"}}
    // edit@qf2 [[sc=25;ec=36]] {{}}

    string = string.toString().toUpperCase(); // Noncompliant [[quickfixes=qf3]]
//           ^^^^^^
    // fix@qf3 {{Remove "toString()"}}
    // edit@qf3 [[sc=20;ec=31]] {{}}

    foo()[0].toString(); // Noncompliant [[quickfixes=qf4]]
//  ^^^^^
    // fix@qf4 {{Remove "toString()"}}
    // edit@qf4 [[sc=13;ec=24]] {{}}

    ((String) object).toString(); // Noncompliant [[quickfixes=qf5]]
//            ^^^^^^
    // fix@qf5 {{Remove "toString()"}}
    // edit@qf5 [[sc=22;ec=33]] {{}}

    string = ((string)).toString(); // Noncompliant [[quickfixes=qf6]]
//             ^^^^^^
    // fix@qf6 {{Remove "toString()"}}
    // edit@qf6 [[sc=24;ec=35]] {{}}
  }

}
