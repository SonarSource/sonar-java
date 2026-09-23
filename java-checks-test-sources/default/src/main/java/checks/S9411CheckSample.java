package checks;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

class S9411CheckSample {

  String suffix = "_suffix";

  void noncompliant(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      list.set(i, list.get(i).toUpperCase()); // Noncompliant {{Replace this loop with "List.replaceAll()".}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }
  }

  void noncompliantStaticMethod(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      list.set(i, transform(list.get(i))); // Noncompliant
    }
  }

  void noncompliantTrim(List<String> names) {
    for (int i = 0; i < names.size(); i++) {
      names.set(i, names.get(i).trim()); // Noncompliant
    }
  }

  void noncompliantPrefixIncrement(List<String> list) {
    for (int i = 0; i < list.size(); ++i) {
      list.set(i, list.get(i) + suffix); // Noncompliant
    }
  }

  void noncompliantArrayList(ArrayList<String> list) {
    for (int i = 0; i < list.size(); i++) {
      list.set(i, list.get(i).toLowerCase()); // Noncompliant
    }
  }

  void compliantReplaceAll(List<String> list) {
    list.replaceAll(String::toUpperCase); // compliant
  }

  void compliantNoGet(List<String> list, String defaultValue) {
    for (int i = 0; i < list.size(); i++) {
      list.set(i, defaultValue); // compliant - not a transformation of current element
    }
  }

  void compliantConditionalSet(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).isEmpty()) { // compliant - conditional replacement
        list.set(i, "default");
      }
    }
  }

  void compliantExtraStatement(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      System.out.println(list.get(i)); // compliant - extra side effect
      list.set(i, list.get(i).toUpperCase());
    }
  }

  void compliantNotStartingAtZero(List<String> list) {
    for (int i = 1; i < list.size(); i++) {
      list.set(i, list.get(i).toUpperCase()); // compliant - does not start at 0
    }
  }

  void compliantPartialIteration(List<String> list) {
    for (int i = 0; i < list.size() - 1; i++) {
      list.set(i, list.get(i).toUpperCase()); // compliant - not full list
    }
  }

  void compliantDifferentSetIndex(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      list.set(0, list.get(i).toUpperCase()); // compliant - set index differs from get index
    }
  }

  void compliantDifferentListForSet(List<String> list, List<String> other) {
    for (int i = 0; i < list.size(); i++) {
      other.set(i, list.get(i).toUpperCase()); // compliant - set on different list
    }
  }

  void compliantDifferentListForGet(List<String> list, List<String> other) {
    for (int i = 0; i < list.size(); i++) {
      list.set(i, other.get(i).toUpperCase()); // compliant - get from different list
    }
  }

  void compliantForEach(List<String> list) {
    for (String s : list) { // compliant - for-each loop
      System.out.println(s.toUpperCase());
    }
  }

  void compliantArray(String[] arr) {
    for (int i = 0; i < arr.length; i++) {
      arr[i] = arr[i].toUpperCase(); // compliant - array, not a list
    }
  }

  void compliantWithBreak(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      list.set(i, list.get(i).toUpperCase()); // compliant - loop has break
      break;
    }
  }

  void compliantWithContinue(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i) == null) continue; // compliant - loop has continue
      list.set(i, list.get(i).toUpperCase());
    }
  }

  void compliantWithReturn(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      list.set(i, list.get(i).toUpperCase()); // compliant - loop has return
      return;
    }
  }

  void compliantCounterModified(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      list.set(i, list.get(i).toUpperCase()); // compliant - counter modified in body
      i++;
    }
  }

  void compliantMultipleInitializers() {
    List<String> list = new ArrayList<>();
    for (int i = 0, j = 0; i < list.size(); i++) {
      list.set(i, list.get(i).toUpperCase()); // compliant - multiple initializers
    }
  }

  void compliantMultipleUpdates(List<String> list) {
    int j = 0;
    for (int i = 0; i < list.size(); i++, j++) { // compliant - multiple updates
      list.set(i, list.get(i).toUpperCase());
    }
  }

  void compliantListIterator(List<String> list) {
    ListIterator<String> it = list.listIterator(); // compliant - ListIterator pattern
    while (it.hasNext()) {
      it.set(it.next().toUpperCase());
    }
  }

  void compliantIndexUsedInArithmetic(List<Integer> list) {
    for (int i = 0; i < list.size(); i++) {
      list.set(i, list.get(i) + i); // compliant - index used in arithmetic
    }
  }

  void compliantIndexUsedInAnotherGet(List<Integer> list, List<Integer> weights) {
    for (int i = 0; i < list.size(); i++) {
      list.set(i, list.get(i) * weights.get(i)); // compliant - index used in another list access
    }
  }

  void compliantNonMatchingGetIndex(List<Integer> list) {
    for (int i = 0; i < list.size(); i++) {
      list.set(i, list.get(i) + list.get(i - 1)); // compliant - list.get(i-1) is not list.get(i)
    }
  }

  void noncompliantGreaterThan(List<String> list) {
    for (int i = 0; list.size() > i; i++) {
      list.set(i, list.get(i).toUpperCase()); // Noncompliant
    }
  }

  void compliantNoCondition(List<String> list) {
    for (int i = 0; ; i++) { // compliant - no condition
      if (i >= list.size()) break;
      list.set(i, list.get(i).toUpperCase());
    }
  }

  void compliantNonListSizeCall(List<String> list) {
    int n = list.size();
    for (int i = 0; i < n; i++) {
      list.set(i, list.get(i).toUpperCase()); // compliant - condition is not list.size()
    }
  }

  void compliantChainedReceiver(List<String> list) {
    for (int i = 0; i < getList().size(); i++) {
      getList().set(i, getList().get(i).toUpperCase()); // compliant - receiver is method call, not identifier
    }
  }

  void compliantBodyNotBlock(List<String> list, String val) {
    for (int i = 0; i < list.size(); i++)
      list.set(i, val); // compliant - no list.get()
  }

  void compliantBodyDeclaration(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      String s = list.get(i).toUpperCase(); // compliant - body is not a set call
    }
  }

  void compliantSetNotOnList(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      set(i, list.get(i).toUpperCase()); // compliant - set is not on a list
    }
  }

  void compliantIncrementNotMatching(List<String> list) {
    int j = 0;
    for (int i = 0; i < list.size(); j++) { // compliant - increment is on j, not i
      list.set(i, list.get(i).toUpperCase());
    }
  }

  private List<String> getList() {
    return new ArrayList<>();
  }

  private void set(int i, String val) {
  }

  private static String transform(String s) {
    return s.trim().toLowerCase();
  }
}
