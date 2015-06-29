import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
class A {
  void foo(){
    Object myObject;
    double value;
    String.format("The value of my integer is %d", "Hello World");  // Noncompliant {{An 'int' is expected rather than a String.}}
    String.format("First {0} and then {1}", "foo", "bar");  // Noncompliant  {{Looks like there is a confusion with the use of java.text.MessageFormat, parameters will be simply ignored here}}
    String.format("Duke's Birthday year is %tX", 12l);  // Noncompliant {{X is not a supported time conversion character}}
    String.format("Display %3$d and then %d", 1, 2, 3);   // Noncompliant {{2nd argument is not used.}}
    String.format("Too many arguments %d and %d", 1, 2, 3);  // Noncompliant {{3rd argument is not used.}}
    String.format("Not enough arguments %d and %d", 1);  // Noncompliant {{Not enough arguments.}}
    String.format("First Line\n %d", 1); // Noncompliant {{%n should be used in place of \n to produce the platform-specific line separator.}}
    String.format("First Line");// Noncompliant {{String contains no format specifiers.}}
    String.format("%< is equals to %d", 2);   // Noncompliant {{The argument index '<' refers to the previous format specifier but there isn't one.}}
    String.format("Is myObject null ? %b", myObject);   // Noncompliant {{Directly inject the boolean value.}}
    String.format("value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    String.format("string without arguments"); // Noncompliant {{String contains no format specifiers.}}

    PrintWriter pr;
    PrintStream ps;
    Formatter formatter;
    Locale loc;

    pr.format("The value of my integer is %d", "Hello World");  // Noncompliant {{An 'int' is expected rather than a String.}}
    pr.printf("The value of my integer is %d", "Hello World");  // Noncompliant {{An 'int' is expected rather than a String.}}
    ps.format("The value of my integer is %d", "Hello World");  // Noncompliant {{An 'int' is expected rather than a String.}}
    ps.printf(loc, "The value of my integer is %d", "Hello World");  // Noncompliant {{An 'int' is expected rather than a String.}}
    formatter.format("The value of my integer is %d", "Hello World");  // Noncompliant {{An 'int' is expected rather than a String.}}
    pr.format("%s:\tintCompact %d\tintVal %d\tscale %d\tprecision %d%n","", 1, 1, 1, 1);
    pr.format("%s:\tintCompact %n%n%n%d\tintVal %d\tscale %d\tprecision %d%n","", 1, 1, 1, 1);
    pr.format("%TH", 1l);
    pr.format("%d", new Long(12));
    pr.format("%d", new java.math.BigInteger("12"));
    String.format("Too many arguments %d and %d and %d", 1, 2, 3, 4);  // Noncompliant {{4th argument is not used.}}
    String.format("normal %d%% ", 1);  //Compliant
    String.format("Duke's Birthday year is %t", 12l);  // Noncompliant {{Time conversion requires a second character.}}
    String.format("Duke's Birthday year is %tH", loc);  // Noncompliant {{Time argument is expected (long, Long, Date or Calendar).}}
    String.format("%08d%n", 1);
    GregorianCalendar gc;
    String.format("Duke's Birthday year is %tH", gc);
    String.format("Duke's Birthday year is %t", loc);  // Noncompliant 2

    pr.format("string without arguments"); // Noncompliant  {{String contains no format specifiers.}}
    pr.format(loc, "string without arguments"); // Noncompliant  {{String contains no format specifiers.}}
    pr.printf("string without arguments"); // Noncompliant  {{String contains no format specifiers.}}
    pr.printf(loc, "string without arguments"); // Noncompliant  {{String contains no format specifiers.}}
    ps.format("string without arguments"); // Noncompliant  {{String contains no format specifiers.}}
    ps.format(loc, "string without arguments"); // Noncompliant  {{String contains no format specifiers.}}
    ps.printf("string without arguments"); // Noncompliant  {{String contains no format specifiers.}}
    ps.printf(loc, "string without arguments"); // Noncompliant  {{String contains no format specifiers.}}
    formatter.format("string without arguments"); // Noncompliant  {{String contains no format specifiers.}}
    formatter.format(loc, "string without arguments"); // Noncompliant  {{String contains no format specifiers.}}
    
    pr.format("value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    pr.format(loc, "value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    pr.printf("value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    pr.printf(loc, "value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    ps.format("value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    ps.format(loc, "value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    ps.printf("value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    ps.printf(loc, "value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    formatter.format("value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    formatter.format(loc, "value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}

    pr.format("value is "+"asd"); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    pr.format("value is "+
        "asd"); // Compliant operand not on the same line.
    String.format("value is %d", value); // Compliant

    String.format("%0$s", "tmp"); // Noncompliant {{Arguments are numbered starting from 1.}}

  }
}