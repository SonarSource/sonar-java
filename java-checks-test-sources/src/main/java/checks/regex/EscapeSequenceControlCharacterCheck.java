package checks.regex;

import java.util.regex.Pattern;
import javax.validation.constraints.Email;

public class EscapeSequenceControlCharacterCheck {

  void nonCompliant() {
    Pattern.compile("\\ca"); // Noncompliant [[sc=22;ec=26]] {{Remove or replace this problematic use of \c.}}
    Pattern.compile("ab\\cbde"); // Noncompliant [[sc=24;ec=28]]
    Pattern.compile("\\cb"); // Noncompliant
    Pattern.compile("\\cx"); // Noncompliant
    Pattern.compile("\\c!"); // Noncompliant
    Pattern.compile("\\c-"); // Noncompliant
  }

  void compliant() {
    Pattern.compile("\\cA");
    Pattern.compile("\\cG");
    Pattern.compile("\\cX");
    Pattern.compile("\\c@");
    Pattern.compile("\\c[");
    Pattern.compile("\\c\\");
    Pattern.compile("\\c]");
    Pattern.compile("\\c^");
    Pattern.compile("\\c_");

    // Not control character
    Pattern.compile("\\da");
    Pattern.compile("\\\\ca");
  }

  @Email(regexp = "\\ca") // Noncompliant [[sc=20;ec=24]]
  String email;

}
