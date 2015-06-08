import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
class A {
  void foo(){
    Object myObject;
    double value;
    String.format("The value of my integer is %d", "Hello World");  // Noncompliant an 'int' is expected rather than a String
    String.format("First {0} and then {1}", "foo", "bar");  //Noncompliant. Looks like there is a confusion with the use of {{java.text.MessageFormat}}, parameters "foo" and "bar" will be simply ignored here
    String.format("Duke's Birthday year is %tX", 12l);  //Noncompliant X is not a supported time conversion character
    String.format("Display %3$d and then %d", 1, 2, 3);   //Noncompliant the second argument '2' is unused
    String.format("Too many arguments %d and %d", 1, 2, 3);  //Noncompliant the third argument '3' is unused
    String.format("Not enough arguments %d and %d", 1);  //Noncompliant the second argument is missing
    String.format("First Line\n");   //Noncompliant %n should be used in place of \n to produce the platform-specific line separator
    String.format("%< is equals to %d", 2);   //Noncompliant the argument index '<' refers to the previous format specifier but there isn't one
    String.format("Is myObject null ? %b", myObject);   //Noncompliant when a non-boolean argument is formatted with %b, it prints true for any nonnull value, and false for null. Even if intended, this is misleading. It's better to directly inject the boolean value (myObject == null in this case)
    String.format("value is " + value); // Noncompliant : string concatenation in argument.
    String.format("string without arguments"); // Noncompliant

    PrintWriter pr;
    PrintStream ps;
    Formatter formatter;
    Locale loc;

    pr.format("The value of my integer is %d", "Hello World");  // Noncompliant a 'int' is expected rather than a String
    pr.printf("The value of my integer is %d", "Hello World");  // Noncompliant a 'int' is expected rather than a String
    ps.format("The value of my integer is %d", "Hello World");  // Noncompliant a 'int' is expected rather than a String
    ps.printf(loc, "The value of my integer is %d", "Hello World");  // Noncompliant an 'int' is expected rather than a String
    formatter.format("The value of my integer is %d", "Hello World");  // Noncompliant an 'int' is expected rather than a String
    pr.format("%s:\tintCompact %d\tintVal %d\tscale %d\tprecision %d%n","", 1, 1, 1, 1);
    pr.format("%s:\tintCompact %n%n%n%d\tintVal %d\tscale %d\tprecision %d%n","", 1, 1, 1, 1);
    pr.format("%TH", 1l);
    pr.format("%d", new Long(12));
    pr.format("%d", new java.math.BigInteger("12"));
    String.format("Too many arguments %d and %d and %d", 1, 2, 3, 4);  //Noncompliant
    String.format("normal %d%% ", 1);  //Compliant
    String.format("Duke's Birthday year is %t", 12l);  //Noncompliant t argument is empty
    String.format("Duke's Birthday year is %tH", loc);  //Noncompliant loc is not a supported object
    String.format("%08d%n", 1);
    GregorianCalendar gc;
    String.format("Duke's Birthday year is %tH", gc);
    String.format("Duke's Birthday year is %t", loc);  //Noncompliant t argument is empty AND loc is not compatible

    pr.format("string without arguments"); // Noncompliant
    pr.format(loc, "string without arguments"); // Noncompliant
    pr.printf("string without arguments"); // Noncompliant
    pr.printf(loc, "string without arguments"); // Noncompliant
    ps.format("string without arguments"); // Noncompliant
    ps.format(loc, "string without arguments"); // Noncompliant
    ps.printf("string without arguments"); // Noncompliant
    ps.printf(loc, "string without arguments"); // Noncompliant
    formatter.format("string without arguments"); // Noncompliant
    formatter.format(loc, "string without arguments"); // Noncompliant
    
    pr.format("value is " + value); // Noncompliant
    pr.format(loc, "value is " + value); // Noncompliant
    pr.printf("value is " + value); // Noncompliant
    pr.printf(loc, "value is " + value); // Noncompliant
    ps.format("value is " + value); // Noncompliant
    ps.format(loc, "value is " + value); // Noncompliant
    ps.printf("value is " + value); // Noncompliant
    ps.printf(loc, "value is " + value); // Noncompliant
    formatter.format("value is " + value); // Noncompliant
    formatter.format(loc, "value is " + value); // Noncompliant

    String.format("value is %d", value); // compliant

    String.format("%0$s", "tmp"); // Noncompliant

  }
}