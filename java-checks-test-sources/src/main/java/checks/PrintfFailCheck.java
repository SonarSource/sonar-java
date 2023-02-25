package checks;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.stream.IntStream;

public class PrintfFailCheck {
  void foo(Calendar c) throws java.io.IOException {
    Object myObject = new Object();
    Object[] objs = new Object[]{14};
    Locale loc = Locale.US;
    // String format ===================================================================================================
    double value = 1.0;
    String.format("The value of my integer is %d", "Hello World");  // Noncompliant {{An 'int' is expected rather than a String.}}
    String.format("First {0} and then {1}", "foo", "bar");
    String.format("Duke's Birthday year is %tX", 12l);  // Noncompliant {{X is not a supported time conversion character}}
    String.format("Display %3$d and then %d", 1, 2, 3);
    String.format("Too many arguments %d and %d", 1, 2, 3);
    String.format("Not enough arguments %d and %d", 1);  // Noncompliant {{Not enough arguments.}}
    String.format("%1$d %2$d %9$-3.3s", 1, 2, "hello");  // Noncompliant {{Not enough arguments to feed formater at index 9: '%9$'.}}
    String.format("%12$s", 1, 2, "hello");  // Noncompliant {{Not enough arguments to feed formater at index 12: '%12$'.}}
    String.format("First Line\n %d", 1);
    String.format("First Line");
    String.format("First Line%%");
    String.format("First Line%n"); // Compliant
    String.format("%< is equals to %d", 2);   // Noncompliant {{The argument index '<' refers to the previous format specifier but there isn't one.}}
    String.format("Is myObject null ? %b", myObject);
    String.format("value is " + value); // Compliant
    String.format("string without arguments");
    String.format("%d %d", new Object[]{1,2}); // Compliant
    String.format("%d %d", new Object[]{1,2,3}); // Compliant, too many arguments but no errors, reported by S3457
    String.format("%d %d", new Object[]{1}); // Noncompliant {{Not enough arguments.}}
    String.format("%d %d", objs); // Compliant, not initialized inside the call
    String.format("%d %d", new Object[42]); // Compliant
    String.format("%d%d", IntStream.range(0, 2).mapToObj(Integer::valueOf).toArray()); // Compliant
    String.format("%d%d", IntStream.range(0, 1).mapToObj(Integer::valueOf).toArray()); // FN, acceptable to avoid noise
    String.format("%d%d", IntStream.range(0, 2).mapToObj(Integer::valueOf).toArray(Object[]::new)); // Compliant
    String.format("Result %s %s",new Exception(),new Exception(),new Exception()); // Compliant, reported by S3457
    String.format("Result %s %s",new Object[] {new Exception(),new Exception(),new Exception()}); // Compliant, reported by S3457
    String.format("Result %s %s",new Exception(),new Exception()); // Compliant
    String.format("Result %s %s",new Object[] {new Exception(),new Exception()}); // Compliant
    String.format("Result %s %s",new Exception()); // Noncompliant {{Not enough arguments.}}
    String.format("Result %s %s",new Object[] {new Exception()});  // Noncompliant {{Not enough arguments.}}
    String.format("%s " + value, new Exception()); // Compliant, reported by S3457
    String.format("%s " + value); // Compliant, reported by S3457

    String.format("Too many arguments %d and %d and %d", 1, 2, 3, 4);
    String.format("normal %d%% ", 1);  //Compliant
    String.format("Duke's Birthday year is %t", 12l);  // Noncompliant {{Time conversion requires a second character.}}
    String.format("Duke's Birthday year is %tH", 12l);  // Compliant
    String.format("Duke's Birthday year is %tH", Long.valueOf(12L));  // Compliant
    String.format("Duke's Birthday year is %tH", loc);  // Noncompliant {{Time argument is expected (long, Long, Calendar, Date and TemporalAccessor).}}
    String.format("%08d%n", 1);
    GregorianCalendar gc = (GregorianCalendar) GregorianCalendar.getInstance();
    String.format("Duke's Birthday year is %tH", gc);
    // Noncompliant@+1
    String.format("Duke's Birthday year is %t", loc);  // Noncompliant
    String.format("Accessed before %tF%n", java.time.LocalDate.now()); // Compliant
    System.out.printf("%1$ty_%1$tm_%1$td_%1$tH_%1$tM_%1$tS", java.time.LocalDateTime.now()); // Compliant

    String.format("Dude's Birthday: %1$tm %<te,%<tY", c); // Compliant
    String.format("Dude's Birthday: %1$tm %1$te,%1$tY", c); // Compliant
    String.format("log/protocol_%tY_%<tm_%<td_%<tH_%<tM_%<tS.zip", new java.util.Date());
    String.format("value is %d", value); // Compliant
    String.format("%0$s", "tmp"); // Compliant, reported by S3457

    String.format("%2147483648$g", 42.0);
    String.format("%2147483648g", 42.0);
    String.format("%.2147483648g", 42.0);

    // String.formatted ================================================================================================
    "The value of my integer is %d".formatted("Hello World");  // Noncompliant {{An 'int' is expected rather than a String.}}
    "First {0} and then {1}".formatted("foo", "bar");
    "Duke's Birthday year is %tX".formatted(12l);  // Noncompliant {{X is not a supported time conversion character}}
    "Display %3$d and then %d".formatted(1, 2, 3);
    "Too many arguments %d and %d".formatted(1, 2, 3);
    "Not enough arguments %d and %d".formatted(1);  // Noncompliant {{Not enough arguments.}}
    "%1$d %2$d %9$-3.3s".formatted(1, 2, "hello");  // Noncompliant {{Not enough arguments to feed formater at index 9: '%9$'.}}
    "%12$s".formatted(1, 2, "hello");  // Noncompliant {{Not enough arguments to feed formater at index 12: '%12$'.}}
    "First Line\n %d".formatted(1);
    "First Line".formatted();
    "First Line%%".formatted();
    "First Line%n".formatted(); // Compliant

    // Print Writer / Stream / Formatter ===============================================================================
    PrintWriter pr = new PrintWriter("file");
    PrintStream ps = new PrintStream("file");
    Formatter formatter = new Formatter();

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

    pr.format("string without arguments");
    pr.format(loc, "string without arguments");
    pr.printf("string without arguments");
    pr.printf(loc, "string without arguments");
    ps.format("string without arguments");
    ps.format(loc, "string without arguments");
    ps.printf("string without arguments");
    ps.printf(loc, "string without arguments");
    formatter.format("string without arguments");
    formatter.format(loc, "string without arguments");

    pr.format("value is " + value);
    pr.format(loc, "value is " + value);
    pr.printf("value is " + value);
    pr.printf(loc, "value is " + value);
    ps.format("value is " + value);
    ps.format(loc, "value is " + value);
    ps.printf("value is " + value);
    ps.printf(loc, "value is " + value);
    formatter.format("value is " + value);
    formatter.format(loc, "value is " + value);

    pr.format("value is "+"asd");
    pr.format("value is "+
        "asd"); // Compliant

    // MessageFormat ===================================================================================================
    MessageFormat messageFormat = new MessageFormat("{0}");
    messageFormat.format(new Object(), new StringBuffer(), new FieldPosition(0)); // Compliant - Not considered
    messageFormat.format(new Object()); // Compliant - Not considered
    messageFormat.format("");  // Compliant - Not considered

    MessageFormat.format("{0,number,$'#',##}", value); // Compliant
    MessageFormat.format("Result ''{0}''.", 14); // Compliant
    MessageFormat.format("Result '{0}'", 14);
    MessageFormat.format("Result ' {0}", 14); // Compliant, wrong string formatting but no error: will be reported by S3457
    MessageFormat.format("Result {{{0}}.", 14); // Noncompliant {{Single left curly braces "{" must be escaped.}}
    MessageFormat.format("Result {0}!", myObject.toString());
    MessageFormat.format("Result {0}!", myObject.hashCode()); // Compliant
    MessageFormat.format("Result yeah!", 14);
    MessageFormat.format("Result {1}!", 14); // Compliant, wrong string formatting but no error: will be reported by S3457
    MessageFormat.format("Result {0} and {1}!", 14); // Compliant, wrong string formatting but no error: will be reported by S3457
    MessageFormat.format("Result {0} and {0}!", 14, 42);
    MessageFormat.format("Result {0, number, integer} and {1, number, integer}!", 14, 42); // compliant
    MessageFormat.format("Result {0} and {1}!", 14, 42, 128);
    MessageFormat.format("{0,number,#.#}{1}", new Object[] {0.07, "$"}); // Compliant
    MessageFormat.format("{0,number,#.#}{1}", new Object[] {0.07}); // Compliant, wrong string formatting but no error: will be reported by S3457
    MessageFormat.format("{0,number,#.#}{1}", objs); // Compliant - skipped as the array is not initialized in the method invocation
    MessageFormat.format("{0,number,#.#}{1}", new Object[42]); // Compliant - Not considered
    MessageFormat.format("value=\"'{'{0}'}'{1}\"", new Object[] {"value 1", "value 2"});
    MessageFormat.format("value=\"{0}'{'{1}'}'\"", new Object[] {"value 1", "value 2"});

    // LOGGERS =========================================================================================================
    // java.util.logging.Logger ========================================================================================
    java.util.logging.Logger logger =  java.util.logging.Logger.getLogger("");
    logger.log(java.util.logging.Level.SEVERE, "Result {0}"); // Compliant, wrong string formatting but no error: will be reported by S3457
    logger.log(java.util.logging.Level.SEVERE, "Result {1}"); // Compliant, wrong string formatting but no error: will be reported by S3457
    logger.log(java.util.logging.Level.SEVERE, "{0,number,$'#',##}", value); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "Result ''{0}''.", 14); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "Result '{0}'", 14);
    logger.log(java.util.logging.Level.SEVERE, "Result ' {0}", 14); // Compliant, wrong string formatting but no error: will be reported by S3457
    logger.log(java.util.logging.Level.SEVERE, "Result {{{0}}.", 14); // Compliant, wrong string formatting but no error: will be reported by S3457
    logger.log(java.util.logging.Level.SEVERE, "Result {0}!", myObject.toString());
    logger.log(java.util.logging.Level.SEVERE, "Result {0}!", myObject.hashCode()); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "Result yeah!", 14);
    logger.log(java.util.logging.Level.SEVERE, "Result yeah!", new Exception()); // compliant, throwable parameter
    logger.log(java.util.logging.Level.SEVERE, "Result {1}!", 14); // Compliant, wrong string formatting but no error: will be reported by S3457
    logger.log(java.util.logging.Level.SEVERE, "Result {0}", new Exception()); // Compliant, wrong string formatting but no error: will be reported by S3457
    logger.log(java.util.logging.Level.SEVERE, "message {0}", new Object[] {new Exception()}); // Compliant, exceptions are not removed from argument list
    logger.log(java.util.logging.Level.SEVERE, "Result {1}", new Integer[]{14}); // Compliant, wrong string formatting but no error: will be reported by S3457
    logger.log(java.util.logging.Level.SEVERE, "Result {0} and {1}!", 14);// Compliant, wrong string formatting but no error: will be reported by S3457
    logger.log(java.util.logging.Level.SEVERE, "Result {0} and {1}!", new Integer[]{14, 18}); // compliant
    logger.log(java.util.logging.Level.SEVERE, "Result {0} and {1}!", new Integer[]{14, 18, 12}); // compliant
    logger.log(java.util.logging.Level.SEVERE, "{0,number,#.#}{1}", new Object[] {0.07, "$"}); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "{0,number,#.#}{1}", new Object[] {0.07}); // Compliant, wrong string formatting but no error: will be reported by S3457
    logger.log(java.util.logging.Level.SEVERE, "{0,number,#.#}{1}", objs); // Compliant - skipped as the array is not initialized in the method invocation
    logger.log(java.util.logging.Level.SEVERE, "{0,number,#.#}{1}", new Object[42]); // Compliant - Not considered
    logger.log(java.util.logging.Level.SEVERE, "value=\"'{'{0}'}'{1}\"", new Object[] {"value 1", "value 2"});
    logger.log(java.util.logging.Level.SEVERE, "value=\"{0}'{'{1}'}'\"", new Object[] {"value 1", "value 2"});

    logger.log(java.util.logging.Level.SEVERE, "message " + value); // Compliant, reported by S3457
    logger.log(java.util.logging.Level.SEVERE, "message " + value, new Exception()); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "message {0} " + value, value); // Compliant, reported by S3457
    logger.log(java.util.logging.Level.SEVERE, new Exception(), () -> "Result " + value); // Compliant

    // slf4jLog ========================================================================================================
    // slf4jLog is a facade, various logging frameworks can be used under it. It implies that we will only report issues when
    // there are obvious mistakes, not when it depends on the underlying framework (even if it works correctly with the common one).
    org.slf4j.Logger slf4jLog = org.slf4j.LoggerFactory.getLogger("");
    org.slf4j.Marker marker = org.slf4j.MarkerFactory.getMarker("");

    slf4jLog.debug(marker, "message {}"); // Compliant, wrong string formatting but no error: will be reported by S3457
    slf4jLog.debug(marker, "message ", 1);
    slf4jLog.debug(marker, "message {}", 1);
    slf4jLog.debug(marker, "message {} - {}", 1, 2);
    slf4jLog.debug(marker, "message {}", 1, 2);
    slf4jLog.debug(marker, "message {} {} {}", 1, 2, 3);
    slf4jLog.debug(marker, "message {} {}", 1, 2, 3);
    slf4jLog.debug(marker, "message {} {}", new Object[]{1, 2, 3});
    slf4jLog.debug(marker, "message {} {} {}", new Object[]{1, 2, 3});
    slf4jLog.debug(marker, "message {} {} {}", new Object[]{1, 2}); // Compliant, wrong string formatting but no error: will be reported by S3457
    slf4jLog.debug(marker, "message ", new Exception());
    slf4jLog.debug(marker, "message {}", new Exception()); // Compliant, wrong string formatting but no error: will be reported by S3457
    slf4jLog.debug(marker, "message {}", new Exception().toString());


    slf4jLog.debug("message {}"); // Compliant, wrong string formatting but no error: will be reported by S3457
    slf4jLog.debug("message ", 1);
    slf4jLog.debug("message {}", 1);
    slf4jLog.debug("message {} - {}", 1, 2);
    slf4jLog.debug("message {}", 1, 2);
    slf4jLog.debug("message {} {} {}", 1, 2, 3);
    slf4jLog.debug("message {} {}", 1, 2, 3);
    slf4jLog.debug("message {} {}", new Object[]{1, 2, 3});
    slf4jLog.debug("message {} {} {}", new Object[]{1, 2, 3});
    slf4jLog.debug("message ", new Exception());
    slf4jLog.debug("message {}", new Exception()); // Compliant, wrong string formatting but no error: will be reported by S3457
    slf4jLog.debug("message {}", new Exception().toString());

    slf4jLog.error("message {}"); // Compliant, wrong string formatting but no error: will be reported by S3457
    slf4jLog.error("message {}", new Exception()); // Compliant, wrong string formatting but no error: will be reported by S3457
    slf4jLog.error("message {}", new Exception().toString());
    slf4jLog.error("message ", 1);
    slf4jLog.error("message {}", 1);
    slf4jLog.error("message {} {}", 1); // Compliant, wrong string formatting but no error: will be reported by S3457
    slf4jLog.error("message {} {}", 1, new Exception()); // Compliant, wrong string formatting but no error: will be reported by S3457
    slf4jLog.error("message {} {}", 1, new Exception().toString());
    slf4jLog.info("message {} - {}", 1, 2);
    slf4jLog.info("message {}", 1, 2);
    slf4jLog.info("message {} {} {}", 1, 2, 3);
    slf4jLog.info("message {}", new Exception()); // Compliant, wrong string formatting but no error: will be reported by S3457
    slf4jLog.trace("message {} {}", 1, 2, 3);
    slf4jLog.trace("message {} {}", new Object[]{1, 2, 3});
    slf4jLog.trace("message {} {} {}", new Object[]{1, 2, 3});
    slf4jLog.trace("message ", new Exception());
    slf4jLog.trace("message {}", new Exception()); // Compliant, wrong string formatting but no error: will be reported by S3457
    slf4jLog.warn("message {}"); // Compliant, wrong string formatting but no error: will be reported by S3457
    slf4jLog.warn("message ", 1);
    slf4jLog.warn("message {}", 1);
    slf4jLog.warn("Output on the error channel detected: this is probably due to a problem on pylint's side.");
    slf4jLog.warn("message {}", new Exception()); // Compliant, wrong string formatting but no error: will be reported by S3457
    String fileKey = "key";
    slf4jLog.warn("The resource for '{}' is not found, drilling down to the details of this test won't be possible", fileKey);
    slf4jLog.warn("The resource for is not found, drilling down to the details of this test won't be possible");

    slf4jLog.error("message: " + value); // Compliant, reported by S3457
    slf4jLog.error("message: {}" + value, value); // Compliant, reported by S3457
    slf4jLog.error("message:" + value, new Exception()); // Compliant
    slf4jLog.error("message: {}", value, new Exception()); // Compliant

    // log4j ===========================================================================================================
    org.apache.logging.log4j.Logger log4j = org.apache.logging.log4j.LogManager.getLogger();
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message");  // Compliant
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message {}");  // Compliant, wrong string formatting but no error: will be reported by S3457
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message {}", new Exception());  // Compliant, wrong string formatting but no error: will be reported by S3457
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message {}", 1);  // Compliant
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message {} {}", 1);  // Compliant, wrong string formatting but no error: will be reported by S3457
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message %d", 1);  // Compliant
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message %d %s", 1, "hello");  // Compliant

    log4j.debug("message"); // Compliant
    log4j.debug("message {} {}", 1); // Compliant, wrong string formatting but no error: will be reported by S3457
    log4j.error("message {}", new Exception()); // Compliant, wrong string formatting but no error: will be reported by S3457
    log4j.error("message {} {}", 1, new Exception()); // Compliant, wrong string formatting but no error: will be reported by S3457
    log4j.error("message {} {}", 1, new Exception().toString());
    log4j.error("message {} {}", 1, 2, 3); // Compliant, detected by S3457 "3rd argument is not used."
    log4j.error("message {} {}", 1, 2, new Exception().toString()); // Compliant, detected by S3457 "3rd argument is not used."
    log4j.error("message ", () -> 1); // Compliant, detected by S3457 "String contains no format specifiers."
    log4j.error("message {}", () -> 1);
    log4j.error("message {} {}", () -> 1); // Compliant, wrong string formatting but no error: will be reported by S3457
    String param1 = "abc";
    log4j.error(() -> "message " + param1);
    log4j.error(() -> "message " + param1, new Exception());

    log4j.fatal("message {} {}", 1); // Compliant, wrong string formatting but no error: will be reported by S3457
    log4j.info("message {} {}", 1); // Compliant, wrong string formatting but no error: will be reported by S3457
    log4j.trace("message {} {}", 1); // Compliant, wrong string formatting but no error: will be reported by S3457
    log4j.warn("message {} {}", 1); // Compliant, wrong string formatting but no error: will be reported by S3457
    log4j.warn("message {} {} {}", 1, 2);  // Compliant, wrong string formatting but no error: will be reported by S3457

    log4j.debug(() -> "hello"); // Compliant
    log4j.debug("message {}", 1); // Compliant
    log4j.debug("message {}", () -> 1); // Compliant
    log4j.debug("message %s message %d", "hello", "world"); // Compliant, wrong string formatting but no error: will be reported by S3457
    log4j.debug("message %s message %d %s", "hello", 42); // Compliant, wrong string formatting but no error: will be reported by S3457

    log4j.printf(org.apache.logging.log4j.Level.DEBUG, "message %s %d", "hello", 42); // Compliant - Java formatters

    log4j.error("message: " + value); // Compliant, reported by S3457
    log4j.error("message: {}" + value, value); // Compliant, reported by S3457
    log4j.error("message:" + value, new Exception()); // Compliant, reported by S3457
    log4j.error("message: {}", value, new Exception()); // Compliant
  }
}
