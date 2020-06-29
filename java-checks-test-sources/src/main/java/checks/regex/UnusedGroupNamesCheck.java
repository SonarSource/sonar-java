package checks.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract class UnusedGroupNamesCheck {

  private final Pattern p0 = Pattern.compile("(?<g1>[0-9]{2})");

  void noncompliant(String input) {

    Matcher m0 = p0.matcher(input);
    if (m0.matches()) {
      m0.group(1); // Noncompliant [[secondary=8]] {{Directly use 'g1' instead of its group number.}}
    }

    Pattern p1 = Pattern
      .compile(
      "(?<g1>[0-9]+)"
        + ":"
        + "(?<g2>[0-9]+)");
    Matcher m1 = p1.matcher(input);
    if (m1.matches()) {
      m1.group("g3"); // Noncompliant [[secondary=19,21]] {{There is no group named 'g3' in the regular expression.}}
    }

    Pattern p2 = Pattern.compile(
      "(?<month>[0-9]{2})"
        + "/"
        + "(?<year>[0-9]{2})");
    Matcher m2 = p2.matcher(input);
    if (m2.matches()) {
        m2.group(
          1 // Noncompliant [[secondary=28]] {{Directly use 'month' instead of its group number.}}
        );
        m2.group(
          2 // Noncompliant [[secondary=30]] {{Directly use 'year' instead of its group number.}}
        );
    }

    Pattern p3 = Pattern.compile(
      "(?<g1>[a-z]+)" // Noncompliant [[secondary=42,44,46]] {{Use the named groups of this regex or remove the names.}}
        + ":"
        + "(?<g2>[0-9]+)"
        + "="
        + "(?<g3>[a-zA-Z0-9]+)");
    Matcher m3 = p3.matcher(input);
    if (m3.matches()) {
      System.out.println(input);
    }

    return;
  }

  public Pattern field = Pattern.compile("(?<group>[a-z])");
  public Pattern visibleFromOutsidePattern;
  private final Pattern[] patterns = new Pattern[1];

  Object compliant(String input, String groupName, int groupNumber) {
    Pattern invalid = Pattern.compile("[");

    Pattern.matches("(?<group>[a-z])", input); // not passing through pattern and matchers

    Pattern.compile("(?<group>[a-z])"); // non-assigned
    Pattern.compile("(?<group>[a-z])").matcher(input).group(0); // no variable to follow

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

    return null;
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
      matcher.group(1); // Noncompliant [[secondary=175]] {{Directly use 'group' instead of its group number.}}
    }
  }

}
