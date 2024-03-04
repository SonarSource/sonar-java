package checks;

public class RecordPatternInsteadOfFieldAccessCheckSample {

  int nonCompliant(Object obj) {
    if (obj instanceof Point p) {
      int x = p.x(); // Noncompliant [[sc=15;ec=20]] {{Use the record pattern instead of field access.}}
      int y = p.y(); // Noncompliant
      return x + y;
    }
    return 0;
  }

  int nonCompliantSwitch(Object o){
    return switch (o) {
      case Point p -> p.x() + p.y(); // Noncompliant
      default -> 0;
    };
  }

  int compliant(Object obj) {
    if (obj instanceof Point(int x, int y)) {
      return x + y;
    }
    return 0;
  }

  int compliantSwitch(Object o){
    return switch (o) {
      case Point(int x, int y) -> x + y;
      default -> 0;
    };
  }

  record Point(int x, int y) {
  }

}
