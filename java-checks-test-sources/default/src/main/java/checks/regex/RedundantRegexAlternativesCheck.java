package checks.regex;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class RedundantRegexAlternativesCheck {

  public void f(Pattern pattern) {
    f(compile("" +
      "." +
      "|a")); // Noncompliant [[sc=9;ec=10;secondary=-1]] {{Remove or rework this redundant alternative.}}
    f(compile("" +
      "a" + // Noncompliant [[sc=8;ec=9;secondary=+1]]
      "|."));
    f(compile("" +
      "(.)" +
      "|(a)")); // Noncompliant [[sc=9;ec=12;secondary=-1]]
    f(compile("(a)|(.)")); // Compliant, the capturing group tells you which alternative was matched
    f(compile("a|(.)"));   // Compliant, the capturing group tells you which alternative was matched
    f(compile("(a)|."));   // Compliant, the capturing group tells you which alternative was matched
    f(compile("" +
      "a" +
      "|b" + // Noncompliant [[sc=9;ec=10;secondary=+1]]
      "|bc?"));
    f(compile("" +
      "a" +
      "|b" + // Noncompliant [[sc=9;ec=10;secondary=+1]]
      "|bc*"));
    f(compile("a|b|bc+"));
    f(compile("" +
      "a" +
      "|b" +
      "|a" + // Noncompliant [[secondary=-2,+2,+4]]
      "|b" + // Noncompliant [[secondary=-2,+2,+4]]
      "|a" +
      "|b" +
      "|a" +
      "|b"));
    f(compile("" +
      "[1-2]" + // Noncompliant [[secondary=+1,+2,+3]]
      "|[1-4]" +
      "|[1-8]" +
      "|[1-3]"));
    f(compile("" +
      "1" +  // Noncompliant [[secondary=+1]]
      "|[1-2]"));
    f(compile("" +
      "1" +  // Noncompliant [[secondary=+4,+1,+2,+3,+5,+6]]
      "|2" +
      "|[1-2]" +
      "|4" +
      "|[1-6]" + // the winner, the superset of all others
      "|5" +
      "|[2-5]"));
    f(compile("" +
      "a" +
      "|b+" +
      "|b" + // Noncompliant [[secondary=-1, +2]]
      "|c" +
      "|b"));
    f(compile("" +
      "a" +
      "|b" + // Noncompliant [[secondary=+1]]
      "|bb*"));
    f(compile("|a"));
    f(compile("[ab]|a")); // Noncompliant
    f(compile(".*|a")); // Noncompliant
    f(compile("[ab]"));
    f(compile(".*"));
    f(compile("[\uD83D\uDE02]" + "[\uD83D\uDE0A]")); // Compliant

    // POSIX character classes are not handled
    compile("\\p{Space}|x"); // Compliant
    compile("x|\\p{Space}"); // Compliant
    compile("\\p{Space}| "); // Compliant - False negative
    compile("\\p{Lower}|a"); // Compliant - False negative
  }
}
