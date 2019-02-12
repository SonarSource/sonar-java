import java.io.File;
class A {
  void replaceAll() {
    "".replaceAll(".", ""); // Noncompliant [[sc=19;ec=22]] {{Correct this regular expression.}}
    "".replaceAll("|", "_");// Noncompliant [[sc=19;ec=22]] {{Correct this regular expression.}}
    "".replaceAll(File.separator, ""); // Noncompliant [[sc=19;ec=33]] {{Correct this regular expression.}}
    "".replaceAll("\\.", "");
    "".replaceAll("\\|", "");
  }
  void replaceFirst() {
    "".replaceFirst(".", ""); // Noncompliant [[sc=21;ec=24]] {{Correct this regular expression.}}
    "".replaceFirst("|", "_");// Noncompliant [[sc=21;ec=24]] {{Correct this regular expression.}}
    "".replaceFirst(File.separator, ""); // Noncompliant [[sc=21;ec=35]] {{Correct this regular expression.}}
    "".replaceFirst("\\.", "");
    "".replaceFirst("\\|", "");
  }
}
