package checks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
                                                                     // edit@ConcurrentHashMap [[sl=11;sc=47;el=11;ec=47]] {{\nimport java.util.concurrent.ConcurrentMap;}}

  public ConcurrentSkipListMap concurrentSkipListMap() { return null; } // Noncompliant {{The return type of this method should be an interface such as "ConcurrentMap" rather than the implementation "ConcurrentSkipListMap".}}

  private void method1(LinkedList<Employee> employees) {}
  public void method2(LinkedList<Employee> employees) {} // Noncompliant {{The type of "employees" should be an interface such as "List" rather than the implementation "LinkedList".}}

  class A {
    public void foo(HashMap<String, String> map) { } // Noncompliant [[sc=21;ec=28;quickfixes=HashMap]]
                                                     // fix@HashMap {{Replace "HashMap" by "Map"}}
                                                     // edit@HashMap [[sc=21;ec=28]] {{Map}}
                                                     // edit@HashMap [[sl=5;sc=29;el=5;ec=29]] {{\nimport java.util.Map;}}
  }

  class B extends A {
    @Override
    public void foo(HashMap<String, String> map) { } // Compliant method is inherited
  }

  public void function(TreeSet<Object> treeSet, // Noncompliant
    TreeMap<Object, Object> treeMap) { // Noncompliant

  }

}

class Employee {

  public Employee() { }

  private Set<Employee> employees = new HashSet<Employee>();      // Compliant

  public Set<Employee> getEmployees(){                            // Compliant
    return employees;
  }
}
