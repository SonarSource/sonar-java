package checks.regex;

import java.util.regex.Pattern;

public class SingleCharCharacterClassCheckSample {

  void nonCompliant() {
    Pattern.compile("[0]"); // Noncompliant {{Replace this character class by the character itself.}}
    Pattern.compile("a[b]c"); // Noncompliant
//                     ^
    Pattern.compile("[ ]"); // Noncompliant

    // Nested character classes
    Pattern.compile("[ab[c]de]"); // Noncompliant
//                       ^
    Pattern.compile("[[0]]"); // Noncompliant
  }

  void compliant() {
    Pattern.compile("abc");
    Pattern.compile("[0-1]");
    Pattern.compile("[^abc]");
    Pattern.compile("[^a]");

    // Exceptions: meta-characters are excluded, Even though it is longer than escaping, some developers do put metacharacters
    // in a character class (where they no longer have a special meaning) to avoid having to escape them
    Pattern.compile("a[*]b");
    Pattern.compile("a\\*b"); // Equivalent to the previous line
    Pattern.compile("a*b"); // This line has another meaning compared to the two previous examples

    Pattern.compile("[$]");
    Pattern.compile("[*]");
    Pattern.compile("[+]");
    Pattern.compile("[?]");
    Pattern.compile("[.]");
    Pattern.compile("[|]");
    Pattern.compile("[(]");
    Pattern.compile("[{]");

    // "\", "^" and "[" can not be inside characters classes without escaping, the character class is redundant
    Pattern.compile("[\\\\]"); // Noncompliant
    Pattern.compile("[\\^]"); // Noncompliant
    Pattern.compile("[\\[]"); // Noncompliant
    // "]" does not need to be escaped
    Pattern.compile("[]]"); // Noncompliant
    Pattern.compile("a]b");
  }
}
