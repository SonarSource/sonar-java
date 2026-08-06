package checks.regex;

import java.util.regex.Pattern;

public class EscapeSequenceControlCharacterCheckSampleWithoutSemantic {

  void nonCompliant() {
    Pattern.compile("\\ca"); // Noncompliant

    Pattern.compile("ab\\cbde"); // Noncompliant

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

  String email;

}
