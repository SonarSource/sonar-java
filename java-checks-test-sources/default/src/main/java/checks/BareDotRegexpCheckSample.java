package checks;

import java.util.regex.Pattern;

class BareDotRegexpCheckSample {

  private static final String DOT = ".";
  private static final String ESCAPED_DOT = "\\.";

  void noncompliant(String filename) {
    filename.split("."); // Noncompliant {{This regex "." matches any character, not a literal dot; escape it as "\\." if a period was intended.}}
//                 ^^^
    filename.split(".", 2); // Noncompliant
    filename.split(DOT); // Noncompliant
    filename.replaceAll(".", "_"); // Noncompliant
    filename.replaceFirst(".", "_"); // Noncompliant
    filename.matches("."); // Noncompliant
    Pattern.compile("."); // Noncompliant
    Pattern.compile(DOT); // Noncompliant
    Pattern.compile(".", Pattern.DOTALL); // Noncompliant
    Pattern.matches(".", filename); // Noncompliant
  }

  void compliant(String filename, String regex, int flags) {
    filename.split("\\.");
    filename.split(ESCAPED_DOT);
    filename.split(Pattern.quote("."));
    filename.split("[.]");
    filename.split(".*");
    filename.split(regex);
    filename.replace(".", "_");
    filename.replaceAll("\\.", "_");
    filename.replaceFirst("\\.", "_");
    filename.matches("\\.");
    Pattern.compile("\\.");
    Pattern.compile(".", Pattern.LITERAL);
    Pattern.compile(".", Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
    Pattern.compile(regex);
    Pattern.compile(".", flags);
    Pattern.matches("\\.", filename);
    filename.matches("[\\s\\S]");
    Pattern.compile("[\\s\\S]");
    boolean oneChar = filename.length() == 1;
    String masked = "*".repeat(filename.length());
  }

}
