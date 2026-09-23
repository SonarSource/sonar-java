package checks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

class CollectionsSortCheckSample {

  void noncompliant(List<String> myList, Comparator<String> myComparator) {
    Collections.sort(myList); // Noncompliant {{Replace this "Collections.sort()" with "List.sort()".}} [[quickfixes=qf1]]
//              ^^^^
    // fix@qf1 {{Use "myList.sort(null)" instead}}
    // edit@qf1 [[sc=5;ec=29]] {{myList.sort(null)}}
    Collections.sort(myList, Comparator.naturalOrder()); // Noncompliant {{Replace this "Collections.sort()" with "List.sort()".}} [[quickfixes=qf2]]
//              ^^^^
    // fix@qf2 {{Use "myList.sort(Comparator.naturalOrder())" instead}}
    // edit@qf2 [[sc=5;ec=56]] {{myList.sort(Comparator.naturalOrder())}}
    Collections.sort(myList, myComparator); // Noncompliant {{Replace this "Collections.sort()" with "List.sort()".}} [[quickfixes=qf3]]
//              ^^^^
    // fix@qf3 {{Use "myList.sort(myComparator)" instead}}
    // edit@qf3 [[sc=5;ec=43]] {{myList.sort(myComparator)}}
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

  void noncompliantComplexExpression(boolean flag, List<String> a, List<String> b) {
    Collections.sort(flag ? a : b); // Noncompliant [[quickfixes=qf4]]
//              ^^^^
    // fix@qf4 {{Use "(flag ? a : b).sort(null)" instead}}
    // edit@qf4 [[sc=5;ec=35]] {{(flag ? a : b).sort(null)}}
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
