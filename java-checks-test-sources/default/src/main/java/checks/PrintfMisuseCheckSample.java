package checks;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Random;
import java.util.stream.IntStream;
import org.apache.logging.log4j.LogManager;

public class PrintfMisuseCheckSample {

  // used inside the tested file
  public static final String COMPILE_TIME_CONSTANT = "message";
  public static final String NON_COMPILE_TIME_CONSTANT = "" + new Random().nextInt();

  java.util.logging.Logger loggerField = java.util.logging.Logger.getAnonymousLogger("som.foo.resources.i18n.LogMessages");
  void foo(Calendar c) throws IOException {
    Object myObject = new Object();
    Object[] objs = new Object[]{14};
    double value = 1.0;
    Locale loc = Locale.US;
    // String format ===================================================================================================
    String.format("The value of my integer is %d", "Hello World");
    String.format("First {0} and then {1}", "foo", "bar");  // Noncompliant  {{Looks like there is a confusion with the use of java.text.MessageFormat, parameters will be simply ignored here}}
    String.format("{1}", "foo", "bar");  // Noncompliant
    String.format("Duke's Birthday year is %tX", 12l);
    String.format("Display %3$d and then %d", 1, 2, 3);   // Noncompliant {{2nd argument is not used.}}
    String.format("Display %2$d and then %2$d", 1, 2);   // Noncompliant {{first argument is not used.}}
    String.format("Too many arguments %d and %d", 1, 2, 3);  // Noncompliant {{3rd argument is not used.}}
    String.format("Not enough arguments %d and %d", 1);
    String.format("%1$d %2$d %9$-3.3s", 1, 2, "hello");  // Compliant - not enough arguments but this will be caught by S2275
    String.format("%12$s", 1, 2, "hello");  // Compliant - not enough arguments but this will be caught by S2275
    String.format("First Line\n %d", 1); // Noncompliant {{%n should be used in place of \n to produce the platform-specific line separator.}}
    String.format("First Line");   // Noncompliant {{String contains no format specifiers.}}
    String.format("First Line%%"); // Noncompliant {{String contains no format specifiers.}}
    String.format("First Line%n"); // Compliant
    String.format("%< is equals to %d", 2);
    String.format("Is myObject null ? %b", myObject);   // Noncompliant {{Directly inject the boolean value.}}
    String.format("boolean are %b, %b, %b and %b", true, Boolean.TRUE, false, Boolean.FALSE);
    String.format("value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    String.format((String) "");
    String.format(value + "value is "); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    String.format("value is " + NON_COMPILE_TIME_CONSTANT); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    String.format("value is " + COMPILE_TIME_CONSTANT);
    String.format("%s " + value, new Exception()); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    String.format("string without arguments"); // Noncompliant {{String contains no format specifiers.}}
    String.format("%s", new Exception());
    String.format("%d %d", new Object[]{1,2}); // Compliant
    String.format("%d %d", new Object[]{1,2,3}); // Noncompliant {{3rd argument is not used.}}
    String.format("%d %d", new Object[]{1}); // Compliant, reported by S2275
    String.format("%d %d", objs); // Compliant, not initialized inside the call
    String.format("%d %d", new Object[42]); // Compliant
    String.format("%d%d", IntStream.range(0, 2).mapToObj(Integer::valueOf).toArray()); // Compliant
    String.format("%d%d", IntStream.range(0, 2).mapToObj(Integer::valueOf).toArray(Object[]::new)); // Compliant
    String.format("Result %s %s",new Exception(),new Exception(),new Exception()); // Noncompliant {{3rd argument is not used.}}
    String.format("Result %s %s",new Object[] {new Exception(),new Exception(),new Exception()}); // Noncompliant {{3rd argument is not used.}}
    String.format("Result %s %s",new Exception(),new Exception()); // Compliant
    String.format("Result %s %s",new Object[] {new Exception(),new Exception()}); // Compliant
    String.format("Result %s %s",new Exception()); // Compliant, reported by S2275
    String.format("Result %s %s",new Object[] {new Exception()}); // Compliant, reported by S2275

    String.format("Too many arguments %d and %d and %d", 1, 2, 3, 4);  // Noncompliant {{4th argument is not used.}}
    String.format("normal %d%% ", 1);  //Compliant
    String.format("Duke's Birthday year is %t", 12l);
    String.format("Duke's Birthday year is %tH", 12l);  // Compliant
    String.format("Duke's Birthday year is %tH", Long.valueOf(12L));  // Compliant
    String.format("Duke's Birthday year is %tH", loc);
    String.format("%08d%n", 1);
    GregorianCalendar gc = (GregorianCalendar) GregorianCalendar.getInstance();
    String.format("Duke's Birthday year is %tH", gc);
    String.format("Duke's Birthday year is %t", loc);
    String.format("Accessed before %tF%n", java.time.LocalDate.now()); // Compliant
    System.out.printf("%1$ty_%1$tm_%1$td_%1$tH_%1$tM_%1$tS", java.time.LocalDateTime.now()); // Compliant

    String.format("%0$s", "tmp"); // Noncompliant {{Arguments are numbered starting from 1.}}
    String.format("Dude's Birthday: %1$tm %<te,%<tY", c); // Compliant
    String.format("Dude's Birthday: %1$tm %1$te,%1$tY", c); // Compliant
    String.format("log/protocol_%tY_%<tm_%<td_%<tH_%<tM_%<tS.zip", new java.util.Date());

    // String.formatted ================================================================================================
    "The value of my integer is %d".formatted("Hello World");
    "First {0} and then {1}".formatted("foo", "bar");  // Noncompliant  {{Looks like there is a confusion with the use of java.text.MessageFormat, parameters will be simply ignored here}}
    "{1}".formatted("foo", "bar");  // Noncompliant
    "Duke's Birthday year is %tX".formatted(12l);
    "Display %3$d and then %d".formatted(1, 2, 3);   // Noncompliant {{2nd argument is not used.}}
    "Display %2$d and then %2$d".formatted(1, 2);   // Noncompliant {{first argument is not used.}}
    "Too many arguments %d and %d".formatted(1, 2, 3);  // Noncompliant {{3rd argument is not used.}}
    "Not enough arguments %d and %d".formatted(1);
    "%1$d %2$d %9$-3.3s".formatted(1, 2, "hello");  // Compliant - not enough arguments but this will be caught by S2275
    "%12$s".formatted(1, 2, "hello");  // Compliant - not enough arguments but this will be caught by S2275
    "First Line\n %d".formatted(1); // Noncompliant {{%n should be used in place of \n to produce the platform-specific line separator.}}
    "First Line".formatted();   // Noncompliant {{String contains no format specifiers.}}


    // Print Writer / Stream / Formatter ===============================================================================
    PrintWriter pr = new PrintWriter("file");
    PrintStream ps = new PrintStream("file");
    Formatter formatter = new Formatter();

    pr.format("The value of my integer is %d", "Hello World");
    pr.printf("The value of my integer is %d", "Hello World");
    ps.format("The value of my integer is %d", "Hello World");
    ps.printf(loc, "The value of my integer is %d", "Hello World");
    formatter.format("The value of my integer is %d", "Hello World");
    pr.format("%s:\tintCompact %d\tintVal %d\tscale %d\tprecision %d%n","", 1, 1, 1, 1);
    pr.format("%s:\tintCompact %n%n%n%d\tintVal %d\tscale %d\tprecision %d%n","", 1, 1, 1, 1);
    pr.format("%TH", 1l);
    pr.format("%d", new Long(12));
    pr.format("%d", new java.math.BigInteger("12"));

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

    pr.format("value is "+"asd");
    String.format("value is %d", value); // Compliant

    pr.format("value is "
      +"asd");
    pr.format("value is "+ // Noncompliant
      value);
    pr.format("value " + " is "+ value + "."); // Noncompliant
    String.format(
      "value is " + " %d " +
      ".", value);
    String.format(
      "line1 %s " +
      "line2", "myValue");

    // MessageFormat ===================================================================================================
    MessageFormat messageFormat = new MessageFormat("{0}");
    messageFormat.format(new Object(), new StringBuffer(), new FieldPosition(0)); // Compliant - Not considered
    messageFormat.format(new Object()); // Compliant - Not considered
    messageFormat.format("");  // Compliant - Not considered

    MessageFormat.format("{0,number,$'#',##}", value); // Compliant
    MessageFormat.format("Result ''{0}''.", 14); // Compliant
    MessageFormat.format("Result '{0}'", 14); // Noncompliant {{String contains no format specifiers.}}
    MessageFormat.format("Result ' {0}", 14); // Noncompliant {{Single quote "'" must be escaped.}}
    MessageFormat.format("Result {{{0}}.", 14); // Compliant, reported by S2275
    MessageFormat.format("Result {0}!", myObject.toString()); // Noncompliant {{No need to call "toString()" method as formatting and string conversion is done by the Formatter.}}
    MessageFormat.format("Result {0}!", myObject.hashCode()); // Compliant
    MessageFormat.format("Result yeah!", 14); // Noncompliant {{String contains no format specifiers.}}
    MessageFormat.format("Result {1}!", 14); // Noncompliant {{Not enough arguments.}}
    MessageFormat.format("Result {0} and {1}!", 14); // Noncompliant {{Not enough arguments.}}
    MessageFormat.format("Result {0} and {0}!", 14, 42); // Noncompliant {{2nd argument is not used.}}
    MessageFormat.format("Result {0, number, integer} and {1, number, integer}!", 14, 42); // compliant
    MessageFormat.format("Result {0} and {1}!", 14, 42, 128); // Noncompliant {{3rd argument is not used.}}
    MessageFormat.format("{0,number,#.#}{1}", new Object[] {0.07, "$"}); // Compliant
    MessageFormat.format("{0,number,#.#}{1}", new Object[] {0.07});  // Noncompliant {{Not enough arguments.}}
    MessageFormat.format("{0,number,#.#}{1}", objs); // Compliant - skipped as the array is not initialized in the method invocation
    MessageFormat.format("{0,number,#.#}{1}", new Object[42]); // Compliant - Not considered
    MessageFormat.format("value=\"'{'{0}'}'{1}\"", new Object[] {"value 1", "value 2"});
    MessageFormat.format("value=\"{0}'{'{1}'}'\"", new Object[] {"value 1", "value 2"});
    MessageFormat.format("Result {0}", new Exception()); // Compliant
    MessageFormat.format("Result {0}", 1,  new Exception()); // Noncompliant {{2nd argument is not used.}}
    MessageFormat.format("Result {0}", new Exception().toString()); // Compliant
    MessageFormat.format("Result {0} {1}", 1,  new Exception()); // Compliant
    MessageFormat.format("Result {0} {1}", new Exception(), 1); // Compliant
    MessageFormat.format("Result {0} {1}", 1, 2,  new Exception()); // Noncompliant {{3rd argument is not used.}}
    MessageFormat.format("Result {0} {1}", new Exception()); // Noncompliant {{Not enough arguments.}}

    // LOGGERS =========================================================================================================
    // java.util.logging.Logger ========================================================================================
    java.util.logging.Logger logger = java.util.logging.Logger.getLogger("");
    logger.log(java.util.logging.Level.SEVERE, "{0,number,$'#',##}", value); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "Result ''{0}''.", 14); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "Result '{0}'", 14); // Noncompliant {{String contains no format specifiers.}}
    logger.log(java.util.logging.Level.SEVERE, "Result ' {0}", 14); // Noncompliant {{Single quote "'" must be escaped.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {0}!", myObject.toString()); // Noncompliant {{No need to call "toString()" method as formatting and string conversion is done by the Formatter.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {0}!", myObject.hashCode()); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "Result yeah!", 14); // Noncompliant {{String contains no format specifiers.}}
    logger.log(java.util.logging.Level.SEVERE, "Result yeah!", new Exception()); // compliant, throwable parameter
    logger.log(java.util.logging.Level.SEVERE, "message", new Object[] {new Exception()}); // Noncompliant {{String contains no format specifiers.}}
    logger.log(java.util.logging.Level.SEVERE, "message {0}", new Object[] {new Exception()}); // Compliant, exceptions are not removed from argument list
    logger.log(java.util.logging.Level.SEVERE, "message {0}", new Object[] {1, new Exception()}); // Noncompliant {{2nd argument is not used.}}
    logger.log(java.util.logging.Level.SEVERE, "message {0} {1}", new Object[] {1, new Exception()}); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "message {0} {1}", new Object[] {1, 2, new Exception()}); // Noncompliant {{3rd argument is not used.}}
    logger.log(java.util.logging.Level.SEVERE, "message {0} {1}", new Object[] {new Exception()}); // Noncompliant {{Not enough arguments.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {0}", new Exception()); // Noncompliant {{Not enough arguments.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {0}"); // Noncompliant {{Not enough arguments.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {1}"); // Noncompliant {{Not enough arguments.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {1}", new Integer[]{14}); // Noncompliant {{Not enough arguments.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {0} and {1}!", 14); // Noncompliant {{Not enough arguments.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {0} and {1}!", new Integer[]{14, 18}); // compliant
    logger.log(java.util.logging.Level.SEVERE, "Result {0} and {1}!", new Integer[]{14, 18, 12}); // Noncompliant {{3rd argument is not used.}}
    logger.log(java.util.logging.Level.SEVERE, "{0,number,#.#}{1}", new Object[] {0.07, "$"}); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "{0,number,#.#}{1}", new Object[] {0.07}); // Noncompliant {{Not enough arguments.}}

    logger.log(java.util.logging.Level.SEVERE, "{0,number,#.#}{1}", objs); // Compliant - skipped as the array is not initialized in the method invocation
    logger.log(java.util.logging.Level.SEVERE, "{0,number,#.#}{1}", new Object[42]); // Compliant - Not considered
    logger.log(java.util.logging.Level.SEVERE, "value=\"'{'{0}'}'{1}\"", new Object[] {"value 1", "value 2"});
    logger.log(java.util.logging.Level.SEVERE, "value=\"{0}'{'{1}'}'\"", new Object[] {"value 1", "value 2"});
    logger.log(java.util.logging.Level.SEVERE, "Result {0}!", new Object[] {myObject.toString()}); // Noncompliant {{No need to call "toString()" method as formatting and string conversion is done by the Formatter.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {0}!", new String[] {myObject.toString()}); // Noncompliant {{No need to call "toString()" method since an array of Objects can be used here.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {0} {1}!", new Object[] {myObject.toString(), myObject}); // Noncompliant {{No need to call "toString()" method as formatting and string conversion is done by the Formatter.}}

    logger.log(java.util.logging.Level.SEVERE, "message " + value); // Noncompliant {{Format specifiers or lambda should be used instead of string concatenation.}}
    logger.log(java.util.logging.Level.SEVERE, "message " + value, new Exception()); // Noncompliant {{Lambda should be used to defer string concatenation.}}
    logger.log(java.util.logging.Level.SEVERE, new Exception(), () -> "Result " + value); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "message {0} " + value, value); // Noncompliant {{Format specifiers or lambda should be used instead of string concatenation.}}

    java.util.logging.Logger logger2 = java.util.logging.Logger.getLogger("som.foo", "som.foo.resources.i18n.LogMessages");
    logger2.log(java.util.logging.Level.WARNING, "som.foo.errorcode", 404);
    getLog().log(java.util.logging.Level.WARNING, "som.foo.errorcode", 404); // Noncompliant

    java.util.logging.Logger logger3 = java.util.logging.Logger.getLogger("");
    logger3.log(java.util.logging.Level.WARNING, "som.foo.errorcode", 404); // Noncompliant

    java.util.logging.Logger logger4 = getLog();
    logger4.log(java.util.logging.Level.WARNING, "som.foo.errorcode", 404); // Noncompliant
    this.loggerField.log(java.util.logging.Level.WARNING, "som.foo.errorcode", 404);
    String param1 = "p1";
    String param2 = "p2";
    String param3 = "p3";
    java.util.logging.Level level = java.util.logging.Level.WARNING;
    logger4.log(level, () -> "message 01 " + param1);
    logger4.log(level, new Exception(), () -> "message 02 " + param1);
    logger4.log(level, "message ");
    logger4.log(level, "message ", new Exception());
    logger4.log(level, "message {0}", param1);
    logger4.log(level, "message {1}", param1);  // Noncompliant {{Not enough arguments.}}
    logger4.log(level, "message {0}", new Exception());  // Noncompliant {{Not enough arguments.}}
    logger4.log(level, "message {0}", new Object[] {param1});
    logger4.log(level, "message {1}", new Object[] {param1});  // Noncompliant {{Not enough arguments.}}
    logger4.log(level, "message {0}", new Object[] {param1, new Exception()}); // Noncompliant {{2nd argument is not used.}}
    logger4.log(level, "message {0} {1}", new Object[] {param1, param2});
    logger4.log(level, "message {0} {1}", new Object[] {param1, param2, param3}); // Noncompliant {{3rd argument is not used.}}
    logger4.log(level, "message " + param1); // Noncompliant {{Format specifiers or lambda should be used instead of string concatenation.}}
    logger4.log(level, "message " + "...");
    logger4.log(level, "message " + param1, new Exception()); // Noncompliant {{Lambda should be used to defer string concatenation.}}

    logger4.log(level, "Can't load library \"{0}\"!", "foo"); // Noncompliant {{Single quote "'" must be escaped.}}
    logger4.log(level, "Can''t load library \"{0}\"!", "foo"); // Compliant, escaping the single quote with ''
    logger4.log(level, "Can't load library \"\"!", new Throwable()); // Compliant, will print: Can't load library ""!
    logger4.log(level, "Can't load library \"\"!"); // Compliant, will print: Can't load library ""!

    // slf4jLog ========================================================================================================
    // slf4jLog is a facade, various logging frameworks can be used under it. It implies that we will only report issues when
    // there are obvious mistakes, not when it depends on the underlying framework (even if it works correctly with the common one).
    org.slf4j.Logger slf4jLog = org.slf4j.LoggerFactory.getLogger("");
    org.slf4j.Marker marker = org.slf4j.MarkerFactory.getMarker("");

    slf4jLog.debug(marker, "message {}"); // Noncompliant {{Not enough arguments.}}
    slf4jLog.debug(marker, "message ", 1); // Noncompliant {{String contains no format specifiers.}}
    slf4jLog.debug(marker, "message {}", 1);
    slf4jLog.debug(marker, "message {} - {}", 1, 2);
    slf4jLog.debug(marker, "message {}", 1, 2);// Noncompliant {{2nd argument is not used.}}
    slf4jLog.debug(marker, "message {} {} {}", 1, 2, 3);
    slf4jLog.debug(marker, "message {} {}", 1, 2, 3); // Noncompliant
    slf4jLog.debug(marker, "message {} {}", new Object[]{1, 2, 3}); // Noncompliant
    slf4jLog.debug(marker, "message {} {} {}", new Object[]{1, 2, 3}); // compliant
    slf4jLog.debug(marker, "message {} {} {}", new Object[]{1, 2}); // Noncompliant {{Not enough arguments.}}
    slf4jLog.debug(marker, "message ", new Exception());
    slf4jLog.debug(marker, "message {}", new Exception()); // Noncompliant {{Not enough arguments.}}
    slf4jLog.debug(marker, "message {}", new Exception().toString());

    slf4jLog.debug("message {}"); // Noncompliant {{Not enough arguments.}}
    slf4jLog.debug("message ", 1); // Noncompliant {{String contains no format specifiers.}}
    slf4jLog.debug("message {}", 1);
    slf4jLog.debug("message {} - {}", 1, 2);
    slf4jLog.debug("message {}", 1, 2);// Noncompliant {{2nd argument is not used.}}
    slf4jLog.debug("message {} {} {}", 1, 2, 3);
    slf4jLog.debug("message {} {}", 1, 2, 3); // Noncompliant
    slf4jLog.debug("message {} {}", new Object[]{1, 2, 3}); // Noncompliant
    slf4jLog.debug("message {} {} {}", new Object[]{1, 2, 3}); // compliant
    slf4jLog.debug("message ", new Exception());
    slf4jLog.debug("message {}", new Exception()); // Noncompliant {{Not enough arguments.}}
    slf4jLog.debug("message {}", new Exception().toString());

    slf4jLog.error("message {}"); // Noncompliant {{Not enough arguments.}}
    slf4jLog.error("message {}", new Exception()); // Noncompliant {{Not enough arguments.}}
    slf4jLog.error("message {}", new Exception().toString());
    slf4jLog.error("message {}", 1, new Exception()); // Compliant
    slf4jLog.error("message ", 1); // Noncompliant {{String contains no format specifiers.}}
    slf4jLog.error("message {}", 1);
    slf4jLog.error("message {}", 1, 2); // Noncompliant {{2nd argument is not used.}}
    slf4jLog.error("message ", new Exception());
    slf4jLog.error("message {} {}", 1); // Noncompliant {{Not enough arguments.}}
    slf4jLog.error("message {} {}", 1, new Exception()); // Compliant, only a problem when we have only one throwable argument.
    slf4jLog.error("message {} {}", 1, new Exception().toString());
    slf4jLog.error("message {} {}", 1, 2, 3); // Noncompliant {{3rd argument is not used.}}
    slf4jLog.error("message {} {}", 1, 2, new Exception().toString()); // Noncompliant {{3rd argument is not used.}}
    slf4jLog.error("message {} {}", 1, new Exception()); // Compliant
    slf4jLog.error("message {} {}", 1, 2, new Exception()); // Compliant
    slf4jLog.error("message {} {} {}", 1, new Exception()); // Noncompliant {{Not enough arguments.}}
    slf4jLog.error("message {} {} {}", 1, 2, new Exception()); // Compliant
    slf4jLog.error("message {} {} {}", 1, 2, 3, new Exception()); // Compliant
    slf4jLog.error("message {} {} {}", 1, 2, new Exception(), 3); // Noncompliant {{4th argument is not used.}}
    slf4jLog.error("message {} {} {}", 1, 2, 3, 4, new Exception()); // Noncompliant {{4th argument is not used.}}

    slf4jLog.info("message {}", new Object[] {new Exception()}); // Noncompliant {{Not enough arguments.}}
    slf4jLog.info("message {}", new Object[] {1, new Exception()}); // Compliant
    slf4jLog.info("message {}", new Object[] {new Exception(), 1}); // Noncompliant {{2nd argument is not used.}}
    slf4jLog.info("message {} {}", new Object[] {1, new Exception()}); // Compliant
    slf4jLog.info("message {} {}", new Object[] {1, 2, new Exception()}); // Compliant
    slf4jLog.info("message {} {}", new Object[] {1, 2, 3, new Exception()}); // Noncompliant {{3rd argument is not used.}}

    try {
    } catch (Exception e) {
      org.slf4j.LoggerFactory.getLogger(PrintfMisuseCheckSample.class).error("there is an error", e);
    }

    slf4jLog.info("message {} - {}", 1, 2);
    slf4jLog.info("message {}", 1, 2);// Noncompliant {{2nd argument is not used.}}
    slf4jLog.info("message {} {} {}", 1, 2, 3);
    slf4jLog.info("message ", new Exception());
    slf4jLog.info("message {}", new Exception()); // Noncompliant {{Not enough arguments.}}

    slf4jLog.trace("message {} {}", 1, 2, 3); // Noncompliant
    slf4jLog.trace("message {} {}", new Object[]{1, 2, 3}); // Noncompliant
    slf4jLog.trace("message {} {} {}", new Object[]{1, 2, 3}); // compliant
    slf4jLog.trace("message ", new Exception());
    slf4jLog.trace("message {}", new Exception()); // Noncompliant {{Not enough arguments.}}

    slf4jLog.warn("message {}"); // Noncompliant {{Not enough arguments.}}
    slf4jLog.warn("message ", 1); // Noncompliant {{String contains no format specifiers.}}
    slf4jLog.warn("message {}", 1);
    slf4jLog.warn("message");
    slf4jLog.warn("message ", new Exception());
    slf4jLog.warn("message {}", new Exception()); // Noncompliant {{Not enough arguments.}}

    slf4jLog.error("message: " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    slf4jLog.error("message: {}" + value, value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    slf4jLog.error("message:" + value, new Exception()); // Compliant, using error(String, Object, Object) would compile, but no guarantee that
    // the last argument will be treated as a Throwable
    slf4jLog.error("message: {}", value, new Exception()); // Compliant, but may not print the stack trace depending on the underlying framework

    // log4j ===========================================================================================================
    org.apache.logging.log4j.Logger log4j = org.apache.logging.log4j.LogManager.getLogger();
    org.apache.logging.log4j.Logger formatterLogger = LogManager.getFormatterLogger();

    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message");  // Compliant
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message", 1);  // Noncompliant {{String contains no format specifiers.}}
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message {}");  // Noncompliant {{Not enough arguments.}}
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message {}", new Exception());  // Noncompliant {{Not enough arguments.}}
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message {}", 1);  // Compliant
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message {} {}", 1);  // Noncompliant {{Not enough arguments.}}
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message {}", 1, "hello");  // Noncompliant
    formatterLogger.log(org.apache.logging.log4j.Level.DEBUG, "message %d", 1);  // Compliant
    formatterLogger.log(org.apache.logging.log4j.Level.DEBUG, "message %d", 1, "hello");  // Noncompliant

    log4j.debug("message"); // Compliant
    log4j.debug("message", 1); // Noncompliant
    log4j.error("message", 1); // Noncompliant
    log4j.fatal("message", 1); // Noncompliant
    log4j.info("message", 1); // Noncompliant
    log4j.trace("message", 1); // Noncompliant
    log4j.warn("message", 1); // Noncompliant
    log4j.warn("message {}", 1, 2); // Noncompliant {{2nd argument is not used.}}
    log4j.fatal("message {} {}", 1); // Noncompliant
    log4j.info("message {} {}", 1); // Noncompliant
    log4j.trace("message {} {}", 1); // Noncompliant
    log4j.warn("message {} {}", 1); // Noncompliant
    log4j.warn("message {} {} {}", 1, 2); // Noncompliant {{Not enough arguments.}}

    log4j.debug("value is " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}

    log4j.debug(() -> "hello"); // Compliant
    log4j.debug("message {}", 1); // Compliant
    log4j.debug("message %s", 1); // false-negative, the rule assumes it's a "FormatterLogger"
    log4j.debug("message {}", () -> 1); // Compliant
    formatterLogger.debug("message %s message %d", "hello", 42); // Compliant
    formatterLogger.debug("message %s message", "hello", 42); // Noncompliant {{2nd argument is not used.}}
    formatterLogger.debug("message %s {} message", "hello", 42); // Noncompliant {{2nd argument is not used.}}
    formatterLogger.debug("message %s {} %s message", "hello", 42); // Noncompliant - false-positive, the rule doesn't know it's a "FormatterLogger"
    formatterLogger.printf(org.apache.logging.log4j.Level.DEBUG, "message %s {} %s message", "hello", 42);
    formatterLogger.debug("message %s {} {} message", "hello", 42); // false-negative, the rule doesn't know it's a "FormatterLogger"
    formatterLogger.printf(org.apache.logging.log4j.Level.DEBUG, "message %s {} {} message", "hello", 42); // Noncompliant {{2nd argument is not used.}}

    log4j.printf(org.apache.logging.log4j.Level.DEBUG, "message"); // Noncompliant {{String contains no format specifiers.}} 
    log4j.printf(org.apache.logging.log4j.Level.DEBUG, "message %s %d", "hello", 42); // Compliant - Java formatters
    log4j.printf(org.apache.logging.log4j.Level.DEBUG, "message %s", "hello", 42); // Noncompliant {{2nd argument is not used.}}
    formatterLogger.printf(org.apache.logging.log4j.Level.DEBUG, "message %s", "hello", 42); // Noncompliant {{2nd argument is not used.}}

    log4j.error("message {}"); // Noncompliant {{Not enough arguments.}}
    log4j.error("message {}", new Exception()); // Noncompliant {{Not enough arguments.}}
    log4j.error("message ", 1); // Noncompliant {{String contains no format specifiers.}}
    log4j.error("message {}", 1);
    log4j.error("message {}", 1, 2); // Noncompliant {{2nd argument is not used.}}
    log4j.error("message ", new Exception());
    log4j.error("message {}", new Exception()); // Noncompliant {{Not enough arguments.}}
    log4j.error("message {}", new Exception().toString());
    log4j.error("message {} {}", 1); // Noncompliant {{Not enough arguments.}}
    log4j.error("message {} {}", 1, new Exception());  // Compliant, only a problem when we have one throwable argument.
    log4j.error("message {} {}", 1, new Exception().toString());
    log4j.error("message {} {}", 1, 2, 3); // Noncompliant {{3rd argument is not used.}}
    log4j.error("message {} {}", 1, 2, new Exception().toString()); // Noncompliant {{3rd argument is not used.}}
    log4j.error("message {} {}", 1, 2, new Exception()); // Compliant
    log4j.error("message {} {} {}", 1, 2, new Exception()); // Compliant
    log4j.error("message {} {} {}", 1, 2, 3, new Exception()); // Compliant
    log4j.error("message ", () -> 1); // Noncompliant {{String contains no format specifiers.}}
    log4j.error("message {}", () -> 1);
    log4j.error("message {} {}", () -> 1); // Noncompliant {{Not enough arguments.}}
    log4j.error(() -> "message " + param1);
    log4j.error(() -> "message " + param1, new Exception());

    log4j.error("message {}", new Object[] {new Exception()}); // Noncompliant {{Not enough arguments.}}
    log4j.error("message {}", new Object[] {1, new Exception()}); // Compliant
    log4j.error("message {}", new Object[] {new Exception(), 1}); // Noncompliant {{2nd argument is not used.}}
    log4j.error("message {} {}", new Object[] {1, new Exception()}); // Compliant
    log4j.error("message {} {}", new Object[] {1, 2, new Exception()}); // Compliant
    log4j.error("message {} {}", new Object[] {1, 2, 3, new Exception()}); // Noncompliant {{3rd argument is not used.}}

    log4j.debug(() -> "hello"); // Compliant
    log4j.debug("message {}", 1); // Compliant
    log4j.debug("message {}", () -> 1); // Compliant
    log4j.debug("message %s message %d", "hello", "world"); // Noncompliant {{An 'int' is expected rather than a String.}}
    log4j.debug("message %s message %d %s", "hello", 42); // Noncompliant {{Not enough arguments.}}

    log4j.printf(org.apache.logging.log4j.Level.DEBUG, "message %s %d", "hello", 42); // Compliant - Java formatters

    log4j.error("message: " + value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    log4j.error("message: {}" + value, value); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    log4j.error("message:" + value, new Exception()); // Noncompliant {{Format specifiers should be used instead of string concatenation.}}
    log4j.error("message: {}", value, new Exception()); // Compliant, print the stack-trace as expected
  }

  private java.util.logging.Logger getLog() {
    return null;
  }
}

class sonarjava3044 {
  void foo(org.slf4j.Logger log, org.slf4j.Marker marker) {
    log.warn(marker, "message");
    log.error(marker, "message");
    log.info(marker, "message");
    log.debug(marker, "message");
    log.debug("message");
  }
}
