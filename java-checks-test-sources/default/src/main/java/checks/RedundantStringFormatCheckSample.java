package checks;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class RedundantStringFormatCheckSample {

  private static final String ROW_FORMAT = "%s: %d";

  void printing(int a, int b, int sum, double ratio, long count, PrintWriter writer, int pageNum, String title) {
    System.out.println(String.format("Result: %d + %d = %d", a, b, sum)); // Noncompliant {{Use "printf" instead of "String.format".}}
//                     ^^^^^^^^^^^^^
    System.err.print(String.format("%.2f", ratio)); // Noncompliant
    System.out.println(String.format(Locale.US, "%,d", count)); // Noncompliant
    writer.println(String.format("Page %d: %s", pageNum, title)); // Noncompliant
    writer.print(String.format(ROW_FORMAT, title, pageNum)); // Noncompliant
    System.out.println((String.format("Total: %d", sum))); // Noncompliant

    System.out.printf("Result: %d + %d = %d%n", a, b, sum);
    System.out.println("Result: " + sum);
    System.out.println(String.format("Result: %d", sum).trim());
    System.out.println("Total " + String.format("%.2f", ratio));
  }

  String json(String name, int age, int hash) {
    StringBuilder sb = new StringBuilder("{");
    sb.append(String.format("\"name\":\"%s\",", name)); // Noncompliant {{Use chained "append" calls instead of "String.format".}}
//            ^^^^^^^^^^^^^
    sb.append(String.format("\"age\":%d", age)).append("}"); // Noncompliant
    StringBuffer buffer = new StringBuffer();
    buffer.append(String.format("Progress 100%% for %s", name)); // Noncompliant

    sb.append("\"age\":").append(age);
    sb.append(String.format("%08X", hash));
    sb.append(String.format("%.2f", age / 3.0));
    sb.append(String.format(Locale.ROOT, "%d", age));
    sb.append(String.format(ROW_FORMAT, name, age));
    sb.append(String.format("%n"));
    sb.append(String.format("%1$s-%1$s", name));
    sb.append(String.format("100%", name));
    sb.append(String.format("""
      %s""", name));
    return sb.toString();
  }

  void validate(int age, String name, Exception cause) throws ValidationException {
    if (age < 0) {
      throw new IllegalArgumentException(String.format("Age cannot be negative: %d", age)); // Noncompliant {{Use string concatenation instead of "String.format".}}
//                                       ^^^^^^^^^^^^^
    }
    if (name == null) {
      throw new RuntimeException(String.format("Missing name for age %d", age), cause); // Noncompliant
    }
    if (name.isEmpty()) {
      throw new ValidationException(String.format("Empty name %s", name)); // Noncompliant
    }
    if (name.length() > 10) {
      throw new IllegalStateException(String.format("%-10s|", name));
    }
    if (age > 150) {
      throw new IllegalArgumentException("Age too high: " + age);
    }
  }

  List<String> unrelatedTargets(String name, int age) {
    List<String> lines = new ArrayList<>();
    lines.add(String.format("name=%s", name));
    Object[] values = new Object[]{String.format("age=%d", age)};
    String message = String.format("name=%s", name);
    System.out.println(message);
    new Wrapper(String.format("name=%s", name));
    lines.add("name=%s".formatted(name));
    return lines;
  }

  static class ValidationException extends Exception {
    ValidationException(String message) {
      super(String.format("Validation failed: %s", message));
    }
  }

  record Wrapper(String value) {
  }
}
