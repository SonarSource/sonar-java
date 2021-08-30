package checks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

class EmployeesTopLevel {

  public EmployeesTopLevel(String s) { }

  public HashSet<Employee> employees = new HashSet<>();          // Noncompliant [[sc=10;ec=17]] {{The type of "employees" should be an interface such as "Set" rather than the implementation "HashSet".}}
  private HashSet<Employee> employees2 = new HashSet<Employee>();

  public HashSet<Employee> getEmployees() { return employees; }  // Noncompliant {{The return type of this method should be an interface such as "Set" rather than the implementation "HashSet".}}

  public LinkedList<Employee> foo1() { return null; }            // Noncompliant [[sc=10;ec=20]] {{The return type of this method should be an interface such as "List" rather than the implementation "LinkedList".}}

  private LinkedList<Employee> foo2() { return null; }

  public java.util.HashSet<Employee> foo3() { return null; }      // Noncompliant {{The return type of this method should be an interface such as "Set" rather than the implementation "HashSet".}}

  public HashMap foo4() { return null; }                          // Noncompliant [[sc=10;ec=17]] {{The return type of this method should be an interface such as "Map" rather than the implementation "HashMap".}}

  private Stack stack;
  private Vector vector;
  public LinkedList<Employee> publicList; // Noncompliant {{The type of "publicList" should be an interface such as "List" rather than the implementation "LinkedList".}}
  private LinkedList<Employee> privateList;
  public ConcurrentHashMap concurrentHashMap() { // Noncompliant {{The return type of this method should be an interface such as "ConcurrentMap" rather than the implementation "ConcurrentHashMap".}}
    return null;
  }
  public ConcurrentSkipListMap concurrentSkipListMap() { // Noncompliant {{The return type of this method should be an interface such as "ConcurrentMap" rather than the implementation "ConcurrentSkipListMap".}}
    return null;
  }
  private void method1(LinkedList<Employee> employees) {}
  public void method2(LinkedList<Employee> employees) {} // Noncompliant {{The type of "employees" should be an interface such as "List" rather than the implementation "LinkedList".}}

  class A {
    public void foo(HashMap<String, String> map) { // Noncompliant
    }
  }

  class B extends A {
    @Override
    public void foo(HashMap<String, String> map) { // Compliant method is inherited
    }
  }
}

class Employee {

  public Employee() {
  }

  private Set<Employee> employees = new HashSet<Employee>();      // Compliant

  public Set<Employee> getEmployees(){                            // Compliant
    return employees;
  }
}
