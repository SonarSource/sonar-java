
import java.lang.Object;
import java.util.List;

public class A {
  private List<String> getList() {
    return null;
  }

  private List<String> globalStrings;

  public void doSomethingToAList(List<String> strings, List<String> strings2) {
    for (String str : strings);
    foo();
    for (String str : strings); // Noncompliant {{Combine this loop with the one that starts on line 13.}}
//                    ^^^^^^^
    strings = null;
    for (String str : strings); // strings get reassigned
    for (String str : getList());
    for (String str : getList()); // Noncompliant {{Combine this loop with the one that starts on line 18.}}
    foo();
    for (String str : getList()); // no issue if something in between for loops
    for (String str : foo());
    List<String> list = null;
    for (String str : list);
    for (String str : list); // Noncompliant {{Combine this loop with the one that starts on line 24.}}
    if (list.size() > 10) {
      list.remove(0);
    }
    for (String str : list);
    if (true) {
      for (String str : strings2);
    } else {
      for (String str : strings2);
    }
    getList().remove(0);
    Object obj = new Object() {
      void foo() {
        for (String str : strings) {
          doStep2(str);
        }
      }
    };
    for (String str : globalStrings);
    modifyGlobalStrings();
    // no issues for fields
    for (String str : globalStrings);
  }
}
