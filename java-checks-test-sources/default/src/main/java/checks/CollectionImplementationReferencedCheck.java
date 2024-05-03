package checks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

class EmployeesTopLevel {

  public EmployeesTopLevel(String s) { }

  public HashSet<Employee> employees = new HashSet<>(); // Noncompliant {{The type of "employees" should be an interface such as "Set" rather than the implementation "HashSet".}} [[quickfixes=HashSet1]]
//       ^^^^^^^
                                                                 // fix@HashSet1 {{Replace "HashSet" by "Set"}}
                                                                 // edit@HashSet1 [[sc=10;ec=17]] {{Set}}
  private HashSet<Employee> employees2 = new HashSet<Employee>();

  public HashSet<Employee> getEmployees() { return employees; } // Noncompliant {{The return type of this method should be an interface such as "Set" rather than the implementation "HashSet".}}

  public LinkedList<Employee> foo1() { return null; } // Noncompliant {{The return type of this method should be an interface such as "List" rather than the implementation "LinkedList".}} [[quickfixes=LinkedList]]
//       ^^^^^^^^^^
                                                                 // fix@LinkedList {{Replace "LinkedList" by "List"}}
                                                                 // edit@LinkedList [[sc=10;ec=20]] {{List}}
                                                                 // edit@LinkedList [[sl=5;sc=29;el=5;ec=29]] {{\nimport java.util.List;}}

  private LinkedList<Employee> foo2() { return null; }

  public java.util.HashSet<Employee> foo3() { return null; } // Noncompliant {{The return type of this method should be an interface such as "Set" rather than the implementation "HashSet".}} [[quickfixes=HashSet2]]
//       ^^^^^^^^^^^^^^^^^
                                                                  // fix@HashSet2 {{Replace "HashSet" by "Set"}}
                                                                  // edit@HashSet2 [[sc=10;ec=27]] {{Set}}

  public HashMap foo4() { return null; } // Noncompliant {{The return type of this method should be an interface such as "Map" rather than the implementation "HashMap".}}
//       ^^^^^^^

  private Stack stack;
  private Vector vector;
  public LinkedList<Employee> publicList; // Noncompliant {{The type of "publicList" should be an interface such as "List" rather than the implementation "LinkedList".}}
  private LinkedList<Employee> privateList;

  public ConcurrentHashMap<?,?> concurrentHashMap() { return null; } // Noncompliant {{The return type of this method should be an interface such as "ConcurrentMap" rather than the implementation "ConcurrentHashMap".}} [[quickfixes=ConcurrentHashMap]]
//       ^^^^^^^^^^^^^^^^^
                                                                     // fix@ConcurrentHashMap {{Replace "ConcurrentHashMap" by "ConcurrentMap"}}
                                                                     // edit@ConcurrentHashMap [[sc=10;ec=27]] {{ConcurrentMap}}
                                                                     // edit@ConcurrentHashMap [[sl=12;sc=47;el=12;ec=47]] {{\nimport java.util.concurrent.ConcurrentMap;}}

  public ConcurrentSkipListMap concurrentSkipListMap() { return null; } // Noncompliant {{The return type of this method should be an interface such as "ConcurrentMap" rather than the implementation "ConcurrentSkipListMap".}}

  private void method1(LinkedList<Employee> employees) {}
  public void method2(LinkedList<Employee> employees) {} // Noncompliant {{The type of "employees" should be an interface such as "List" rather than the implementation "LinkedList".}}

  class A {
    public void foo(HashMap<String, String> map) { } // Noncompliant [[quickfixes=HashMap]]
//                  ^^^^^^^
                                                     // fix@HashMap {{Replace "HashMap" by "Map"}}
                                                     // edit@HashMap [[sc=21;ec=28]] {{Map}}
  }

  class B extends A {
    @Override
    public void foo(HashMap<String, String> map) { } // Compliant method is inherited
  }
}

class Employee {

  public Employee() { }

  private Set<Employee> employees = new HashSet<Employee>();      // Compliant

  public Set<Employee> getEmployees(){                            // Compliant
    return employees;
  }
}

abstract class ApiEnforcesClassSonarjava4590 {

  public void foo1(TreeMap<String, String> map) { } // Noncompliant

  public Map.Entry<String, String> foo2(TreeMap<String, String> map) { // Compliant, TreeMap specific API used
    return map.lowerEntry("bar");
  }

  public void foo3(TreeSet<Integer> set) { } // Noncompliant

  public Integer foo4(TreeSet<Integer> set) { // Compliant, TreeSet specific API used
    return set.ceiling(42);
  }

  public Integer foo5(TreeSet<Integer> set) { // Noncompliant
    return set.size();
  }

  public Integer foo6(LinkedList<Integer> dq, boolean condition) { // Compliant, LinkedList (Queue) specific API used
    if (condition) {
      return dq.size();
    } else {
      var result = 23 + dq.poll() * 42;
      return result;
    }
  }

  public Integer foo7(LinkedList<Integer> dq, boolean condition) { // Noncompliant
    if (condition) {
      return dq.size();
    } else {
      var result = (Integer) (dq.toArray()[23]);
      return result;
    }
  }

  public Map.Entry<String, String> foo8(TreeMap<String, String> map) { // Noncompliant
    TreeMap<String, String> map2 = map;
    return map2.lowerEntry("bar");
  }

  public Map.Entry<String, String> foo9(TreeMap<String, String> map) { // Noncompliant
    var map2 = map;
    return map2.lowerEntry("bar");
  }

  public Integer foo10(LinkedList<Integer> l1, LinkedList<Integer> l2, LinkedList<Integer> l3) { // Compliant, LinkedList (Queue) specific API used
    return l1.poll() + l2.poll() + l3.poll();
  }

  public Integer foo11(LinkedList<Integer> l1, LinkedList<Integer> l2, LinkedList<Integer> l3) { // Noncompliant
//                                             ^^^^^^^^^^
    return l1.poll() + l2.get(0) + l3.poll();
  }



  public Integer foo12(LinkedList<Integer> l1, LinkedList<Integer> l2, LinkedList<Integer> l3) { // Noncompliant@+1
//                                                                     ^^^^^^^^^^
    return l1.get(0) + l2.poll() + l3.get(0);
  }




  public abstract Integer foo13(LinkedList<Integer> l1, LinkedList<Integer> l2, LinkedList<Integer> l3); // Noncompliant@+1
//                                                                              ^^^^^^^^^^

  public Integer foo14(LinkedList<Integer> list) { // Noncompliant
//                     ^^^^^^^^^^
    return getList().poll();
  }

  private LinkedList<Integer> getList() {
    return null;
  }
}
