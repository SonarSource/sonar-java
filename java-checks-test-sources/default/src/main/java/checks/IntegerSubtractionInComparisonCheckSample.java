package checks;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntSupplier;

class IntegerSubtractionInComparisonCheckSample {

  static class TimestampedEvent implements Comparable<TimestampedEvent> {
    private long timestamp;

    @Override
    public int compareTo(TimestampedEvent other) {
      return (int) (this.timestamp - other.timestamp); // Noncompliant {{Subtracting numeric values in compareTo can overflow; use Long.compare instead.}}
    }
  }

  static class IntHolder implements Comparable<IntHolder> {
    private int value;

    @Override
    public int compareTo(IntHolder other) {
      return this.value - other.value; // Noncompliant {{Subtracting numeric values in compareTo can overflow; use Integer.compare instead.}}
    }
  }

  static class BoxedIntHolder implements Comparable<BoxedIntHolder> {
    private Integer value;

    @Override
    public int compareTo(BoxedIntHolder other) {
      return this.value - other.value; // Noncompliant {{Subtracting numeric values in compareTo can overflow; use Integer.compare instead.}}
    }
  }

  static class BoxedLongHolder implements Comparable<BoxedLongHolder> {
    private Long value;

    @Override
    public int compareTo(BoxedLongHolder other) {
      return (int) (this.value - other.value); // Noncompliant {{Subtracting numeric values in compareTo can overflow; use Long.compare instead.}}
    }
  }

  static class MixedOperands implements Comparable<MixedOperands> {
    private long longValue;
    private int intValue;

    @Override
    public int compareTo(MixedOperands other) {
      return (int) (this.longValue - other.intValue); // Noncompliant {{Subtracting numeric values in compareTo can overflow; use Long.compare instead.}}
    }
  }

  static class IntermediateDiff implements Comparable<IntermediateDiff> {
    private int age;

    @Override
    public int compareTo(IntermediateDiff other) {
      int diff = this.age - other.age; // Noncompliant {{Subtracting numeric values in compareTo can overflow; use Integer.compare instead.}}
      if (diff != 0) {
        return diff;
      }
      return 0;
    }
  }

  static class HashCodeCompare implements Comparable<HashCodeCompare> {
    @Override
    public int compareTo(HashCodeCompare other) {
      return this.hashCode() - other.hashCode(); // Noncompliant {{Subtracting numeric values in compareTo can overflow; use Integer.compare instead.}}
    }
  }

  static class AgeComparator implements Comparator<IntHolder> {
    @Override
    public int compare(IntHolder left, IntHolder right) {
      return left.value - right.value; // Noncompliant {{Subtracting numeric values in compare can overflow; use Integer.compare instead.}}
    }
  }

  static class LongArrayComparator implements Comparator<long[]> {
    @Override
    public int compare(long[] a, long[] b) {
      return (int) (a[0] - b[0]); // Noncompliant {{Subtracting numeric values in compare can overflow; use Long.compare instead.}}
    }
  }

  static final Comparator<Number> COMPARATOR_UNBOXED_INT_CAST = new Comparator<Number>() {
    @Override
    public int compare(Number n1, Number n2) {
      return (int) (n1.longValue() - n2.longValue()); // Noncompliant {{Subtracting numeric values in compare can overflow; use Long.compare instead.}}
    }
  };

  static final Comparator<Long> COMPARATOR_BOXED_INT_CAST = new Comparator<Long>() {
    @Override
    public int compare(Long n1, Long n2) {
      return (int) (n1 - n2); // Noncompliant {{Subtracting numeric values in compare can overflow; use Long.compare instead.}}
    }
  };

  static final Comparator<File> COMPARATOR_FILE_INT_CAST = new Comparator<File>() {
    @Override
    public int compare(File lhs, File rhs) {
      return (int) (rhs.lastModified() - lhs.lastModified()); // Noncompliant {{Subtracting numeric values in compare can overflow; use Long.compare instead.}}
    }
  };

  void lambdaSubtraction(List<IntHolder> list, List<TimestampedEvent> events) {
    list.sort((a, b) -> a.value - b.value); // Noncompliant {{Subtracting numeric values in compare can overflow; use Integer.compare instead.}}
    events.sort((left, right) -> (int) (left.timestamp - right.timestamp)); // Noncompliant {{Subtracting numeric values in compare can overflow; use Long.compare instead.}}
  }

  static class CorrectLongCompareTo implements Comparable<CorrectLongCompareTo> {
    private long timestamp;

    @Override
    public int compareTo(CorrectLongCompareTo other) {
      return Long.compare(this.timestamp, other.timestamp); // Compliant
    }
  }

  static class CorrectIntCompareTo implements Comparable<CorrectIntCompareTo> {
    private int value;

    @Override
    public int compareTo(CorrectIntCompareTo other) {
      return Integer.compare(this.value, other.value); // Compliant
    }
  }

  static class CorrectBoxedLongCompareTo implements Comparable<CorrectBoxedLongCompareTo> {
    private Long value;

    @Override
    public int compareTo(CorrectBoxedLongCompareTo other) {
      return value.compareTo(other.value); // Compliant
    }
  }

  static class IntegerRelational implements Comparable<IntegerRelational> {
    private int value;

    @Override
    public int compareTo(IntegerRelational other) {
      if (this.value < other.value) { // Compliant
        return -1;
      }
      return this.value > other.value ? 1 : 0; // Compliant
    }
  }

  static class FloatingPointCompareTo implements Comparable<FloatingPointCompareTo> {
    private double latitude;

    @Override
    public int compareTo(FloatingPointCompareTo other) {
      return (int) (this.latitude - other.latitude); // Compliant - handled by S9148
    }
  }

  static class ByteCompareTo implements Comparable<ByteCompareTo> {
    private byte value;

    @Override
    public int compareTo(ByteCompareTo other) {
      return this.value - other.value; // Compliant - difference fits in int
    }
  }

  static class ShortCompareTo implements Comparable<ShortCompareTo> {
    private short value;

    @Override
    public int compareTo(ShortCompareTo other) {
      return this.value - other.value; // Compliant - difference fits in int
    }
  }

  static class CharCompareTo implements Comparable<CharCompareTo> {
    private char value;

    @Override
    public int compareTo(CharCompareTo other) {
      return this.value - other.value; // Compliant - difference fits in int
    }
  }

  static class BoxedShortCompareTo implements Comparable<BoxedShortCompareTo> {
    private Short value;

    @Override
    public int compareTo(BoxedShortCompareTo other) {
      return this.value - other.value; // Compliant - difference fits in int
    }
  }

  int subtract(int a, int b) {
    return a - b; // Compliant - not in a comparison method
  }

  static class DoubleUtils {
    int compare(int a, int b) {
      return a - b; // Compliant - not in a Comparator
    }
  }

  static class NonComparableTest {
    private final long value = 0;

    public int compareTo(NonComparableTest other) {
      return (int) (this.value - other.value); // Compliant - class is not Comparable
    }
  }

  static final Object COMPARATOR_LIKE_INT_CAST = new Object() {
    public int compare(Long n1, Long n2) {
      return (int) (n1 - n2); // Compliant - not a Comparator
    }
  };

  static class LookAlikeMethods {
    double compareTo(Object other) {
      return other.hashCode() - 1; // Compliant - does not return an int
    }

    int compareTo(Object a, Object b) {
      return a.hashCode() - b.hashCode(); // Compliant - compareTo takes exactly one parameter
    }

    int compare(IntHolder a) {
      return a.value - 1; // Compliant - compare takes exactly two parameters
    }
  }

  static class NestedLambda implements Comparable<NestedLambda> {
    private int value;

    @Override
    public int compareTo(NestedLambda other) {
      IntSupplier difference = () -> this.value - other.value; // Compliant - not a Comparator
      return Integer.compare(difference.getAsInt(), 0); // Compliant
    }
  }

  static class LocalClassInCompareTo implements Comparable<LocalClassInCompareTo> {
    private int value;

    @Override
    public int compareTo(LocalClassInCompareTo other) {
      class Difference {
        int between(int a, int b) {
          return a - b; // Compliant - not in a comparison method
        }
      }
      return Integer.compare(new Difference().between(this.value, other.value), 0); // Compliant
    }
  }

  interface CustomComparable<T> {
    int compareTo(T other); // Compliant - abstract method, no body
  }

  abstract static class AbstractIntComparator implements Comparator<Integer> {
    @Override
    public abstract int compare(Integer a, Integer b); // Compliant - abstract method, no body
  }

  void lambdaCorrect(List<IntHolder> list, List<TimestampedEvent> events) {
    list.sort((a, b) -> Integer.compare(a.value, b.value)); // Compliant
    events.sort((left, right) -> Long.compare(left.timestamp, right.timestamp)); // Compliant
  }

}
