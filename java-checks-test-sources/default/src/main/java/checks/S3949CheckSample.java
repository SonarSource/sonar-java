package checks;

import java.util.Comparator;
import java.util.List;

class S3949CheckSample {

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

  int exactIntMaximum = Integer.MAX_VALUE - 1;
  int exactIntMinimum = Integer.MIN_VALUE + 1;
  long exactLongMaximum = Long.MAX_VALUE - 1L;
  long widenedBefore = (long) Integer.MAX_VALUE + 1;
  long ownedByS2184 = Integer.MAX_VALUE + 1;

  long ownedReturn() {
    return Integer.MAX_VALUE + 1;
  }

  void widenedArguments() {
    consume(Integer.MAX_VALUE + 1);
    new LongHolder(Integer.MAX_VALUE + 1);
  }

  void consume(long value) {
  }

  int unknown(int left, int right) {
    int arbitrary = left + right;
    return arbitrary;
  }

  int midpoint(int low, int high) {
    int result = (low + high) / 2; // Noncompliant
//                ^^^^^^^^^^
    int safe = low + (high - low) / 2;
    int shifted = (low + high) >>> 1;
    return result + safe + shifted;
  }

  int bounded(String text, List<String> values, int[] array) {
    int safe = text.length() + values.size();
    int unsafe = array.length * Integer.MAX_VALUE; // Noncompliant
//               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    int mask = (hashCode() & 255) * 1_000_000;
    return safe + unsafe + mask;
  }

  int effectivelyFinal() {
    int value = Integer.MAX_VALUE;
    return value + 1; // Noncompliant
//         ^^^^^^^^^
  }

  Comparator<Integer> comparator = (left, right) -> left - right;

  int minimumNegation = -Integer.MIN_VALUE;
  long minimumLongNegation = -Long.MIN_VALUE;

  int exactMethods() {
    return Math.addExact(Integer.MAX_VALUE, 1)
      + Math.subtractExact(Integer.MIN_VALUE, 1)
      + Math.multiplyExact(Integer.MAX_VALUE, 2)
      + Math.negateExact(Integer.MIN_VALUE);
  }

  static class LongHolder {
    LongHolder(long value) {
    }
  }
}
