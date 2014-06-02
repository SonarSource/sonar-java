public class EmployeesTopLevel {
  private HashSet<Employee> employees = new HashSet<Employee>();  // Noncompliant - "persons" should have type "Set" rather than "HashSet"

  public HashSet<Employee> getEmployees() {                       // Noncompliant
    return employees;
  }

  public LinkedList<Employee> foo() {                             // Noncompliant
  }

  public java.util.HashSet<Employee> foo() {                      // Compliant - limitation
  }

  public HashMap foo() {                                          // Noncompliant
  }

  public Employees() {
  }

  private Stack stack;
  private Vector vector;
}

public class Employees {
  private Set<Employee> employees = new HashSet<Employee>();      // Compliant

  public Set<Employee> getEmployees(){                            // Compliant
    return employees;
  }
}
