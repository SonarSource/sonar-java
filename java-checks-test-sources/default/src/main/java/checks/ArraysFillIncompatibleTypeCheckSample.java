package checks;

import java.util.Arrays;

class ArraysFillIncompatibleTypeCheckSample {

  interface InterfaceA {}
  interface InterfaceB {}
  static class NonFinalBase {}
  static class UnrelatedClass {}

  void compliantExamples(String[] textBuffer, Integer[] numbers, Number[] numBuffer, Object[] objBuffer,
      CharSequence charSeq, Object objVal, java.util.List<String>[] listArray, java.util.List<Integer> intList,
      short s, int i, Long[] longs, Long longObj, Short shortObj, boolean flag, boolean outer,
      InterfaceA[] ifaceAArray, InterfaceB ifaceB, InterfaceB[] ifaceBArray, InterfaceA[][] ifaceA2D,
      NonFinalBase nonFinalBase, NonFinalBase[] nonFinalBaseArray, NonFinalBase[][] nonFinalBase2D) {
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

    // Overlapping interfaces and non-final classes
    Arrays.fill(ifaceAArray, ifaceB); // Compliant: unrelated interfaces can share an implementation
    Arrays.fill(nonFinalBaseArray, ifaceB); // Compliant: non-final class subclass can implement interface
    Arrays.fill(ifaceAArray, nonFinalBase); // Compliant: non-final class subclass can implement interface
    Arrays.fill(ifaceA2D, ifaceBArray); // Compliant: array component types can share implementation
    Arrays.fill(nonFinalBase2D, ifaceBArray); // Compliant: array component types can share implementation

    // Nested reference and primitive ternaries
    Arrays.fill(textBuffer, flag ? (outer ? "a" : "b") : "c"); // Compliant: all branches compatible
    Arrays.fill(numbers, flag ? (outer ? s : i) : 42); // Compliant: promoted numeric ternary inside reference ternary

    int[] primitiveInts = new int[5];
    Arrays.fill(primitiveInts, 10); // Compliant: primitive array overload
    Arrays.fill(primitiveInts, 0, 2, 10); // Compliant: primitive array overload
  }

  void noncompliantExamples(String[] textBuffer, Integer[] numbers, Number[] numBuffer,
      boolean flag, boolean outer, boolean inner, InterfaceA[] ifaceAArray, InterfaceB ifaceB,
      InterfaceB[] ifaceBArray, NonFinalBase[] nonFinalBaseArray, UnrelatedClass unrelatedVal, String[][] str2D) {
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
    Arrays.fill(textBuffer, ifaceB); // Noncompliant {{An array of type "String[]" cannot be filled with a value of type "InterfaceB".}}
//         ^^^^ ^^^^^^^^^^<
    Arrays.fill(ifaceAArray, "string"); // Noncompliant {{An array of type "InterfaceA[]" cannot be filled with a value of type "String".}}
//         ^^^^ ^^^^^^^^^^^<
    Arrays.fill(nonFinalBaseArray, unrelatedVal); // Noncompliant {{An array of type "NonFinalBase[]" cannot be filled with a value of type "UnrelatedClass".}}
//         ^^^^ ^^^^^^^^^^^^^^^^^<
    Arrays.fill(str2D, ifaceBArray); // Noncompliant {{An array of type "String[][]" cannot be filled with a value of type "InterfaceB[]".}}
//         ^^^^ ^^^^^<
    Arrays.fill(textBuffer, flag ? (outer ? 42 : "ok") : "ok"); // Noncompliant {{An array of type "String[]" cannot be filled with a value of type "int".}}
//         ^^^^ ^^^^^^^^^^<
    Arrays.fill(textBuffer, flag ? "ok" : (inner ? 42 : "ok")); // Noncompliant {{An array of type "String[]" cannot be filled with a value of type "int".}}
//         ^^^^ ^^^^^^^^^^<
  }

  <T> void generics(T[] array, T value, String[] stringArray) {
    Arrays.fill(array, value); // Compliant: type variable
    Arrays.fill(array, "test"); // Compliant: type variable array
    Arrays.fill(stringArray, value); // Compliant: filling type is type variable
  }
}
