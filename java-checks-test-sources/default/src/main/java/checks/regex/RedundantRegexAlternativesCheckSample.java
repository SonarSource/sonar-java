package checks.regex;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class RedundantRegexAlternativesCheckSample {

  public void f(Pattern pattern) {
    f(compile("" +
      "." +
  //   ^>
      "|a")); // Noncompliant  {{Remove or rework this redundant alternative.}}
  //    ^
    f(compile("" +
      "a" + // Noncompliant
  //   ^
      "|."));
  //    ^<
    f(compile("" +
      "(.)" +
  //   ^^^>
      "|(a)")); // Noncompliant
  //    ^^^
    f(compile("(a)|(.)")); // Compliant, the capturing group tells you which alternative was matched
    f(compile("a|(.)"));   // Compliant, the capturing group tells you which alternative was matched
    f(compile("(a)|."));   // Compliant, the capturing group tells you which alternative was matched
    f(compile("" +
      "a" +
      "|b" + // Noncompliant
//      ^
      "|bc?"));
//      ^^^<
    f(compile("" +
      "a" +
      "|b" + // Noncompliant
//      ^
      "|bc*"));
//      ^^^<
    f(compile("a|b|bc+"));
    f(compile("" +
      "a" +
      "|b" +
      "|a" + // Noncompliant
//      ^
//     ^@-3<
//      ^@+8<
//      ^@+9<
      "|b" + // Noncompliant
//      ^
//      ^@-7<
//      ^@+4<
//      ^@+5<
      "|a" +
      "|b" +
      "|a" +
      "|b"));
    f(compile("" +
      "[1-2]" + // Noncompliant
//     ^^^^^
      "|[1-4]" +
//      ^^^^^<
      "|[1-8]" +
//      ^^^^^<
      "|[1-3]"));
//      ^^^^^<
    f(compile("" +
      "1" +  // Noncompliant
//     ^
      "|[1-2]"));
//      ^^^^^<
    f(compile("" +
      "1" +  // Noncompliant
//     ^
      "|2" +
//      ^<
      "|[1-2]" +
//      ^^^^^<
      "|4" +
//      ^<
      "|[1-6]" + // the winner, the superset of all others
//      ^^^^^<
      "|5" +
//      ^<
      "|[2-5]"));
//      ^^^^^<
    f(compile("" +
      "a" +
      "|b+" +
//      ^^>
      "|b" + // Noncompliant
//      ^
      "|c" +
      "|b"));
//      ^<
    f(compile("" +
      "a" +
      "|b" + // Noncompliant
//      ^
      "|bb*"));
//      ^^^<
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
