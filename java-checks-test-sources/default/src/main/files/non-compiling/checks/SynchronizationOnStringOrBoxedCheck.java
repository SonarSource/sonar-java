package checks;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

class A {

  List<String> list = List.of("", "b","c");
  List<String> list1 = List.copyOf(list);
  List<String> list2 = new ArrayList<>();
  List<String> list3;
  List<String> list4 = list;
  List<String> list5 = Arrays.asList("", "b","c");
  

  Set<String> set = Set.of("a", "b", "c");
  Set<String> set1 = Set.copyOf(set);

  Map<String, String> map = Map.of("a", "b", "c", "d");
  Map<String, String> map1 = Map.copyOf(map);
  Map.Entry<String, Integer> entry = Map.entry("a", 1);
  Map<String, Integer> map2 = Map.ofEntries(entry, Map.entry("b", 2));
  
  private final ProcessHandle processHandle = ProcessHandle.current();


  void method1() {
    list3 = List.of();

    synchronized(42) {  // Noncompliant
      // ...
    }
    synchronized(list) {  // Noncompliant
      // ...
    }
    synchronized(list1) {  // Noncompliant
      // ...
    }
    synchronized(list2) {  // Compliant
      // ...
    }
    synchronized(list3) {  // Compliant
      // ...
    }
    synchronized(list4) {  // Compliant
      // ...
    }
    synchronized(set) {  // Noncompliant
      // ...
    }
    synchronized(set1) {  // Noncompliant
      // ...
    }
    synchronized(map) {  // Noncompliant
      // ...
    }
    synchronized(map1) {  // Noncompliant
      // ...
    }
    synchronized(entry) {  // Noncompliant
      // ...
    }
    synchronized(map2) {  // Noncompliant
      // ...
    }
    synchronized(processHandle) {  // Noncompliant
      // ...
    }
    synchronized(List.of("", "b","c")) {  // Noncompliant
      // ...
    }
    synchronized(list5) {  // Compliant
      // ...
    }
  }
  
}
