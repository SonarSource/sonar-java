package checks;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Formattable;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

class RedundantStringFormatCheckSample {

  private static final String ROW_FORMAT = "%s: %d";

  void printing(int a, int b, int sum, double ratio, long count, PrintWriter writer, int pageNum, String title) {
    System.out.println(String.format("Result: %d + %d = %d", a, b, sum)); // Noncompliant {{Use "printf" instead of "String.format".}} [[quickfixes=qf1]]
//                     ^^^^^^^^^^^^^
    // fix@qf1 {{Replace with "printf"}}
    // edit@qf1 [[sc=59;ec=59]] {{%n}}
    // edit@qf1 [[sc=16;ec=23]] {{printf}}
    // edit@qf1 [[sc=24;ec=38]] {{}}
    // edit@qf1 [[sc=71;ec=72]] {{}}
    System.err.print(String.format("%.2f", ratio)); // Noncompliant [[quickfixes=qf2]]
    // fix@qf2 {{Replace with "printf"}}
    // edit@qf2 [[sc=16;ec=21]] {{printf}}
    // edit@qf2 [[sc=22;ec=36]] {{}}
    // edit@qf2 [[sc=49;ec=50]] {{}}
    System.out.println(String.format(Locale.US, "%,d", count)); // Noncompliant [[quickfixes=qf3]]
    // fix@qf3 {{Replace with "printf"}}
    // edit@qf3 [[sc=53;ec=53]] {{%n}}
    // edit@qf3 [[sc=16;ec=23]] {{printf}}
    // edit@qf3 [[sc=24;ec=38]] {{}}
    // edit@qf3 [[sc=61;ec=62]] {{}}
    writer.println(String.format("Page %d: %s", pageNum, title)); // Noncompliant
    writer.print(String.format(ROW_FORMAT, title, pageNum)); // Noncompliant [[quickfixes=qf4]]
    // fix@qf4 {{Replace with "printf"}}
    // edit@qf4 [[sc=12;ec=17]] {{printf}}
    // edit@qf4 [[sc=18;ec=32]] {{}}
    // edit@qf4 [[sc=58;ec=59]] {{}}
    System.out.println((String.format("Total: %d", sum))); // Noncompliant [[quickfixes=qf5]]
    // fix@qf5 {{Replace with "printf"}}
    // edit@qf5 [[sc=49;ec=49]] {{%n}}
    // edit@qf5 [[sc=16;ec=23]] {{printf}}
    // edit@qf5 [[sc=24;ec=39]] {{}}
    // edit@qf5 [[sc=55;ec=57]] {{}}
    System.out.println(String.format(ROW_FORMAT, title, pageNum)); // Noncompliant [[quickfixes=!]]
    // Noncompliant@+1 [[quickfixes=!]]
    writer.println(String.format("""
      %s""", title));
    System.out.println(String.format("Id %s", new FormattableId())); // Noncompliant

    System.out.printf("Result: %d + %d = %d%n", a, b, sum);
    System.out.println("Result: " + sum);
    System.out.println(String.format("Result: %d", sum).trim());
    System.out.println("Total " + String.format("%.2f", ratio));
  }

  String builder(String name, int age) {
    StringBuilder sb = new StringBuilder("{");
    sb.append(String.format("name: %s, ", name));
    new StringBuffer().append(String.format("age: %d", age));
    sb.append("name: ").append(name);
    return sb.toString();
  }

  void validate(int age, String name, Exception cause) throws ValidationException {
    if (age < 0) {
      throw new IllegalArgumentException(String.format("Age cannot be negative: %d", age));
    }
    if (name == null) {
      throw new RuntimeException(String.format("Missing name for age %d", age), cause);
    }
    if (name.isEmpty()) {
      throw new ValidationException(String.format("Empty name %s", name));
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

  static class FormattableId implements Formattable {
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision) {
      formatter.format("ID");
    }
  }
}
