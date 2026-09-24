package checks;

import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleSupplier;

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

  // === Noncompliant: only one operand is floating-point ===

  static class MixedOperands implements Comparable<MixedOperands> {
    private double value;

    @Override
    public int compareTo(MixedOperands other) {
      if (0 < other.value) { // Noncompliant
        return -1;
      }
      return Double.compare(this.value, other.value); // Compliant
    }
  }

  // === Compliant: lambda which is not a Comparator, nested in compareTo ===

  static class NonComparatorLambda implements Comparable<NonComparatorLambda> {
    private double value;

    @Override
    public int compareTo(NonComparatorLambda other) {
      DoubleSupplier difference = () -> this.value - other.value; // Compliant - not a Comparator
      return Double.compare(difference.getAsDouble(), 0.0); // Compliant
    }
  }

  // === Compliant: local class nested in compareTo is checked independently ===

  static class LocalClassInCompareTo implements Comparable<LocalClassInCompareTo> {
    private double value;

    @Override
    public int compareTo(LocalClassInCompareTo other) {
      class Difference {
        double between(double a, double b) {
          return a - b; // Compliant - not in a comparison method
        }
      }
      return Double.compare(new Difference().between(this.value, other.value), 0.0); // Compliant
    }
  }

  // === Compliant: subtraction used with Float.compare/Double.compare ===

  static class DifferenceWithFloatCompare implements Comparable<DifferenceWithFloatCompare> {
    private float a;
    private float b;

    @Override
    public int compareTo(DifferenceWithFloatCompare other) {
      float d1 = this.a - this.b; // Compliant - result flows into Float.compare
      float d2 = other.a - other.b;
      return Float.compare(d1, d2);
    }
  }

  static class DifferenceWithDoubleCompare implements Comparable<DifferenceWithDoubleCompare> {
    private double x;
    private double y;

    @Override
    public int compareTo(DifferenceWithDoubleCompare other) {
      double d1 = this.x - this.y; // Compliant - result flows into Double.compare
      double d2 = other.x - other.y;
      return Double.compare(d1, d2);
    }
  }

  // === Noncompliant: subtraction with Float.compare but also relational operators ===

  static class MixedSubtractionAndRelational implements Comparable<MixedSubtractionAndRelational> {
    private float a;
    private float b;

    @Override
    public int compareTo(MixedSubtractionAndRelational other) {
      float d1 = this.a - this.b; // Noncompliant
//                      ^
      float d2 = other.a - other.b; // Noncompliant
//                       ^
      if (d1 > d2) { // Noncompliant
//           ^
        return 1;
      }
      return Float.compare(d1, d2);
    }
  }

  // === Compliant: subtraction in Comparator lambda with Double.compare ===

  void lambdaSubtractionWithCompare() {
    List<double[]> list = null;
    list.sort((a, b) -> {
      double diff1 = a[0] - a[1]; // Compliant - result flows into Double.compare
      double diff2 = b[0] - b[1];
      return Double.compare(diff1, diff2);
    });
  }

  // === Noncompliant: Float.compare used for one field, raw subtraction for another ===

  static class PartialCompare implements Comparable<PartialCompare> {
    private float a;
    private float x;

    @Override
    public int compareTo(PartialCompare other) {
      int c = Float.compare(this.a, other.a);
      if (c != 0) return c;
      return (int) (this.x - other.x); // Noncompliant
    }
  }

  // === Noncompliant: Double.compare called conditionally, subtraction returned directly ===

  static class ConditionalCompare implements Comparable<ConditionalCompare> {
    private double a;
    private double b;

    @Override
    public int compareTo(ConditionalCompare other) {
      if (this.a != other.a) {
        return Double.compare(this.a, other.a);
      }
      return (int) (this.b - other.b); // Noncompliant
    }
  }

  // === Noncompliant: subtraction in ternary, compare call elsewhere ===

  static class TernaryWithCompare implements Comparable<TernaryWithCompare> {
    private double x;
    private double y;

    @Override
    public int compareTo(TernaryWithCompare other) {
      double diff = this.x - other.x; // Noncompliant
//                         ^
      return diff != 0 ? (int) diff : Double.compare(this.y, other.y);
    }
  }

  // === Compliant: subtraction used as direct argument to Float.compare ===

  static class DirectSubtractionArgument implements Comparable<DirectSubtractionArgument> {
    private float a;
    private float b;

    @Override
    public int compareTo(DirectSubtractionArgument other) {
      return Float.compare(this.a - this.b, other.a - other.b); // Compliant
    }
  }

  // === Compliant: methods which only look like comparison methods ===

  static class LookAlikeMethods {
    double compareTo(Object other) {
      return other.hashCode() - 1.0; // Compliant - does not return an int
    }

    int compareTo(Object a, Object b) {
      return (int) (a.hashCode() - b.hashCode() - 0.5); // Compliant - compareTo takes exactly one parameter
    }

    int compareTo(double a) {
      return (int) (a - 1.0); // Compliant - the parameter is primitive
    }

    int compare(Double a) {
      return (int) (a - 1.0); // Compliant - compare takes exactly two parameters
    }

    double compare(Double a, Double b) {
      return a - b; // Compliant - does not return an int
    }
  }

  // === Compliant: parenthesized subtraction in Float.compare ===

  static class ParenthesizedSubtractionInCompare implements Comparable<ParenthesizedSubtractionInCompare> {
    private float a;
    private float b;

    @Override
    public int compareTo(ParenthesizedSubtractionInCompare other) {
      return Float.compare((this.a - this.b), (other.a - other.b)); // Compliant
    }
  }

  // === Compliant: cast subtraction in Double.compare ===

  static class CastSubtractionInCompare implements Comparable<CastSubtractionInCompare> {
    private float x;
    private float y;

    @Override
    public int compareTo(CastSubtractionInCompare other) {
      return Double.compare((double) (this.x - this.y), (double) (other.x - other.y)); // Compliant
    }
  }

  // === Compliant: chained subtraction in Double.compare ===

  static class ChainedSubtractionInCompare implements Comparable<ChainedSubtractionInCompare> {
    private double a;
    private double b;
    private double c;

    @Override
    public int compareTo(ChainedSubtractionInCompare other) {
      return Double.compare(this.a - this.b - this.c, other.a - other.b - other.c); // Compliant
    }
  }

  // === Compliant: parenthesized initializer variable in Float.compare ===

  static class ParenthesizedInitializerInCompare implements Comparable<ParenthesizedInitializerInCompare> {
    private float a;
    private float b;

    @Override
    public int compareTo(ParenthesizedInitializerInCompare other) {
      float d1 = (this.a - this.b); // Compliant - result flows into Float.compare
      float d2 = (other.a - other.b);
      return Float.compare(d1, d2);
    }
  }

  // === Noncompliant: variable used in compare AND truncated ===

  static class VariableUsedOutsideCompare implements Comparable<VariableUsedOutsideCompare> {
    private double x;

    @Override
    public int compareTo(VariableUsedOutsideCompare other) {
      double d = this.x - other.x; // Noncompliant
//                      ^
      if (Double.compare(d, 0) == 0) return 0;
      return (int) d;
    }
  }

  // === Noncompliant: subtraction compared against constant in Double.compare ===

  static class SubtractionAgainstConstant implements Comparable<SubtractionAgainstConstant> {
    private double x;

    @Override
    public int compareTo(SubtractionAgainstConstant other) {
      return Double.compare(this.x - other.x, 0); // Noncompliant
//                                 ^
    }
  }

  static class SubtractionAgainstZeroFloat implements Comparable<SubtractionAgainstZeroFloat> {
    private float a;

    @Override
    public int compareTo(SubtractionAgainstZeroFloat other) {
      return Float.compare(this.a - other.a, 0f); // Noncompliant
//                                ^
    }
  }

  // === Compliant: parenthesized variable usage in compare ===

  static class ParenthesizedVariableUsage implements Comparable<ParenthesizedVariableUsage> {
    private double x;
    private double y;

    @Override
    public int compareTo(ParenthesizedVariableUsage other) {
      double a = this.x - this.y; // Compliant - flows into Double.compare
      double b = other.x - other.y;
      return Double.compare((a), (b));
    }
  }

  // === Compliant: cast variable usage in compare ===

  static class CastVariableUsage implements Comparable<CastVariableUsage> {
    private float x;
    private float y;

    @Override
    public int compareTo(CastVariableUsage other) {
      float a = this.x - this.y; // Compliant - flows into Double.compare via cast
      float b = other.x - other.y;
      return Double.compare((double) a, (double) b);
    }
  }

  // === Compliant: subtraction nested in method call inside Double.compare ===

  static class MathHypotInCompare implements Comparable<MathHypotInCompare> {
    private double x;
    private double y;

    @Override
    public int compareTo(MathHypotInCompare other) {
      return Double.compare(Math.hypot(this.x - 1, this.y - 1), Math.hypot(other.x - 1, other.y - 1)); // Compliant
    }
  }

  // === Compliant: Math.abs of subtraction stored in variable passed to Double.compare ===

  static class AbsSubtractionInCompare implements Comparable<AbsSubtractionInCompare> {
    private double x;
    private double y;

    @Override
    public int compareTo(AbsSubtractionInCompare other) {
      double d1 = Math.abs(this.x - this.y); // Compliant - flows into Double.compare
      double d2 = Math.abs(other.x - other.y);
      return Double.compare(d1, d2);
    }
  }

  // === Compliant: subtraction in addition inside Float.compare ===

  static class AdditionWithSubtractionInCompare implements Comparable<AdditionWithSubtractionInCompare> {
    private float a;
    private float b;
    private float c;

    @Override
    public int compareTo(AdditionWithSubtractionInCompare other) {
      return Float.compare(this.a - this.b + this.c, other.a - other.b + other.c); // Compliant
    }
  }

  // === Compliant: squared delta in Double.compare ===

  static class SquaredDeltaInCompare implements Comparable<SquaredDeltaInCompare> {
    private double a;
    private double b;

    @Override
    public int compareTo(SquaredDeltaInCompare other) {
      double d1 = (this.a - this.b) * (this.a - this.b); // Compliant - flows into Double.compare
      double d2 = (other.a - other.b) * (other.a - other.b);
      return Double.compare(d1, d2);
    }
  }
}
