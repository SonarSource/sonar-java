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
    String.format("Too many arguments %d and %d and %d", 1, 2, 3, 4);
    String.format("normal %d%% ", 1);  //Compliant
    String.format("Duke's Birthday year is %t", 12l);  // Noncompliant {{Time conversion requires a second character.}}
    String.format("Duke's Birthday year is %tH", 12l);  // Compliant
    String.format("Duke's Birthday year is %tH", Long.valueOf(12L));  // Compliant
    String.format("Duke's Birthday year is %tH", loc);  // Noncompliant {{Time argument is expected (long, Long, Calendar, Date and TemporalAccessor).}}
    String.format("%08d%n", 1);
    GregorianCalendar gc;
    String.format("Duke's Birthday year is %tH", gc);
    // Noncompliant@+1
    String.format("Duke's Birthday year is %t", loc);  // Noncompliant
    String.format("Accessed before %tF%n", java.time.LocalDate.now()); // Compliant
    System.out.printf("%1$ty_%1$tm_%1$td_%1$tH_%1$tM_%1$tS", java.time.LocalDateTime.now()); // Compliant

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
    MessageFormat.format("Result '{0}'", 14);
    MessageFormat.format("Result ' {0}", 14); // Noncompliant {{Single quote "'" must be escaped.}}
    MessageFormat.format("Result {{{0}}.", 14); // Noncompliant {{Single left curly braces "{" must be escaped.}}
    MessageFormat.format("Result {0}!", myObject.toString());
    MessageFormat.format("Result {0}!", myObject.hashCode()); // Compliant
    MessageFormat.format("Result yeah!", 14);
    MessageFormat.format("Result {1}!", 14); // Noncompliant {{Not enough arguments.}}
    MessageFormat.format("Result {0} and {1}!", 14); // Noncompliant {{Not enough arguments.}}
    MessageFormat.format("Result {0} and {0}!", 14, 42);
    MessageFormat.format("Result {0, number, integer} and {1, number, integer}!", 14, 42); // compliant
    MessageFormat.format("Result {0} and {1}!", 14, 42, 128);
    MessageFormat.format("{0,number,#.#}{1}", new Object[] {0.07, "$"}); // Compliant
    MessageFormat.format("{0,number,#.#}{1}", new Object[] {0.07}); // Noncompliant {{Not enough arguments.}}
    MessageFormat.format("{0,number,#.#}{1}", objs); // Compliant - skipped as the array is not initialized in the method invocation
    MessageFormat.format("{0,number,#.#}{1}", new Object[42]); // Compliant - Not considered
    MessageFormat.format("value=\"'{'{0}'}'{1}\"", new Object[] {"value 1", "value 2"});
    MessageFormat.format("value=\"{0}'{'{1}'}'\"", new Object[] {"value 1", "value 2"});

    java.util.logging.Logger logger;
    logger.log(java.util.logging.Level.SEVERE, "{0,number,$'#',##}", value); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "Result ''{0}''.", 14); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "Result '{0}'", 14);
    logger.log(java.util.logging.Level.SEVERE, "Result ' {0}", 14); // Noncompliant {{Single quote "'" must be escaped.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {{{0}}.", 14); // Noncompliant {{Single left curly braces "{" must be escaped.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {0}!", myObject.toString());
    logger.log(java.util.logging.Level.SEVERE, "Result {0}!", myObject.hashCode()); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "Result yeah!", 14);
    logger.log(java.util.logging.Level.SEVERE, "Result yeah!", new Exception()); // compliant, throwable parameter
    logger.log(java.util.logging.Level.SEVERE, "Result {1}!", 14); // Noncompliant {{Not enough arguments.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {0} and {1}!", 14); // Noncompliant {{Not enough arguments.}}
    logger.log(java.util.logging.Level.SEVERE, "Result {0} and {1}!", new String[]{14,18}); // compliant
    logger.log(java.util.logging.Level.SEVERE, "Result {0} and {1}!", new String[]{14,18, 12}); // compliant
    logger.log(java.util.logging.Level.SEVERE, "{0,number,#.#}{1}", new Object[] {0.07, "$"}); // Compliant
    logger.log(java.util.logging.Level.SEVERE, "{0,number,#.#}{1}", new Object[] {0.07}); // Noncompliant {{Not enough arguments.}}
    logger.log(java.util.logging.Level.SEVERE, "{0,number,#.#}{1}", objs); // Compliant - skipped as the array is not initialized in the method invocation
    logger.log(java.util.logging.Level.SEVERE, "{0,number,#.#}{1}", new Object[42]); // Compliant - Not considered
    logger.log(java.util.logging.Level.SEVERE, "value=\"'{'{0}'}'{1}\"", new Object[] {"value 1", "value 2"});
    logger.log(java.util.logging.Level.SEVERE, "value=\"{0}'{'{1}'}'\"", new Object[] {"value 1", "value 2"});

    org.slf4j.Logger slf4jLog;
    org.slf4j.Marker marker;

    slf4jLog.debug(marker, "message {}"); // Noncompliant {{Not enough arguments.}}
    slf4jLog.debug(marker, "message ", 1);
    slf4jLog.debug(marker, "message {}", 1);
    slf4jLog.debug(marker, "message {} - {}", 1, 2);
    slf4jLog.debug(marker, "message {}", 1, 2);
    slf4jLog.debug(marker, "message {} {} {}", 1, 2, 3);
    slf4jLog.debug(marker, "message {} {}", 1, 2, 3);
    slf4jLog.debug(marker, "message {} {}", new Object[]{1, 2, 3});
    slf4jLog.debug(marker, "message {} {} {}", new Object[]{1, 2, 3});
    slf4jLog.debug(marker, "message {} {} {}", new Object[]{1, 2}); // Noncompliant {{Not enough arguments.}}
    slf4jLog.debug(marker, "message ", new Exception());
    slf4jLog.debug(marker, "message {}", new Exception());


    slf4jLog.debug("message {}"); // Noncompliant {{Not enough arguments.}}
    slf4jLog.debug("message ", 1);
    slf4jLog.debug("message {}", 1);
    slf4jLog.debug("message {} - {}", 1, 2);
    slf4jLog.debug("message {}", 1, 2);
    slf4jLog.debug("message {} {} {}", 1, 2, 3);
    slf4jLog.debug("message {} {}", 1, 2, 3);
    slf4jLog.debug("message {} {}", new Object[]{1, 2, 3});
    slf4jLog.debug("message {} {} {}", new Object[]{1, 2, 3});
    slf4jLog.debug("message ", new Exception());
    slf4jLog.debug("message {}", new Exception());

    slf4jLog.error("message {}"); // Noncompliant {{Not enough arguments.}}
    slf4jLog.error("message ", 1);
    slf4jLog.error("message {}", 1);
    slf4jLog.info("message {} - {}", 1, 2);
    slf4jLog.info("message {}", 1, 2);
    slf4jLog.info("message {} {} {}", 1, 2, 3);
    slf4jLog.trace("message {} {}", 1, 2, 3);
    slf4jLog.trace("message {} {}", new Object[]{1, 2, 3});
    slf4jLog.trace("message {} {} {}", new Object[]{1, 2, 3});
    slf4jLog.trace("message ", new Exception());
    slf4jLog.trace("message {}", new Exception());
    slf4jLog.warn("message {}"); // Noncompliant {{Not enough arguments.}}
    slf4jLog.warn("message ", 1);
    slf4jLog.warn("message {}", 1);
    slf4jLog.warn("Output on the error channel detected: this is probably due to a problem on pylint's side.");
    String fileKey;
    slf4jLog.warn("The resource for '{}' is not found, drilling down to the details of this test won't be possible", fileKey);
    slf4jLog.warn("The resource for is not found, drilling down to the details of this test won't be possible");

    org.apache.logging.log4j.Logger log4j = org.apache.logging.log4j.LogManager.getLogger();
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message");  // Compliant
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message {}");  // Noncompliant {{Not enough arguments.}}
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message {}", 1);  // Compliant
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message {} {}", 1);  // Noncompliant
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message %d", 1);  // Compliant
    log4j.log(org.apache.logging.log4j.Level.DEBUG, "message %d %s", 1, "hello");  // Compliant

    log4j.debug("message"); // Compliant
    log4j.debug("message {} {}", 1); // Noncompliant
    log4j.error("message {} {}", 1); // Noncompliant
    log4j.fatal("message {} {}", 1); // Noncompliant
    log4j.info("message {} {}", 1); // Noncompliant
    log4j.trace("message {} {}", 1); // Noncompliant
    log4j.warn("message {} {}", 1); // Noncompliant
    log4j.warn("message {} {} {}", 1, 2); // Noncompliant {{Not enough arguments.}}

    log4j.debug(() -> "hello"); // Compliant
    log4j.debug("message {}", 1); // Compliant
    log4j.debug("message {}", () -> 1); // Compliant
    log4j.debug("message %s message %d", "hello", "world"); // Noncompliant {{An 'int' is expected rather than a String.}}
    log4j.debug("message %s message %d %s", "hello", 42); // Noncompliant {{Not enough arguments.}}

    log4j.printf(org.apache.logging.log4j.Level.DEBUG, "message %s %d", "hello", 42); // Compliant - Java formatters
  }
}

class UsingLambda {

  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(UsingLambda.class);

  void start(int port) {

    unknown((a, b) -> {
      LOG.info(a.foo());
    });

  }
}
