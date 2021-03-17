package checks.regex;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class RegexLookaheadCheck {

  public void test(String str) {
    compile("(?=a)b"); // Noncompliant [[sc=14;ec=19]] {{Remove or fix this lookahead assertion that can never be true.}}
    compile("(?=ac)ab"); // Noncompliant
    compile("(?=a)bc"); // Noncompliant
    compile("(?=a)a");
    compile("(?=a)..");
    compile("(?=a)ab");

    compile("(?!a)a"); // Noncompliant - support negative lookahead
    compile("(?!ab)ab"); // Noncompliant
    compile("(?!ab)..");

    compile("(?<=a)b");
    compile("a(?=b)");
    compile("(?<=a)b");
    compile("(?=a)b").matcher(str).find(); // Noncompliant
    compile("(?=abc)ab").matcher(str).find();
    compile("(?=a)ab").matcher(str).find();
    compile("(?=abc)ab").matcher(str).matches(); // Noncompliant
    compile("(?=abc)ab").matcher(str).find();

    str.matches("(?=.*foo-bar)\\w*"); // Noncompliant
    compile("(?=.*foo-bar)\\w*$"); // False negative because boundaries aren't yet supported by intersects
    compile("(?=.*foo-bar)\\w*"); // Compliant because foo-bar can match after the \\w* finishes matching

    compile("(?=.*foo)(?=.*bar)(?=.*baz)rest-of-pattern-baz").matcher(str).find(); // Compliant, foo and bar may appear after the (partial) match
    compile("(?=.*foo)(?=.*bar)(?=.*baz)rest-of-pattern-baz").matcher(str).matches(); // FN because there are consecutive lookaheads and intersects can't handle lookaheads

    // string regex and pattern regex share the same string literal
    String pattern_str = "(?=a)b"; // Noncompliant
    Pattern pattern = compile(pattern_str);
    "".matches(pattern_str);
    pattern.matcher("").matches();
  }
}
