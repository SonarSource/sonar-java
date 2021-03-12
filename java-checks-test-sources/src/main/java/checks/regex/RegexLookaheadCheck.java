package checks.regex;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public abstract class RegexLookaheadCheck {

  public void test(String str) {
    f(compile("(?=a)b"));   // Noncompliant [[sc=16;ec=21]] {{Remove or fix this lookahead assertion that can never be true.}}
    f(compile("(?=a)bc"));  // Noncompliant
    f(compile("(?=a)a"));
    f(compile("(?=a)ab"));
    f(compile("(?!ab)ab")); // false-negative, negation support will be added by SONARJAVA-3629
    f(compile("(?<=a)b"));
    f(compile("a(?=b)"));
    f(compile("(?!ab).."));
    f(compile("(?<=a)b"));
    f(compile("(?=a)b").matcher(str).find()); // Noncompliant
    f(compile("(?=abc)ab").matcher(str).find());
    f(compile("(?=a)ab").matcher(str).find());
    f(compile("(?=abc)ab").matcher(str).matches()); // Noncompliant
    f(compile("(?=abc)ab").matcher(str).find());

    f(str.matches("(?=.*foo-bar)\\w*")); // Noncompliant
    f(compile("(?=.*foo-bar)\\w*$")); // False negative because boundaries aren't yet supported by intersects
    f(compile("(?=.*foo-bar)\\w*")); // Compliant because foo-bar can match after the \\w* finishes matching

    f(compile("(?=.*foo)(?=.*bar)(?=.*baz)rest-of-pattern-baz").matcher(str).find()); // Compliant, foo and bar may appear after the (partial) match
    f(compile("(?=.*foo)(?=.*bar)(?=.*baz)rest-of-pattern-baz").matcher(str).matches()); // FN because there are consecutive lookaheads and intersects can't handle lookaheads

    // string regex and pattern regex share the same string literal
    String pattern_str = "(?=a)b"; // Noncompliant
    Pattern pattern = Pattern.compile(pattern_str);
    f("".matches(pattern_str));
    f(pattern.matcher("").matches());
  }

  abstract void f(Pattern pattern);
  abstract void f(boolean str);

}
