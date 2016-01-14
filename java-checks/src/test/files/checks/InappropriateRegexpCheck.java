import java.io.File;
class A {
  void foo() {
    "".replaceAll(".", ""); // Noncompliant [[sc=19;ec=22]] {{Correct this regular expression.}}
    "".replaceAll("|", "_");// Noncompliant [[sc=19;ec=22]] {{Correct this regular expression.}}
    "".replaceAll(File.separator, ""); // Noncompliant [[sc=19;ec=33]] {{Correct this regular expression.}}
    "".replaceAll("\\.", "");
    "".replaceAll("\\|", "");
  }
}