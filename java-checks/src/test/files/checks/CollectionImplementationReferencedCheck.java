public class EmployeesTopLevel {
  public HashSet<Employee> employees = new HashSet<Employee>();  // Noncompliant {{The type of the "employees" object should be an interface such as "Set" rather than the implementation "HashSet".}} [[sc=10;ec=27]]
  private HashSet<Employee> employees2 = new HashSet<Employee>();

  public HashSet<Employee> getEmployees() {                       // Noncompliant {{The return type of this method should be an interface such as "Set" rather than the implementation "HashSet".}}
    return employees;
  }

  public LinkedList<Employee> foo() {                             // Noncompliant {{The return type of this method should be an interface such as "List" rather than the implementation "LinkedList".}} [[sc=10;ec=30]]
  }
  private LinkedList<Employee> foo() { }

  public java.util.HashSet<Employee> foo() {                      // Compliant - limitation
  }

  public HashMap foo() {                                          // Noncompliant {{The return type of this method should be an interface such as "Map" rather than the implementation "HashMap".}} [[sc=10;ec=17]]
  }

  public Employees() {
  }

  private Stack stack;
  private Vector vector;
  public LinkedList<Employee> publicList; // Noncompliant {{The type of the "publicList" object should be an interface such as "List" rather than the implementation "LinkedList".}}
  private LinkedList<Employee> privateList;
  public ConcurrentHashMap concurrentHashMap() { // Noncompliant {{The return type of this method should be an interface such as "ConcurrentMap" rather than the implementation "ConcurrentHashMap".}}
    return null;
  }
  public ConcurrentSkipListMap concurrentSkipListMap() { // Noncompliant {{The return type of this method should be an interface such as "ConcurrentMap" rather than the implementation "ConcurrentSkipListMap".}}
    return null;
  }
  private method(LinkedList<Employee> employees) {}
  public method(LinkedList<Employee> employees) {} // Noncompliant {{The type of the "employees" object should be an interface such as "List" rather than the implementation "LinkedList".}}
}

public class Employees {
  private Set<Employee> employees = new HashSet<Employee>();      // Compliant

  public Set<Employee> getEmployees(){                            // Compliant
    return employees;
  }
}

class A {
  public void foo(HashMap<String, String> map) { // Noncompliant
  }
}

class B extends A {
  @Override
  public void foo(HashMap<String, String> map) { // Compliant method is inherited
  }
}
