package checks;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.stream.Stream;

class InvalidComparatorMethodReferenceCheckSample {

  void noncompliant(List<Integer> list, Integer[] array, Stream<Integer> stream) {
    list.sort(Math::min); // Noncompliant {{Replace this method reference; "Math::min" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//            ^^^^^^^^^
    list.sort(Math::max); // Noncompliant {{Replace this method reference; "Math::max" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//            ^^^^^^^^^
    list.sort(Integer::min); // Noncompliant {{Replace this method reference; "Integer::min" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//            ^^^^^^^^^^^^
    list.sort(Integer::max); // Noncompliant {{Replace this method reference; "Integer::max" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//            ^^^^^^^^^^^^
    list.sort(StrictMath::min); // Noncompliant {{Replace this method reference; "StrictMath::min" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//            ^^^^^^^^^^^^^^^
    list.sort(StrictMath::max); // Noncompliant {{Replace this method reference; "StrictMath::max" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//            ^^^^^^^^^^^^^^^
    list.sort(java.lang.Math::min); // Noncompliant {{Replace this method reference; "Math::min" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//            ^^^^^^^^^^^^^^^^^^^

    stream.sorted(Integer::max); // Noncompliant {{Replace this method reference; "Integer::max" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//                ^^^^^^^^^^^^
    stream.sorted(Math::min); // Noncompliant {{Replace this method reference; "Math::min" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//                ^^^^^^^^^

    Collections.sort(list, Math::min); // Noncompliant {{Replace this method reference; "Math::min" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//                         ^^^^^^^^^
    Arrays.sort(array, Math::min); // Noncompliant {{Replace this method reference; "Math::min" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//                     ^^^^^^^^^

    Comparator<Integer> c1 = Math::min; // Noncompliant {{Replace this method reference; "Math::min" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//                           ^^^^^^^^^
    Comparator<Integer> c2 = Integer::max; // Noncompliant {{Replace this method reference; "Integer::max" returns an extremum rather than a comparison result, violating the "Comparator" contract.}}
//                           ^^^^^^^^^^^^
  }

  void compliant(List<Integer> list, Stream<Integer> stream) {
    list.sort(Comparator.naturalOrder());
    list.sort(Comparator.reverseOrder());
    list.sort(Integer::compare);
    list.sort((a, b) -> Integer.compare(a, b));

    stream.sorted(Comparator.naturalOrder());
    stream.sorted(Integer::compare);

    BinaryOperator<Integer> operator = Math::min;
    IntBinaryOperator intOperator = Math::min;
    BinaryOperator<Integer> maxOp = Integer::max;

    CustomExtremum custom = new CustomExtremum();
    list.sort(custom::min);
  }

  static class CustomExtremum {
    int min(int a, int b) {
      return a - b;
    }
  }
}
