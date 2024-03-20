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

  public HashSet<Employee> employees = new HashSet<>();          // Noncompliant [[sc=10;ec=17;quickfixes=HashSet1]] {{The type of "employees" should be an interface such as "Set" rather than the implementation "HashSet".}}
                                                                 // fix@HashSet1 {{Replace "HashSet" by "Set"}}
                                                                 // edit@HashSet1 [[sc=10;ec=17]] {{Set}}
  private HashSet<Employee> employees2 = new HashSet<Employee>();

  public HashSet<Employee> getEmployees() { return employees; }  // Noncompliant {{The return type of this method should be an interface such as "Set" rather than the implementation "HashSet".}}

  public LinkedList<Employee> foo1() { return null; }            // Noncompliant [[sc=10;ec=20;quickfixes=LinkedList]] {{The return type of this method should be an interface such as "List" rather than the implementation "LinkedList".}}
                                                                 // fix@LinkedList {{Replace "LinkedList" by "List"}}
                                                                 // edit@LinkedList [[sc=10;ec=20]] {{List}}
                                                                 // edit@LinkedList [[sl=5;sc=29;el=5;ec=29]] {{\nimport java.util.List;}}

  private LinkedList<Employee> foo2() { return null; }

  public java.util.HashSet<Employee> foo3() { return null; }      // Noncompliant [[sc=10;ec=27;quickfixes=HashSet2]] {{The return type of this method should be an interface such as "Set" rather than the implementation "HashSet".}}
                                                                  // fix@HashSet2 {{Replace "HashSet" by "Set"}}
                                                                  // edit@HashSet2 [[sc=10;ec=27]] {{Set}}

  public HashMap foo4() { return null; }                          // Noncompliant [[sc=10;ec=17]] {{The return type of this method should be an interface such as "Map" rather than the implementation "HashMap".}}

  private Stack stack;
  private Vector vector;
  public LinkedList<Employee> publicList; // Noncompliant {{The type of "publicList" should be an interface such as "List" rather than the implementation "LinkedList".}}
  private LinkedList<Employee> privateList;

  public ConcurrentHashMap<?,?> concurrentHashMap() { return null; } // Noncompliant [[sc=10;ec=27;quickfixes=ConcurrentHashMap]] {{The return type of this method should be an interface such as "ConcurrentMap" rather than the implementation "ConcurrentHashMap".}}
                                                                     // fix@ConcurrentHashMap {{Replace "ConcurrentHashMap" by "ConcurrentMap"}}
                                                                     // edit@ConcurrentHashMap [[sc=10;ec=27]] {{ConcurrentMap}}
                                                                     // edit@ConcurrentHashMap [[sl=12;sc=47;el=12;ec=47]] {{\nimport java.util.concurrent.ConcurrentMap;}}

  public ConcurrentSkipListMap concurrentSkipListMap() { return null; } // Noncompliant {{The return type of this method should be an interface such as "ConcurrentMap" rather than the implementation "ConcurrentSkipListMap".}}

  private void method1(LinkedList<Employee> employees) {}
  public void method2(LinkedList<Employee> employees) {} // Noncompliant {{The type of "employees" should be an interface such as "List" rather than the implementation "LinkedList".}}

  class A {
    public void foo(HashMap<String, String> map) { } // Noncompliant [[sc=21;ec=28;quickfixes=HashMap]]
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

  public void foo1(TreeMap<String, String> map) { } // Noncompliant, no TreeMap specific API used

  public Map.Entry<String, String> foo2(TreeMap<String, String> map) { // Compliant, TreeMap specific API used
    return map.lowerEntry("bar");
  }

  public void foo3(TreeSet<Integer> set) { } // Noncompliant, no TreeSet specific API used

  public Integer foo4(TreeSet<Integer> set) { // Compliant, TreeSet specific API used
    return set.ceiling(42);
  }

  public Integer foo5(TreeSet<Integer> set) { // Noncompliant, no TreeSet specific API used
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

  public Integer foo7(LinkedList<Integer> dq, boolean condition) { // Noncompliant, no LinkedList specific API used
    if (condition) {
      return dq.size();
    } else {
      var result = (Integer) (dq.toArray()[23]);
      return result;
    }
  }

  public Map.Entry<String, String> foo8(TreeMap<String, String> map) { // Noncompliant due to current limitation of the rule
    TreeMap<String, String> map2 = map;
    return map2.lowerEntry("bar");
  }

  public Map.Entry<String, String> foo9(TreeMap<String, String> map) { // Noncompliant due to current limitation of the rule
    var map2 = map;
    return map2.lowerEntry("bar");
  }

  public Integer foo10(LinkedList<Integer> l1, LinkedList<Integer> l2, LinkedList<Integer> l3) { // Compliant, LinkedList (Queue) specific API used
    return l1.poll() + l2.poll() + l3.poll();
  }

  public Integer foo11(LinkedList<Integer> l1, LinkedList<Integer> l2, LinkedList<Integer> l3) { // Noncompliant [[sc=48;ec=58]]
    return l1.poll() + l2.get(0) + l3.poll();
  }

  // Noncompliant@+2 [[sc=24;ec=34]]
  // Noncompliant@+1 [[sc=72;ec=82]]
  public Integer foo12(LinkedList<Integer> l1, LinkedList<Integer> l2, LinkedList<Integer> l3) {
    return l1.get(0) + l2.poll() + l3.get(0);
  }

  // Noncompliant@+3 [[sc=33;ec=43]]
  // Noncompliant@+2 [[sc=57;ec=67]]
  // Noncompliant@+1 [[sc=81;ec=91]]
  public abstract Integer foo13(LinkedList<Integer> l1, LinkedList<Integer> l2, LinkedList<Integer> l3);

  public Integer foo14(LinkedList<Integer> list) { // Noncompliant [[sc=24;ec=34]]
    return getList().poll();
  }

  private LinkedList<Integer> getList() {
    return null;
  }
}
