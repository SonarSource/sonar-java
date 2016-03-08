import static java.lang.String.format;
class A {
  void foo() {
    String myString ="";
    myString.toLowerCase(); // Noncompliant [[sc=14;ec=25]]
    myString.toUpperCase(); // Noncompliant
    myString.toLowerCase(java.util.Locale.US);
    myString.toUpperCase(java.util.Locale.US);

  }
}
