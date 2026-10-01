package checks;

import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.IntBinaryOperator;
import java.util.function.IntSupplier;

class IntegerOverflowCheckSample {

  int intAddition = Integer.MAX_VALUE + 1; // Noncompliant
//                  ^^^^^^^^^^^^^^^^^^^^^
  int intSubtraction = Integer.MIN_VALUE - 1; // Noncompliant
//                     ^^^^^^^^^^^^^^^^^^^^^
  int intMultiplication = 1_000_000 * 10_000; // Noncompliant
//                        ^^^^^^^^^^^^^^^^^^
  long longAddition = Long.MAX_VALUE + 1L; // Noncompliant
//                    ^^^^^^^^^^^^^^^^^^^
  long longSubtraction = Long.MIN_VALUE - 1L; // Noncompliant
//                       ^^^^^^^^^^^^^^^^^^^
  long longMultiplication = 4_000_000_000L * 4_000_000_000L; // Noncompliant
//                          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

  int nested = Integer.MAX_VALUE + 1; // Noncompliant
//             ^^^^^^^^^^^^^^^^^^^^^
  int wrappedExact = (Integer.MAX_VALUE + 1) + Integer.MAX_VALUE; // Noncompliant
//                    ^^^^^^^^^^^^^^^^^^^^^
  long wrappedLongExact = (Long.MAX_VALUE + 1L) + Long.MAX_VALUE; // Noncompliant
//                         ^^^^^^^^^^^^^^^^^^^
  int nestedWithoutOverflow = (1 + 2) + 3;

  int exactIntMaximum = Integer.MAX_VALUE - 1;
  int exactIntMinimum = Integer.MIN_VALUE + 1;
  long exactLongMaximum = Long.MAX_VALUE - 1L;
  long widenedBefore = (long) Integer.MAX_VALUE + 1;
  long ownedByS2184 = Integer.MAX_VALUE + 1;
  Object explicitResultCast = (long) (Integer.MAX_VALUE + 1); // Noncompliant
//                                    ^^^^^^^^^^^^^^^^^^^^^
  double ownedLongToDouble = Long.MAX_VALUE + 1L;
  float ownedLongToFloat = Long.MAX_VALUE + 1L;
  double assignedDouble;

  long ownedReturn() {
    return Integer.MAX_VALUE + 1;
  }

  void widenedArguments() {
    consume(Integer.MAX_VALUE + 1);
    new LongHolder(Integer.MAX_VALUE + 1);
    consumeTwo(0L, Integer.MAX_VALUE + 1);
    new Date((long) (Integer.MAX_VALUE + 1));
    Instant.ofEpochSecond((long) (Integer.MAX_VALUE + 1), 0L);
    Instant.ofEpochSecond(0L, (long) (Integer.MAX_VALUE + 1)); // Noncompliant
//                                    ^^^^^^^^^^^^^^^^^^^^^
    Instant.ofEpochSecond((long) (Long.MAX_VALUE + 1L), 0L); // Noncompliant
//                                ^^^^^^^^^^^^^^^^^^^
    consumeObject((long) (Integer.MAX_VALUE + 1)); // Noncompliant
//                        ^^^^^^^^^^^^^^^^^^^^^
  }

  void consume(long value) {
  }

  void consumeTwo(long first, long second) {
  }

  void consumeObject(Object value) {
  }

  void widenedAssignment() {
    assignedDouble = Long.MAX_VALUE + 1L;
  }

  int unknown(int left, int right) {
    int arbitrary = left + right;
    long member = new LongHolder(0L).value + 1;
    return arbitrary;
  }

  int midpoint(int low, int high) {
    int result = (low + high) / 2; // Noncompliant
//                ^^^^^^^^^^
    double doubleResult = (low + high) / 2.0; // Noncompliant
//                         ^^^^^^^^^^
    float floatResult = (low + high) / 2f; // Noncompliant
//                       ^^^^^^^^^^
    int safe = low + (high - low) / 2;
    int shifted = (low + high) >>> 1;
    long widenedAverage = ((long) low + high) / 2;
    int knownAverage = (1 + 2) / 2;
    int ceilingHalf = (low + 1) / 2;
    return result + safe + shifted + (int) doubleResult + (int) floatResult + knownAverage + ceilingHalf;
  }

  int boundedMidpoints(int low, String text, List<String> values) {
    int boundedAverage = (text.length() + values.size()) / 2;
    int mixedAverage = (low + values.size()) / 2; // Noncompliant
//                      ^^^^^^^^^^^^^^^^^^^
    return boundedAverage + mixedAverage;
  }

  long longAverage(long low, long high) {
    return (low + high) / 2;
  }

  int narrowAverages(short shortLow, short shortHigh, byte byteLow, byte byteHigh, char charLow, char charHigh, int intLow) {
    int shortMidpoint = (shortLow + shortHigh) / 2;
    int byteMidpoint = (byteLow + byteHigh) / 2;
    int charMidpoint = (charLow + charHigh) / 2;
    int mixedMidpoint = (intLow + shortHigh) / 2;
    return shortMidpoint + byteMidpoint + charMidpoint + mixedMidpoint;
  }

  int bounded(String text, List<String> values, Map<String, String> map, Day day, int[] array) {
    int safe = text.length();
    int twoBounded = text.length() + values.size();
    int lengthPlusOne = array.length + 1;
    int possibleOverflow = array.length * Integer.MAX_VALUE;
    int wrappedUnknown = (text.length() + values.size()) + Integer.MAX_VALUE;
    int mask = (hashCode() & 255) * 1_000_000;
    int reverseMask = (255 & hashCode()) * 1_000_000;
    int invalidMask = (hashCode() & -1) + 1;
    int mapAndOrdinal = map.size() + day.ordinal();
    int bitCount = Integer.bitCount(hashCode()) * Integer.MAX_VALUE;
    int alwaysAbove = (Integer.bitCount(hashCode()) + 2) * Integer.MAX_VALUE; // Noncompliant
//                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    int alwaysBelow = Integer.MIN_VALUE - (Integer.bitCount(hashCode()) + 1); // Noncompliant
//                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    long longBitCount = (Long.bitCount(System.nanoTime()) + 2L) * Long.MAX_VALUE; // Noncompliant
//                      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    long unsignedLow = System.nanoTime() & 0xFFFFFFFFL;
    long unsignedSquare = unsignedLow * unsignedLow;
    return safe + twoBounded + lengthPlusOne + possibleOverflow + wrappedUnknown + mask + reverseMask + invalidMask + mapAndOrdinal + bitCount
      + alwaysAbove + alwaysBelow + (int) longBitCount + (int) unsignedSquare;
  }

  int effectivelyFinal() {
    int value = Integer.MAX_VALUE;
    return value + 1; // Noncompliant
//         ^^^^^^^^^
  }

  int reassigned() {
    int value = Integer.MAX_VALUE;
    value = 0;
    return value + 1;
  }

  int unknownNegation(int value) {
    return -value;
  }

  long literalNegation() {
    int minimumLiteral = -2147483648;
    long minimumLongLiteral = -9223372036854775808L;
    int belowMinimumLiteral = -2147483648 - 1; // Noncompliant
//                            ^^^^^^^^^^^^^^^
    return minimumLiteral + minimumLongLiteral + belowMinimumLiteral;
  }

  long parenthesizedSinks() {
    long parenthesized = (Integer.MAX_VALUE + 1); // Noncompliant
//                        ^^^^^^^^^^^^^^^^^^^^^
    consume((Integer.MAX_VALUE + 1)); // Noncompliant
//           ^^^^^^^^^^^^^^^^^^^^^
    return (Integer.MAX_VALUE + 1); // Noncompliant
//          ^^^^^^^^^^^^^^^^^^^^^
  }

  long lambdaReturn() {
    IntSupplier supplier = () -> {
      return Integer.MAX_VALUE + 1; // Noncompliant
//           ^^^^^^^^^^^^^^^^^^^^^
    };
    return supplier.getAsInt();
  }

  int ordinaryReturn() {
    return Integer.MIN_VALUE - 1; // Noncompliant
//         ^^^^^^^^^^^^^^^^^^^^^
  }

  Comparator<Integer> comparator = (left, right) -> left - right;
  Comparator<Integer> constantComparator = (left, right) -> Integer.MIN_VALUE - 1;
  Comparator<Integer> castComparator = (left, right) -> (int) (Integer.MIN_VALUE - 1);
  IntBinaryOperator nonComparator = (left, right) -> Integer.MIN_VALUE - 1; // Noncompliant
//                                                   ^^^^^^^^^^^^^^^^^^^^^

  Comparator<Integer> blockComparator = (left, right) -> {
    return Integer.MIN_VALUE - 1;
  };

  Comparator<Integer> comparatorWithIntermediate = (left, right) -> {
    int overflow = Integer.MIN_VALUE - 1; // Noncompliant
//                 ^^^^^^^^^^^^^^^^^^^^^
    if (overflow == 0) {
      return 0;
    }
    return left - right;
  };

  int minimumNegation = -Integer.MIN_VALUE;
  long minimumLongNegation = -Long.MIN_VALUE;
  final int minimumAlias = Integer.MIN_VALUE;
  int minimumAliasNegation = -minimumAlias; // Noncompliant
//                           ^^^^^^^^^^^^^
  int parenthesizedMinimumNegation = -(Integer.MIN_VALUE); // Noncompliant
//                                   ^^^^^^^^^^^^^^^^^^^^
  long widenedMinimumAliasNegation = -minimumAlias; // Noncompliant
//                                   ^^^^^^^^^^^^^
  Date minimumAliasTimestamp = new Date(-minimumAlias);
  Instant minimumAliasInstant = Instant.ofEpochMilli((long) -minimumAlias);

  int exactMethods() {
    return Math.addExact(Integer.MAX_VALUE, 1)
      + Math.subtractExact(Integer.MIN_VALUE, 1)
      + Math.multiplyExact(Integer.MAX_VALUE, 2)
      + Math.negateExact(Integer.MIN_VALUE);
  }

  static class LongHolder {
    long value;

    LongHolder(long value) {
      this.value = value;
    }
  }

  static class ComparatorImplementation implements Comparator<Integer> {
    @Override
    public int compare(Integer left, Integer right) {
      return Integer.MIN_VALUE - 1;
    }
  }

  enum Day {
    MONDAY
  }
}
