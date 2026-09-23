package checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

class CollectionsSortCheckSample {

  void noncompliant(List<String> myList, Comparator<String> myComparator) {
    Collections.sort(myList); // Noncompliant {{Replace this "Collections.sort()" with "List.sort()".}}
//              ^^^^
    Collections.sort(myList, Comparator.naturalOrder()); // Noncompliant {{Replace this "Collections.sort()" with "List.sort()".}}
//              ^^^^
    Collections.sort(myList, myComparator); // Noncompliant {{Replace this "Collections.sort()" with "List.sort()".}}
//              ^^^^
  }

  void noncompliantSubtypes() {
    ArrayList<String> arrayList = new ArrayList<>();
    Collections.sort(arrayList); // Noncompliant
//              ^^^^

    LinkedList<Integer> linkedList = new LinkedList<>();
    Collections.sort(linkedList); // Noncompliant
//              ^^^^
  }

  void noncompliantMethodReturn() {
    Collections.sort(getList()); // Noncompliant
//              ^^^^
  }

  void noncompliantFullyQualified(List<String> myList) {
    java.util.Collections.sort(myList); // Noncompliant
  }

  void compliant(List<String> myList, Comparator<String> comparator) {
    myList.sort(null);
    myList.sort(comparator);
    Collections.shuffle(myList);
    Collections.reverse(myList);
    Collections.unmodifiableList(myList);
    int[] array = {3, 1, 2};
    Arrays.sort(array);
  }

  void compliantCustomClass(List<String> list) {
    MyCollections.sort(list);
  }

  private List<String> getList() {
    return new ArrayList<>();
  }

  static class MyCollections {
    static <T> void sort(List<T> list) {
    }
  }
}
