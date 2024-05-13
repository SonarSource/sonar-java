package checks.regex;

import java.util.regex.Pattern;

public class MultipleWhitespaceCheckSample {

  void noncompliant() {
    Pattern.compile("Hello,   world!"); // Noncompliant {{Replace spaces with quantifier `{3}`.}}
//                          ^^
    Pattern.compile("Hello,  world!"); // Noncompliant {{Replace spaces with quantifier `{2}`.}}
    Pattern.compile("Hello, world!      "); // Noncompliant {{Replace spaces with quantifier `{6}`.}}
  }

  void compliant() {
    Pattern.compile("Hello, {3}world!");
    Pattern.compile("Hello , world!");
    // Whitespaces are ignored when flag "COMMENTS" is used.
    Pattern.compile("Hello,   world!", Pattern.COMMENTS);
    Pattern.compile("(?x)Hello,   world!");
  }
}
