package checks;

import java.util.List;
import java.util.ListIterator;

public class ReversedMethodSequencedCollectionCheckSample {

  void printLastToFirst(List<String> list, ListIterator<String> listIterator) {
    for (var it = list.listIterator(list.size()); it.hasPrevious(); ) { // Noncompliant
//  ^^^
      // instead of manually iterating the list in reverse.}}
      var element = it.previous();
    }

    // for each statement
    for (var element : list) { // Compliant

    }

    int size = list.size();
    for (var it = list.listIterator(size); it.hasPrevious(); ) { // Compliant - size is unknown
      var element = it.previous();
    }

    // for each statement
    for (var element : list.reversed()) { // Compliant
    }

  }

  void coverage(List<String> list, ListIterator<String> iterator) {
    boolean isTrue = false;
    for (var it = list.listIterator(list.size()); isTrue; ) { // Compliant - coverage
      var element = it.previous();
    }

    for (var it = list.listIterator(list.size()); getBoolean(); ) { // Compliant - coverage
      var element = it.previous();
    }

    for (; iterator.hasPrevious(); ) { // Compliant
      var element = iterator.previous();
    }

    for (int a; iterator.hasPrevious(); ) { // Compliant - coverage
      var element = iterator.previous();
    }

    int c = 0;
    for (c++; iterator.hasPrevious(); ) { // Compliant - coverage
      var element = iterator.previous();
    }

    for (int a = getInt(); iterator.hasPrevious(); ) { // Compliant - coverage
      var element = iterator.previous();
    }

    for (int a = 1; iterator.hasPrevious(); ) { // Compliant - coverage
      var element = iterator.previous();
    }

    for (var it = list.listIterator(getInt()); it.hasPrevious(); ) { // Compliant - coverage
      var element = it.previous();
    }

    for (var it = list.listIterator(5); it.hasPrevious(); ) { // Compliant - coverage
      var element = it.previous();
    }

    for (var it = list.listIterator(list.size()); it.hasPrevious(); ) { // Noncompliant
    }

    for (var it = list.listIterator(list.size()); it.hasPrevious(); ) { // Noncompliant
      it.previous();
      it.previous();
    }

    for (var it = list.listIterator(list.size()); ; ) { // Compliant
      var element = it.previous();
    }
  }

  boolean getBoolean() {
    return false;
  }

  int getInt() {
    return 0;
  }
}
