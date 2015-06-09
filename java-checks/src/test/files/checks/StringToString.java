public class TestClass {

  public static class InnerClass {
    String field;

    InnerClass inner() {
    }
  }

  String[] array;

  InnerClass inner;

  String string = "hello".toString(); // Noncompliant {{there's no need to call "toString()" on a string literal.}}

  public void method() {
    Object object = new Object();

    object.toString(); // Compliant
    new Object().toString(); // Compliant
    ((String) object).toString(); // Noncompliant {{"object" is already a string, there's no need to call "toString()" on it.}}
    array[0].toString(); // Noncompliant {{"array" is an array of strings, there's no need to call "toString()".}}
    string.toString(); // Noncompliant {{"string" is already a string, there's no need to call "toString()" on it.}}
    string.toUpperCase().toString(); // Noncompliant {{"toUpperCase" returns a string, there's no need to call "toString()".}}

    this.inner.field.toString(); // Noncompliant {{"field" is already a string, there's no need to call "toString()" on it.}}

    toString(); // Compliant
    foo()[0].toString(); // Noncompliant {{There's no need to call "toString()" on an array of String.}}
    bar()[0][0].toString(); // Noncompliant {{There's no need to call "toString()" on an array of String.}}
  }

  String[] foo() {}
  String[][] bar() {}

}
