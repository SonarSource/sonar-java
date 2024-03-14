package checks;

import java.util.List;
import java.util.ListIterator;

public class ReversedSequencedCollectionCheckSample {

  void unreachable(List<String> list) {
    for (int i = list.size() - 1; ; i = i - 1) { // Compliant
      var element = list.get(i);
    }
  }

  void coverage(List<String> list, ListIterator<String> iterator) {

    int m;

    for (int i = list.size() - 1; i >= 0; m = 3) { // Complaint
      var element = list.get(i);
    }

    for (int i = list.size() - 1; i >= 0; ) { // Complaint
      var element = list.get(i);
    }

    for (int i = list.size() - getInt(); i >= 0; i = i - 1) { // Complaint
      var element = list.get(i);
    }

    for (int i = 1 - 1; i >= 0; i -= 1) { // Compliant - coverage
      var element = list.get(i);
    }

    for (int i = getInt() - 1; i >= 0; i = i - 1) { // Compliant - coverage
      var element = list.get(i);
    }

    int b = list.size() - 1;
    for (b++; b >= 0; b--) { // Compliant
      var element = list.get(b);
    }

    for (int c; b >= 0; b--) { // Compliant
      var element = list.get(b);
    }

    for (int i = list.size() + 1; i >= 0; i -= 1) { // Compliant
      var element = list.get(i);
    }

    for (int i = list.size() - 1; i >= 0; i -= 3) { // Compliant
      var element = list.get(i);
    }

    for (int i = list.size() - 1; i >= 0; i = i - 3) { // Compliant
      var element = list.get(i);
    }

    for (int i = list.size() - 1; i >= 0; i = i + 3) { // Compliant
      var element = list.get(i);
    }

    for (; b >= 0; b--) { // Compliant
      var element = list.get(b);
    }

    boolean isTrue = false;
    for (var it = list.listIterator(list.size()); isTrue;) { // Compliant - coverage
      var element = it.previous();
    }

    for (var it = list.listIterator(list.size()); getBoolean();) { // Compliant - coverage
      var element = it.previous();
    }

    for (int i = list.size() - 1; getInt() >= 0; i--) { // Complaint - coverage
      var element = list.get(i);
    }

    for (int i = list.size() - 10; i >= 0; i--) { // Compliant - loop doesn't iterate over the whole list
      var element = list.get(i);
    }

    for (int i = list.size() - 10; i >= 0; i++) { // Compliant - incrementing
      var element = list.get(i);
    }

    for (int i = list.size() - 1; i >= 0; i = i - 2) { // Compliant - coverage
      var element = list.get(i);
    }

    for (; iterator.hasPrevious();) { // Compliant
      var element = iterator.previous();
    }

    for (int a; iterator.hasPrevious();) { // Compliant - coverage
      var element = iterator.previous();
    }

    int c = 0;
    for (c++; iterator.hasPrevious();) { // Compliant - coverage
      var element = iterator.previous();
    }

    for (int a = getInt(); iterator.hasPrevious();) { // Compliant - coverage
      var element = iterator.previous();
    }

    for (int a = 1; iterator.hasPrevious();) { // Compliant - coverage
      var element = iterator.previous();
    }

    for (var it = list.listIterator(getInt()); it.hasPrevious();) { // Compliant - coverage
      var element = it.previous();
    }

    for (var it = list.listIterator(5); it.hasPrevious();) { // Compliant - coverage
      var element = it.previous();
    }

    for (;;) { // Compliant
    }
  }

  void printLastToFirst(List<String> list, ListIterator<String> listIterator) {
    for (int i = list.size() - 1; i >= 0; i -= 1) { // Noncompliant
      var element = list.get(i);
    }

    for (int i = list.size() - 1; i >= 0; i = i - 1) { // Noncompliant
      var element = list.get(i);
    }

    int size = list.size();
    for (var it = list.listIterator(size); it.hasPrevious();) { // Compliant - size is unknown
      var element = it.previous();
    }

    for (var it = list.listIterator(list.size()); it.hasPrevious();) { // Noncompliant
      var element = it.previous();
    }

    for (int i = list.size() - 1; i >= 0; i--) { // Noncompliant
      var element = list.get(i);
    }

    for (int i = list.size() - 1; i >= 0; i--) { // Noncompliant
      var element = list.get(i);
    }

    for (int i = list.size() - 1; i > 0; i--) { // Complaint - not including first element
      var element = list.get(i);
    }

    // while statement
    while (list.listIterator(list.size()).hasPrevious()) { // Noncompliant
      var element = list.listIterator(list.size()).previous();
    }

    while (list.listIterator(list.size()).hasNext()) { // Compliant
      var element = list.listIterator(list.size()).previous();
    }

    boolean isTrue2 = false;
    while (isTrue2) { // Compliant
      var element = listIterator.previous();
    }

    var it = list.listIterator(list.size());
    do {
      var element = it.previous();
    } while (list.listIterator(list.size()).hasPrevious());

    // for each statement
    for (var element : list) { // Compliant

    }

    // for each statement
    for (var element : list.reversed()) { // Compliant
    }

  }

  boolean getBoolean() {
    return false;
  }

  int getInt() {
    return 0;
  }
}
