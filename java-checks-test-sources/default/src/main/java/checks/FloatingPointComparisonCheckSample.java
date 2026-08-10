package checks;

import java.util.Comparator;
import java.util.List;

class FloatingPointComparisonCheckSample {

  // === Noncompliant: subtraction in compareTo with double ===

  static class PositionBySubtraction implements Comparable<PositionBySubtraction> {
    private double latitude;
    private double longitude;

    @Override
    public int compareTo(PositionBySubtraction other) {
      int latComparison = (int) (this.latitude - other.latitude); // Noncompliant {{Use "Double.compare" or "Float.compare" to compare floating-point values.}}
//                                             ^
      if (latComparison != 0) {
        return latComparison;
      }
      return (int) (this.longitude - other.longitude); // Noncompliant
    }
  }

  // === Noncompliant: subtraction in compareTo with float ===

  static class FloatHolder implements Comparable<FloatHolder> {
    private float value;

    @Override
    public int compareTo(FloatHolder other) {
      return (int) (this.value - other.value); // Noncompliant
    }
  }

  // === Noncompliant: subtraction in Comparator.compare ===

  static class DoubleComparator implements Comparator<double[]> {
    @Override
    public int compare(double[] a, double[] b) {
      return (int) (a[0] - b[0]); // Noncompliant
    }
  }

  // === Noncompliant: subtraction in lambda Comparator ===

  void lambdaSubtraction() {
    List<double[]> list = null;
    list.sort((a, b) -> (int) (a[0] - b[0])); // Noncompliant
  }

  // === Noncompliant: relational operators in compareTo ===

  static class RelationalCompareTo implements Comparable<RelationalCompareTo> {
    private double value;

    @Override
    public int compareTo(RelationalCompareTo other) {
      if (this.value < other.value) { // Noncompliant
        return -1;
      }
      if (this.value > other.value) { // Noncompliant
        return 1;
      }
      return 0;
    }
  }

  // === Noncompliant: relational operators in Comparator.compare ===

  static class RelationalComparator implements Comparator<Float> {
    @Override
    public int compare(Float a, Float b) {
      float x = a;
      float y = b;
      if (x <= y) { // Noncompliant
        return x >= y ? 0 : -1; // Noncompliant
      }
      return 1;
    }
  }

  // === Noncompliant: relational operators in lambda ===

  void lambdaRelational() {
    List<Double> list = null;
    list.sort((a, b) -> {
      double x = a;
      double y = b;
      if (x > y) return 1; // Noncompliant
      if (x < y) return -1; // Noncompliant
      return 0;
    });
  }

  // === Compliant: using Double.compare in compareTo ===

  static class CorrectCompareTo implements Comparable<CorrectCompareTo> {
    private double value;

    @Override
    public int compareTo(CorrectCompareTo other) {
      return Double.compare(this.value, other.value); // Compliant
    }
  }

  // === Compliant: using Float.compare in compareTo ===

  static class CorrectFloatCompareTo implements Comparable<CorrectFloatCompareTo> {
    private float value;

    @Override
    public int compareTo(CorrectFloatCompareTo other) {
      return Float.compare(this.value, other.value); // Compliant
    }
  }

  // === Compliant: using Double.compare in Comparator ===

  static class CorrectComparator implements Comparator<double[]> {
    @Override
    public int compare(double[] a, double[] b) {
      return Double.compare(a[0], b[0]); // Compliant
    }
  }

  // === Compliant: using Double.compare in lambda ===

  void lambdaCorrect() {
    List<double[]> list = null;
    list.sort((a, b) -> Double.compare(a[0], b[0])); // Compliant
  }

  // === Compliant: integer subtraction in compareTo ===

  static class IntegerCompareTo implements Comparable<IntegerCompareTo> {
    private int value;

    @Override
    public int compareTo(IntegerCompareTo other) {
      return this.value - other.value; // Compliant - not floating-point
    }
  }

  // === Compliant: integer relational in compareTo ===

  static class IntegerRelational implements Comparable<IntegerRelational> {
    private int value;

    @Override
    public int compareTo(IntegerRelational other) {
      if (this.value < other.value) { // Compliant - not floating-point
        return -1;
      }
      return this.value > other.value ? 1 : 0; // Compliant
    }
  }

  // === Compliant: floating-point subtraction outside compareTo/compare ===

  double subtract(double a, double b) {
    return a - b; // Compliant - not in a comparison method
  }

  boolean isGreater(double a, double b) {
    return a > b; // Compliant - not in a comparison method
  }

  // === Compliant: inner class has its own compareTo ===

  static class OuterWithInner implements Comparable<OuterWithInner> {
    private double value;

    @Override
    public int compareTo(OuterWithInner other) {
      return Double.compare(this.value, other.value); // Compliant

      // Inner class should be checked independently
    }

    static class InnerNotComparable {
      double compute(double a, double b) {
        return a - b; // Compliant - not in a comparison method
      }
    }
  }

  // === Compliant: long subtraction in Comparator ===

  static class LongComparator implements Comparator<long[]> {
    @Override
    public int compare(long[] a, long[] b) {
      return (int) (a[0] - b[0]); // Compliant - not floating-point
    }
  }

  // === Compliant: abstract compareTo method (no body) ===

  interface CustomComparable<T> {
    int compareTo(T other); // Compliant - abstract method, no body
  }

  // === Compliant: abstract compare method in Comparator (no body) ===

  abstract static class AbstractDoubleComparator implements Comparator<Double> {
    @Override
    public abstract int compare(Double a, Double b); // Compliant - abstract method, no body
  }

  // === Compliant: compare method not in a Comparator ===

  static class DoubleUtils {
    int compare(double a, double b) {
      return (int) (a - b); // Compliant - not in a Comparator
    }
  }
}
