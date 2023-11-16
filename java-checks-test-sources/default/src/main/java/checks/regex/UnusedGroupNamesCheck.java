package checks.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class UnusedGroupNamesCheck {

  private final Pattern p0 = Pattern.compile("(?<g1>[0-9]{2})");

  void noncompliant(String input) {

    Matcher m0 = p0.matcher(input);
    if (m0.matches()) {
      m0.group(1); // Noncompliant [[secondary=-6]] {{Directly use 'g1' instead of its group number.}}
      m0.start(1); // Noncompliant [[secondary=-7]] {{Directly use 'g1' instead of its group number.}}
      m0.end(1); // Noncompliant [[secondary=-8]] {{Directly use 'g1' instead of its group number.}}
    }

    Pattern p1 = Pattern
      .compile(
      "(?<g1>[0-9]+)"
        + ":"
        + "(?<g2>[0-9]+)");
    Matcher m1 = p1.matcher(input);
    if (m1.matches()) {
      m1.group("g3"); // Noncompliant [[secondary=-3,-5]] {{There is no group named 'g3' in the regular expression.}}
      m1.start("g3"); // Noncompliant [[secondary=-4,-6]] {{There is no group named 'g3' in the regular expression.}}
      m1.end("g3"); // Noncompliant [[secondary=-5,-7]] {{There is no group named 'g3' in the regular expression.}}
    }

    Matcher m2 = Pattern.compile(
      "(?<month>[0-9]{2})"
        + "/"
        + "(?<year>[0-9]{2})")
      .matcher(input);
    System.out.println(m2); // Printing the matcher does not count as the matcher escaping the scope because the parameter
                            // type of println is Object, not Matcher, so we can assume that it won't be used as a matcher
    new Bar(m2); // Same. Bar takes Object as its argument type
    if (m2.matches()) {
        m2.group(
          1 // Noncompliant [[secondary=-9]] {{Directly use 'month' instead of its group number.}}
        );
        m2.group(
          2 // Noncompliant [[secondary=-10]] {{Directly use 'year' instead of its group number.}}
        );
    }

    Pattern p3 = Pattern.compile(
      "(?<g1>[a-z]+)" // Noncompliant [[secondary=+0,+2,+4]] {{Use the named groups of this regex or remove the names.}}
        + ":"
        + "(?<g2>[0-9]+)"
        + "="
        + "(?<g3>[a-zA-Z0-9]+)");

    if (p3.matcher(input).matches()) {
      System.out.println(input);
    }

    if (input.matches("(?<name>test)")) { // Noncompliant {{Use the named groups of this regex or remove the names.}}
      System.out.println(input);
    }

    Pattern.matches("(?<group>[a-z])", input); // Noncompliant {{Use the named groups of this regex or remove the names.}}

    Pattern.compile("(?<group>[a-z])"); // Noncompliant {{Use the named groups of this regex or remove the names.}}
    Pattern.compile("(?<group>[a-z])").matcher(input).group(1); // Noncompliant {{Directly use 'group' instead of its group number.}}

    return;
  }

  public Pattern field = Pattern.compile("(?<group>[a-z])");
  public Pattern visibleFromOutsidePattern;
  private final Pattern[] patterns = new Pattern[1];

  Object compliant(String input, String groupName, int groupNumber) {
    Pattern invalid = Pattern.compile("[");

    visibleFromOutsidePattern = Pattern.compile("(?<group>[a-z])");
    patterns[0] = Pattern.compile("(?<group>[a-z])");

    Pattern p1 = Pattern.compile("(?<month>[0-9]{2})/(?<year>[0-9]{2})");
    Matcher m1 = p1.matcher(input);
    if (m1.matches()) {
      m1.group("month");
      m1.group("year");
    }

    Pattern p2 = Pattern.compile("(?<player1>[0-9]+):(?<player2>[0-9]+)");
    Matcher m2 = p2.matcher(input);
    if (m2.matches()) {
      m2.group("player1");
      m2.group("player2");
    }

    Pattern p3 = Pattern.compile("(?<group>[a-z])");
    Matcher m3 = p3.matcher(input);
    if (m3.matches()) {
      m3.group(groupNumber);
    }

    Pattern p4 = Pattern.compile("(?<g1>[a-z]):(?<g2>[a-z]):(?<g3>[a-z])");
    Matcher m4 = p4.matcher(input);
    if (m4.matches()) {
      m4.group(groupName);
    }

    Pattern p5 = Pattern.compile("(?<g1>[a-z]):\\k<g1>");
    Matcher m5 = p5.matcher(input);
    if (m5.matches()) {
      System.out.println("OK");
    }

    if (Math.random() > 0.5) {
      Pattern p6 = Pattern.compile("(?<group>[a-z])");
      Matcher m6 = p6.matcher(input);
      return m6;
    }

    if (Math.random() > 0.5) {
      Pattern p7 = Pattern.compile("(?<group>[a-z])");
      return p7;
    }

    Pattern p8 = Pattern.compile("(?<g1>[a-z])([0-9]{2})");
    Matcher m8 = p8.matcher(input);
    if (m8.matches()) {
      m8.group("g1");
      m8.group(2);
    }

    Pattern p9 = Pattern.compile("[0-9]{2}");
    Matcher m9 = p9.matcher(input);
    if (m9.matches()) {
      m9.group(0);
    }

    Pattern p10 = Pattern.compile("(?<g1>[0-9]{2})");
    if (Math.random() > 0.5) {
      p10 = Pattern.compile("(?<g2>[0-9]{2})");
    }
    Matcher m10 = p10.matcher(input);
    if (m10.matches()) {
      m10.group(1);
    }

    if (Math.random() > 0.5) {
      return this;
    }
    if (Math.random() > 0.5) {
      String str = "";
      return str;
    }

     Pattern p11 = Pattern.compile("\\\\u+[a-fA-F0-9]{4}");
     Matcher m11 = p11.matcher(input);
     m11.group(0);

    if (input.matches("(?<name>test)\\k<name>")) {
      System.out.println(input);
    }

    // When a pattern or matcher is passed to another method, we consider its group as used
    Pattern p12 = Pattern.compile("(?<name>test)");
    Matcher m12 = p12.matcher(input);
    someMethod(m12);

    Pattern p13 = Pattern.compile("(?<name>test)");
    someMethod(p13);

    someOtherMethod().group(1); // This should be ignored since we don't know which regex we're calling group for
    someOtherMethod().start("group"); // This should be ignored since we don't know which regex we're calling group for
    someOtherMethod().end(3); // This should be ignored since we don't know which regex we're calling group for

    Pattern p14 = Pattern.compile("(?<name>test)"); // Compliant because passed to constructor
    new Foo(p14);

    Pattern p15 = Pattern.compile("(?<month>[0-9]{2})/(?<year>[0-9]{2})");
    Matcher m15 = p15.matcher(input);
    if (m15.matches()) {
      m15.start("month");
      m15.start("year");
    }

    Pattern p16 = Pattern.compile("(?<price>\\d+(?:\\.\\d{2})?)CHF");
    Matcher m16 = p16.matcher(input);
    if (m16.matches()) {
      System.out.println(m16.end("price"));
    }

    // When patterns or matchers are directly passed to methods or constructors, they're considered as escaping the scope
    // even if the parameter type isn't Pattern/Matcher
    new Bar(Pattern.compile("(?<name>test)"));
    System.out.println(Pattern.compile("(?<name>test)").matcher(input));

    return null;
  }

  class Foo {
    Foo(Pattern p) {}
  }

  class Bar {
    Bar(Object o) {}
  }

  private enum BlockTag {
    RETURN(Pattern.compile("^@return(\\s++)?(?<descr>.+)?"), false); // Compliant because passed to constructor

    BlockTag(Pattern pattern, boolean patternWithName) {
    }
  }

  private void someMethod(Pattern p) {
    // Anything could be happening here - we don't know because we don't track patterns and matchers across methods
  }

  private void someMethod(Matcher m) {
    // Anything could be happening here - we don't know because we don't track patterns and matchers across methods
  }

  private Matcher someOtherMethod() {
    return null;
  }

  void replacement(String input) {
    Matcher m1 = Pattern.compile("a(?<foo>.)").matcher(input);
    m1.replaceAll("x${foo}"); // Compliant, foo is used

    Matcher m2 = Pattern.compile("a(?<foo>.)").matcher(input);
    m2.replaceAll("${foo}x${bar}"); // Noncompliant {{There is no group named 'bar' in the regular expression.}}

    Matcher m3 = Pattern.compile("a(?<bar>.)").matcher(input);
    m3.replaceFirst("x$1"); // Noncompliant {{Directly use '${bar}' instead of its group number.}}

    Matcher m4 = Pattern.compile("a(?<foo>.)").matcher(input);
    m4.replaceFirst("x${bar}x"); // Noncompliant {{There is no group named 'bar' in the regular expression.}}

    Matcher m5 = Pattern.compile("a(?<foo>.)").matcher(input);
    StringBuffer buffer = new StringBuffer();
    m5.appendReplacement(buffer, "${bar}x"); // Noncompliant {{There is no group named 'bar' in the regular expression.}}
    m5.appendReplacement(buffer, "x$1x"); // Noncompliant {{Directly use '${foo}' instead of its group number.}}

    Matcher m6 = Pattern.compile("a(.)(?<foo>.)").matcher(input);
    m6.appendReplacement(buffer, "$1x${foo}x$1"); // Compliant, the group 1 has no name
    m6.appendReplacement(buffer, "xx${bar}xx"); // Noncompliant {{There is no group named 'bar' in the regular expression.}}
    m6.appendReplacement(buffer, "\\${bar}xx"); // Compliant, dollar is escaped
    m6.appendReplacement(buffer, "$1x$2x${foo}"); // Noncompliant {{Directly use '${foo}' instead of its group number.}}
    m6.appendReplacement(buffer, "\\$2x${foo}"); // Compliant, dollar is escaped
    m6.appendReplacement(buffer, "${2}"); // Compliant, 2 is not a valid group name
  }

  static class UsingConstant {
    private static final Pattern CONSTANT_PATTERN = Pattern.compile("(?<group>[a-z])");

    java.util.Optional<String> extract(String value) {
      return java.util.Optional.of(CONSTANT_PATTERN.matcher(
        value))
        .filter(Matcher::matches)
        .map(match -> match.group("group")); // use "group"
    }
  }

  static class UsingFields {
    private static final Pattern CONSTANT_PATTERN = Pattern.compile("(?<group>[a-z])");
    private Matcher matcher;

    UsingFields(String value) {
      this.matcher = CONSTANT_PATTERN.matcher(value);
    }

    void useMatcher() {
      matcher.group("group"); // use "group"
    }
  }

  static class UsingFields2 {
    private static final Pattern CONSTANT_PATTERN = Pattern.compile("(?<group>[a-z])");
    private final Matcher matcher;

    UsingFields2(String value) {
      this.matcher = CONSTANT_PATTERN.matcher(value);
    }

    void useMatcher() {
      matcher.group(1); // Noncompliant [[secondary=-8]] {{Directly use 'group' instead of its group number.}}
    }
  }

  @org.hibernate.validator.constraints.URL(regexp = "(?<group>[a-z])") // Noncompliant
  String url;

}
