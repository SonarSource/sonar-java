package checks;

public class UnusedVarInPatternMatchingCheckSample {

  public int goo(Object o) {
    if (o instanceof Point(double x, double y)) { // Noncompliant  {{Remove this unused record pattern matching.}} [[quickfixes=qf2]]
      //                  ^^^^^^^^^^^^^^^^^^^^
      // fix@qf2 {{Remove the record deconstruction.}}
      // edit@qf2 [[sc=27;ec=47]] {{}}
      return 1;
    }
    if (o instanceof Point(double x, double y)) {
      return (int) (1 + x);
    }
    return 0;
  }

  record Point(double x, double y) {
  }


}
