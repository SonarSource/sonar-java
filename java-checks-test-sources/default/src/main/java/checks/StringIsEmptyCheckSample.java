package checks;

public class StringIsEmptyCheckSample {
  public boolean sample(String s, String t) {
    boolean b;

    // test `length() == 0` and equivalent code
    b = s.length() == 0; // Noncompliant
    b = s.length() <= 0; // Noncompliant
    b = s.length() < 1; // Noncompliant

    // test `length() != 0` and equivalent code
    b = s.length() != 0; // Noncompliant
    b = s.length() > 0; // Noncompliant
    b = s.length() >= 1; // Noncompliant

    // reversed order
    b = 0 == s.length(); // Noncompliant
    b = 0 >= s.length(); // Noncompliant
    b = 1 > s.length(); // Noncompliant
    b = 0 != s.length(); // Noncompliant
    b = 0 < s.length(); // Noncompliant
    b = 1 <= s.length(); // Noncompliant

    // extra parentheses
    b = (s.length()) == 0; // Noncompliant

    // chained method calls
    b = s.toUpperCase().length() == 0; // Noncompliant

    // problem in a nested expression
    b = "abc".equals(s) || s.length() == 0; // Noncompliant

    b = s.length() == 1;
    b = s.length() > 3;
    b = s.length() <= 10;
    b = 2 < s.length();

    b = s.trim().length() >= 8;

    b = s.length() == t.length();

    b = s.isEmpty();
    b = !s.isEmpty();

    b = 1 < 0;

    // StringBuilder does not have `isEmpty()`
    StringBuilder stringBuilder = new StringBuilder();
    b = stringBuilder.length() == 0;

    return b;
  }
}
