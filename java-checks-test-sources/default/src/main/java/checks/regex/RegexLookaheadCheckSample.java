package checks.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class RegexLookaheadCheckSample {

  public void test(String str) {
    // Positive lookahead
    compile("(?=a)b"); // Noncompliant [[sc=14;ec=19]] {{Remove or fix this lookahead assertion that can never be true.}}
    compile("(?=ac)ab"); // Noncompliant
    compile("(?=a)bc"); // Noncompliant
    compile("(?=a)a");
    compile("(?=a)..");
    compile("(?=a)ab"); // Compliant: "a" is a prefix of "ab" and will match "ab"
    compile("a(?=b)");
    compile("(?=abc)ab");
    compile("(?=abc)abcd");

    // Negative Lookahead
    compile("(?!a)a"); // Noncompliant - support negative lookahead
    compile("(?!ab)ab"); // Noncompliant
    compile("(?!a)ab").matcher(str).find(); // Noncompliant
    compile("(?!abc)abcd"); // Noncompliant
    compile("(?!ab)..");

    // Lookbehind are not considered
    compile("(?<=a)b");
    compile("(?<!a)b");

    // Match type can influence the result:
    // With "partial" match, "both" or "unknown", what comes after can be a prefix:
    compile("(?=abc)ab").matcher(str).find(); // Compliant: "ab" can be a prefix of "abc" and will match "abc"
    Matcher bothMatcher = compile("(?=abc)ab").matcher(str); // Compliant: "ab" can be a prefix of "abc" and will match "abc"
    bothMatcher.find(); // Compliant
    bothMatcher.matches(); // Will never match, but no issue reported on the regex
    compile("(?=abc)ab"); // Compliant, match is unknown
    // Same for negative lookahead:
    compile("(?!abc)ab").matcher(str).find(); // Compliant
    Matcher bothMatcherNegative = compile("(?!abc)ab").matcher(str); // Compliant: "ab" can be a prefix of "abc" and will match "abc"
    bothMatcherNegative.find(); // Compliant
    bothMatcherNegative.matches(); // Will never match, but no issue reported on the regex
    compile("(?!abc)ab"); // Compliant, match is unknown

    // For full match, what comes after can not be a prefix of the lookahead, we report additional issues:
    compile("(?=abc)ab").matcher(str).matches(); // Noncompliant
    // For negative lookahead, the continuation can still be a prefix
    compile("(?!abc)ab").matcher(str).matches(); // Compliant: will match "ab"

    compile("a(?!:abc):ab").matcher(str).matches(); // Compliant, match "a:ab"
    compile("a(?!:..c):ab").matcher(str).matches();
    compile("a(?!:abc):ab").matcher(str).find();

    // Other examples
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
