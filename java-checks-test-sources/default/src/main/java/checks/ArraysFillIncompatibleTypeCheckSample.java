package checks;

import java.util.Arrays;

class ArraysFillIncompatibleTypeCheckSample {

  void compliantExamples(String[] textBuffer, Integer[] numbers, Number[] numBuffer, Object[] objBuffer,
      CharSequence charSeq, Object objVal, java.util.List<String>[] listArray, java.util.List<Integer> intList,
      short s, int i, Long[] longs, Long longObj, Short shortObj, boolean flag) {
    Arrays.fill(textBuffer, "default"); // Compliant
    Arrays.fill(textBuffer, 0, 5, "default"); // Compliant
    Arrays.fill(textBuffer, null); // Compliant: null can be stored in reference array
    Arrays.fill(numbers, 0); // Compliant: primitive int boxed to Integer
    Arrays.fill(numBuffer, 42); // Compliant: Integer is a Number
    Arrays.fill(objBuffer, 42); // Compliant: Integer is an Object
    Arrays.fill(objBuffer, "string"); // Compliant
    Arrays.fill(textBuffer, true ? "a" : "b"); // Compliant

    // Supertype filling value (may be compatible instance at runtime)
    Arrays.fill(textBuffer, charSeq); // Compliant: CharSequence could be a String at runtime
    Arrays.fill(numbers, objVal); // Compliant: Object could be an Integer at runtime

    // Differently-parameterized generic types (erased to raw List at runtime)
    Arrays.fill(listArray, intList); // Compliant: erased types are both List

    // Numeric ternary promotion
    Arrays.fill(numbers, true ? s : i); // Compliant: ternary promoted to int, which boxes to Integer
    Arrays.fill(longs, flag ? 0 : longObj); // Compliant: promoted to long, which boxes to Long
    Arrays.fill(numbers, flag ? shortObj : i); // Compliant: promoted to int, which boxes to Integer

    int[] primitiveInts = new int[5];
    Arrays.fill(primitiveInts, 10); // Compliant: primitive array overload
    Arrays.fill(primitiveInts, 0, 2, 10); // Compliant: primitive array overload
  }

  void noncompliantExamples(String[] textBuffer, Integer[] numbers, Number[] numBuffer) {
    Arrays.fill(textBuffer, 42); // Noncompliant {{An array of type "String[]" cannot be filled with a value of type "int".}}
//         ^^^^ ^^^^^^^^^^<
    Arrays.fill(textBuffer, 0, 5, 42); // Noncompliant {{An array of type "String[]" cannot be filled with a value of type "int".}}
//         ^^^^ ^^^^^^^^^^<
    Arrays.fill(numbers, "default"); // Noncompliant {{An array of type "Integer[]" cannot be filled with a value of type "String".}}
//         ^^^^ ^^^^^^^<
    Arrays.fill(numbers, 2.0d); // Noncompliant {{An array of type "Integer[]" cannot be filled with a value of type "double".}}
//         ^^^^ ^^^^^^^<
    Arrays.fill(numBuffer, "text"); // Noncompliant {{An array of type "Number[]" cannot be filled with a value of type "String".}}
//         ^^^^ ^^^^^^^^^<
    Arrays.fill(textBuffer, true ? "a" : 123); // Noncompliant {{An array of type "String[]" cannot be filled with a value of type "int".}}
//         ^^^^ ^^^^^^^^^^<
    Arrays.fill(textBuffer, true ? 456 : "b"); // Noncompliant {{An array of type "String[]" cannot be filled with a value of type "int".}}
//         ^^^^ ^^^^^^^^^^<
    Arrays.fill(textBuffer, true ? 123 : 456); // Noncompliant {{An array of type "String[]" cannot be filled with a value of type "int".}}
//         ^^^^ ^^^^^^^^^^<
  }

  <T> void generics(T[] array, T value, String[] stringArray) {
    Arrays.fill(array, value); // Compliant: type variable
    Arrays.fill(array, "test"); // Compliant: type variable array
    Arrays.fill(stringArray, value); // Compliant: filling type is type variable
  }
}
