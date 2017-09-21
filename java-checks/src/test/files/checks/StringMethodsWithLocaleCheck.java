import static java.lang.String.format;

class A {

  private static final String MY_TEMPLATE = "%t";

  void foo(java.util.Date date) {
    String myString ="";
    myString.toLowerCase(); // Noncompliant [[sc=14;ec=25]]
    myString.toUpperCase(); // Noncompliant
    myString.toLowerCase(java.util.Locale.US);
    myString.toUpperCase(java.util.Locale.US);
    String s = new String() {
      void foo() {
        toLowerCase(); // Noncompliant [[sc=9;ec=20]] {{Define the locale to be used in this String operation.}}
      }
    };

    myString.format("foo"); // Compliant
    myString.format("foo", "bar", "qix"); // Compliant
    myString.format(java.util.Locale.US, "foo", "bar", "qix"); // Compliant
    format("foo"); // Compliant

    // dates
    myString.format("%t", date); // Noncompliant [[sc=14;ec=20]]
    myString.format(java.util.Locale.US, "%T", date); // Compliant
    format("%t" + 42, date); // Noncompliant [[sc=5;ec=11]]
    format("%1$tDc", date); // Noncompliant

    // integer values : only when comma is used
    format("%d", 42); // Compliant
    format("%0+(15d", 42); // Compliant
    format("%%0+,(15d", 42); // compliant - double %% is simply displaying the percent char
    format("%1$0+,(15d", 42); // Noncompliant

    // any floating point format
    format("this will display my value in dollar: %f$" + " (dollars)", 42.01234f); // Noncompliant
    format("this will display my value in dollar: %0+,(15.25f$" + " (dollars)", 12345.01234f); // Noncompliant

    format(MY_TEMPLATE, date); // Compliant - FN - non-trivial case
  }
}
