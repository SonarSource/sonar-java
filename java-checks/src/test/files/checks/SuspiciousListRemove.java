package test;

import java.util.List;

public class SuspiciousListRemove {

  int x;

  void removeFrom(List<String> list) {
    // expected: iterate over all the elements of the list
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).isEmpty()) {
        // actual: remaining elements are shifted, so the one immediately following will be skipped
        list.remove(i); // Noncompliant  {{Verify that "remove()" is used correctly.}}
      }
    }
  }

  void removeFrom(List<String> list, int from) {
    for (int i = from; i < list.size(); i++)
      list.remove(i); // Noncompliant
  }


  void descending(List<String> list) {
    for (int i = list.size() - 1; i >= 0; i--) {
      if (list.get(i).isEmpty()) {
        list.remove(i);  // Compliant
      }
    }
  }

  void shiftCounter(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).isEmpty()) {
        list.remove(i);  // Compliant because counter is assigned
        i--;
      }
    }
  }

  void shiftCounter(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).isEmpty()) {
        list.remove(i);  // Compliant because counter is assigned
        i -= 1;
      }
    }
  }

  void controlFlow(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).isEmpty()) {
        list.remove(i);  // Compliant because control flow
        break;
      }
    }
  }

  void controlFlow2(List<String> list) {
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).isEmpty()) {
        list.remove(i);  // Compliant because control flow
        continue;
      }
    }
  }

  void coverage1(List<String> list, int from) {
    for (this.x = from; this.x < list.size(); this.x++)
      list.remove(i); // Consider only local vars
  }

  void coverage2(List<String> list) {
    for (int i,j = 0; i < list.size(); i++)
      list.remove(i); // Consider only simple initialization
  }

  void coverage2(List<String> list) {
    for (int i = 0; i < list.size(); i++, this.x++)
      list.remove(i); // Invalid update
  }

  void noRemove(List<String> list, int from) {
    for (int i = from; i < list.size(); i++) {
      list.get(i);
      this.x++;
      this.x += 1;
    }
  }

  void noUpdate(List<String> list, int from) {
    for (int i = from; i < list.size(); )
      list.get(i);

    for (int i = from; i < list.size(); this.x++)
      list.get(i);

    int j = 0;
    for (int i = from; i < list.size(); j++)
      list.get(i);
  }
}
