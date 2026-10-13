package checks;

public class S9432CheckSample {

  boolean hasContent(String input) {
    boolean result;
    result = input.trim().length() == 0; // Noncompliant {{Use "isBlank()" on the original string instead, if Unicode whitespace should be treated as blank and control characters as non-blank.}}
    result = input.trim().length() <= 0; // Noncompliant {{Use "isBlank()" on the original string instead, if Unicode whitespace should be treated as blank and control characters as non-blank.}}
    result = input.trim().length() < 1; // Noncompliant {{Use "isBlank()" on the original string instead, if Unicode whitespace should be treated as blank and control characters as non-blank.}}
    result = input.strip().length() != 0; // Noncompliant {{Use "isBlank()" on the original string instead.}}
    result = input.strip().length() > 0; // Noncompliant {{Use "isBlank()" on the original string instead.}}
    result = input.strip().length() >= 1; // Noncompliant {{Use "isBlank()" on the original string instead.}}
    result = 0 == input.trim().length(); // Noncompliant {{Use "isBlank()" on the original string instead, if Unicode whitespace should be treated as blank and control characters as non-blank.}}
    result = 0 >= input.trim().length(); // Noncompliant {{Use "isBlank()" on the original string instead, if Unicode whitespace should be treated as blank and control characters as non-blank.}}
    result = 1 > input.trim().length(); // Noncompliant {{Use "isBlank()" on the original string instead, if Unicode whitespace should be treated as blank and control characters as non-blank.}}
    result = 0 != input.strip().length(); // Noncompliant {{Use "isBlank()" on the original string instead.}}
    result = 0 < input.strip().length(); // Noncompliant {{Use "isBlank()" on the original string instead.}}
    result = 1 <= input.strip().length(); // Noncompliant {{Use "isBlank()" on the original string instead.}}
    result = input.trim().isEmpty(); // Noncompliant {{Use "isBlank()" on the original string instead, if Unicode whitespace should be treated as blank and control characters as non-blank.}}
    result = !input.strip().isEmpty(); // Noncompliant {{Use "isBlank()" on the original string instead.}}
    result = input.trim().isBlank(); // Noncompliant {{Use "isBlank()" on the original string instead, if Unicode whitespace should be treated as blank and control characters as non-blank.}}
    result = input.strip().isBlank(); // Noncompliant {{Use "isBlank()" on the original string instead.}}
    return result;
  }

  void validUses(String input) {
    boolean blank = input.isBlank();
    boolean present = !input.isBlank();
    boolean longEnough = input.trim().length() > 1;
    boolean exactLength = input.strip().length() == 5;
    boolean matches = input.trim().equals("value");
    String normalized = input.trim();
    audit(input.strip());
    audit(normalized + blank + present + longEnough + exactLength + matches);
  }

  void audit(String input) {
  }

  static class TextBuffer {
    TextBuffer trim() {
      return this;
    }

    TextBuffer strip() {
      return this;
    }

    int length() {
      return 0;
    }

    boolean isEmpty() {
      return true;
    }

    boolean isBlank() {
      return true;
    }
  }

  boolean customText(TextBuffer input) {
    return input.trim().length() == 0 || input.strip().isEmpty() || input.trim().isBlank();
  }
}
