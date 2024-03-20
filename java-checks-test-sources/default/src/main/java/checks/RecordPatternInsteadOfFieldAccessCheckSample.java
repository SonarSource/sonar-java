package checks;

import java.util.Objects;

public class RecordPatternInsteadOfFieldAccessCheckSample {

  record Box() { }

  static void switchOnSealedClass(Object shape) {
    switch (shape) {
      case Box unused -> { } // Compliant, record has no components
      default -> {}
    }
  }

  int sameComponentAccessTwice(Object obj){
    if (obj instanceof Point p) { // Compliant; not all record components are used
      return p.x() + p.x();
    }
    return 0;
  }

  int notAComponentAccessTwice(Object obj){
    if (obj instanceof Point p) { // Compliant; not all record components are used
      return p.x() + p.notAComponent();
    }
    return 0;
  }

  int allComponentsPlusAMethod(Object obj){
    if (obj instanceof Point p) { // Compliant, using record pattern would not allow access to p.notAComponent
      return p.x() + p.y() + p.notAComponent();
    }
    return 0;
  }

  int nonCompliant(Object obj) {
    if (obj instanceof Point p) { // Noncompliant [[sc=24;ec=31;secondary=+1,+2]] {{Use the record pattern instead of this pattern match variable.}}
      int x = p.x();
      int y = p.y();
      return x + y;
    } else if (obj instanceof String s) {
      return s.length();
    }
    return 0;
  }

  int compliantSinceOnly1MemberUsed(Object obj) {
    if (obj instanceof Point p) {
      return p.x();
    } else if (obj instanceof String s) {
      return s.length();
    }
    return 0;
  }

  int nonCompliantSwitch(Object o) {
    return switch (o) {
      case Point p -> p.x() + p.y(); // Noncompliant [[secondary=+0,+0]]
      default -> 0;
    };
  }

  int nonCompliantSwitchOnLine(Object obj) {
    if (obj instanceof Line line) { // Noncompliant [[secondary=+1,+2]]
      int x = line.start().x();
      int y = line.end().y();
      return x + y;
    }
    return 0;
  }

  int compliantSwitch(Object o) {
    return switch (o) {
      case Point p -> {
        if (Objects.equals(p.toString(), "")) {
          yield -1;
        }
        int x = p.x();
        int y = p.y();
        yield x + y;
      }
      default -> 0;
    };
  }

  int compliant(Object obj) {
    if (obj instanceof Point(int x, int y)) {
      return x + y;
    }
    return 0;
  }

  int compliantSwitch2(Object o) {
    return switch (o) {
      case Point(int x, int y) -> x + y;
      default -> 0;
    };
  }

  int compliantBecausePIsUsed(Object obj, Point a) {
    if (obj instanceof Point p) {
      if (p == a) {
        return -1;
      }
      int x = p.x(); // Compliant
      int y = p.y(); // Compliant
      return x + y;
    }
    return 0;
  }

  int compliantBecauseHashCodeIsUsed(Object obj) {
    if (obj instanceof Line line) {
      if (line.hashCode() < 1) {
        return -1;
      }
      int x = line.start().x();
      int y = line.end().y();
      return x + y;
    }
    return 0;
  }

  int compliantSwitchOnOtherTypePattern(Object o) {
    return switch (o) {
      case String s -> s.length();
      default -> 0;
    };
  }

  record Point(int x, int y) {
    public int notAComponent() {
      return 0;
    }
  }

  record Line(Point start, Point end) {
    @Override
    public int hashCode() {
      return 0;
    }
  }

  record Shape(int myInt){}

}
