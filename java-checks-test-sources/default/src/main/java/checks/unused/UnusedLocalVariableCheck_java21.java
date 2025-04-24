package checks.unused;

import java.util.stream.Stream;

public class UnusedLocalVariableCheck_java21 {
  public static int count(int[] elements) {
    int count = 0;
    for (int element : elements) { // Compliant
      count++;
    }

    for (int a : new int[]{0, 1, 2}) { // Compliant
      count++;
    }

    return count;
  }

  public static void tryWithResources() {
    try (Stream foo = Stream.of()) { // Compliant
    } catch (Exception e) {
    }

    Stream bar = Stream.of();
    try(bar) {
    } catch (Exception e) {
    }
  }

  sealed interface Shape permits Box, Circle {}
  record Box() implements Shape { }
  record Circle() implements Shape {}

  static void switchOnSealedClass(Shape shape) {
    switch (shape) {
      case Box unused -> { } // Compliant
      case Circle circle -> circle.toString();
    }
  }

  static void switchWithTypePattern(Object o) {
    switch (o) {
      case Number used -> used.longValue();
      case Shape unused -> { } // Compliant
      default -> System.out.println();
    }
  }

  record MyRecord(int x, int y) { }

  static void switchRecordGuardedPattern(Object o) {
    if(o instanceof MyRecord(int x, int y)) {} // Compliant
    switch (o) {
      case MyRecord(int x, int y) when x > 42 -> { } // Compliant
      case MyRecord(int x, int y) when y < 42 -> { } // Compliant
      case MyRecord m when m.x > 42 -> { }
      case MyRecord m when o.toString().length() > 42 -> { } // Compliant
      case MyRecord(int x, int y) -> { } // Compliant
      case MyRecord m -> { } // Compliant
      case Object object -> {
        object.toString();
        var x = 42; // Noncompliant
        System.out.println();
      }
    }
  }

}
