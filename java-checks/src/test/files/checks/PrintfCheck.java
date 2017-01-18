import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Calendar;

class A {
  void foo(Calendar c){
    Object myObject;
    double value;
    String.format("The value of my integer is %d", "Hello World");  // Noncompliant {{An 'int' is expected rather than a String.}}
    String.format("First {0} and then {1}", "foo", "bar");  // Noncompliant  {{Looks like there is a confusion with the use of java.text.MessageFormat, parameters will be simply ignored here}}
    String.format("Duke's Birthday year is %tX", 12l);  // Noncompliant {{X is not a supported time conversion character}}
    String.format("Display %3$d and then %d", 1, 2, 3);   // Noncompliant {{2nd argument is not used.}}
    String.format("Too many arguments %d and %d", 1, 2, 3);  // Noncompliant {{3rd argument is not used.}}
    String.format("Not enough arguments %d and %d", 1);  // Noncompliant {{Not enough arguments.}}
    String.format("First Line\n %d", 1); // Noncompliant {{%n should be used in place of \n to produce the platform-specific line separator.}}
    String.format("First Line");   // Noncompliant {{String contains no format specifiers.}}
    String.format("First Line%%"); // Noncompliant {{String contains no format specifiers.}}
    String.format("First Line%n"); // Compliant
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
    String.format("Duke's Birthday year is %tH", loc);  // Noncompliant {{Time argument is expected (long, Long, Calendar, Date and TemporalAccessor).}}
    String.format("%08d%n", 1);
    GregorianCalendar gc;
    String.format("Duke's Birthday year is %tH", gc);
    // Noncompliant@+1
    String.format("Duke's Birthday year is %t", loc);  // Noncompliant
    String.format("Accessed before %tF%n", java.time.LocalDate.now()); // Compliant
    System.out.printf("%1$ty_%1$tm_%1$td_%1$tH_%1$tM_%1$tS", java.time.LocalDateTime.now()); // Compliant

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

    String.format("Dude's Birthday: %1$tm %<te,%<tY", c); // Compliant
    String.format("Dude's Birthday: %1$tm %1$te,%1$tY", c); // Compliant
    String.format("log/protocol_%tY_%<tm_%<td_%<tH_%<tM_%<tS.zip", new java.util.Date());

    MessageFormat messageFormat = new MessageFormat("{0}");
    messageFormat.format(new Object(), new StringBuffer(), new FieldPosition(0)); // Compliant - Not considered
    messageFormat.format(new Object()); // Compliant - Not considered
    messageFormat.format("");  // Compliant - Not considered

    Object[] objs;
    MessageFormat.format("{0,number,$'#',##}", value); // Compliant
    MessageFormat.format("Result ''{0}''.", 14); // Compliant
    MessageFormat.format("Result '{0}'", 14); // Noncompliant {{String contains no format specifiers.}}
    MessageFormat.format("Result ' {0}", 14); // Noncompliant {{Single quote "'" must be escaped.}}
    MessageFormat.format("Result {{{0}}.", 14); // Noncompliant {{Single left curly braces "{" must be escaped.}}
    MessageFormat.format("Result {0}!", myObject.toString()); // Noncompliant {{No need to call toString "method()" as formatting and string conversion is done by the Formatter.}}
    MessageFormat.format("Result {0}!", myObject.hashCode()); // Compliant
    MessageFormat.format("Result yeah!", 14); // Noncompliant {{String contains no format specifiers.}}
    MessageFormat.format("Result {1}!", 14); // Noncompliant {{Not enough arguments.}}
    MessageFormat.format("Result {0} and {1}!", 14); // Noncompliant {{Not enough arguments.}}
    MessageFormat.format("Result {0} and {0}!", 14, 42); // Noncompliant {{2nd argument is not used.}}
    MessageFormat.format("Result {0, number, integer} and {1, number, integer}!", 14, 42); // compliant
    MessageFormat.format("Result {0} and {1}!", 14, 42, 128); // Noncompliant {{3rd argument is not used.}}
    MessageFormat.format("{0,number,#.#}{1}", new Object[] {0.07, "$"}); // Compliant
    MessageFormat.format("{0,number,#.#}{1}", new Object[] {0.07}); // Noncompliant {{Not enough arguments.}}
    MessageFormat.format("{0,number,#.#}{1}", objs); // Compliant - skipped as the array is not initialized in the method invocation
    MessageFormat.format("{0,number,#.#}{1}", new Object[42]); // Compliant - Not considered
  }
}
