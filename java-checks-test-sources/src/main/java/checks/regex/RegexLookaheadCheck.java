package checks.regex;

import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public abstract class RegexLookaheadCheck {

  public void test() {
    f(compile("(?=a)b"));   // Noncompliant [[sc=16;ec=21]] {{Remove or fix this lookahead assertion that can never be true.}}
    f(compile("(?=a)bc"));  // Noncompliant
    f(compile("(?=a)a"));
    f(compile("(?=a)ab"));
    f(compile("(?!ab)ab")); // false-negative, negation support will be added by SONARJAVA-3629
    f(compile("(?<=a)b"));
    f(compile("a(?=b)"));
    f(compile("(?!ab).."));
    f(compile("(?<=a)b"));
    f(compile("(?=a)b").matcher("abc").find()); // Noncompliant
    f(compile("(?=a)ab").matcher("abc").find());
    f(compile("(?=abc)ab").matcher("abc").matches()); // Noncompliant
    f(compile("(?=abc)ab").matcher("abc").find());
  }

  abstract void f(Pattern pattern);
  abstract void f(boolean str);

}
