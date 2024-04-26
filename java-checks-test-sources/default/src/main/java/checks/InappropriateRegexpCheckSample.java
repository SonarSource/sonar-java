package checks;

import java.io.File;

class InappropriateRegexpCheckSample {

  private static final String MY_REGEX = ".";

  void replaceAll() {
    "".replaceAll(".", ""); // Noncompliant {{Correct this regular expression.}}
//                ^^^
    "".replaceAll("|", "_"); // Noncompliant {{Correct this regular expression.}}
//                ^^^
    "".replaceAll(File.separator, ""); // Noncompliant {{Correct this regular expression.}}
//                ^^^^^^^^^^^^^^
    "".replaceAll("\\.", "");
    "".replaceAll("\\|", "");

    "".replaceAll(MY_REGEX, ""); // Noncompliant
  }
  void replaceFirst() {
    "".replaceFirst(".", ""); // Noncompliant {{Correct this regular expression.}}
//                  ^^^
    "".replaceFirst("|", "_"); // Noncompliant {{Correct this regular expression.}}
//                  ^^^
    "".replaceFirst(File.separator, ""); // Noncompliant {{Correct this regular expression.}}
//                  ^^^^^^^^^^^^^^
    "".replaceFirst("\\.", "");
    "".replaceFirst("\\|", "");
  }
}
