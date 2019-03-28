import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.Constraint;

class User {
  @NotNull
  private String name;
}

@Constraint
@interface MyConstraint {
}

@MyConstraint
class Department {
  private int hierarchyLevel;
}

class NonCompliantGroup {
  private Department department; // Noncompliant

  private User owner; // Noncompliant

  private List<User> members; // Noncompliant [[sc=11;ec=21]] {{Add missing "@Valid" on "members" to validate it with "Bean Validation".}}

  private Building.Size<User> office; // Noncompliant [[sc=11;ec=30]]
  private Building.Size company; // Compliant - Parametrized type, non-specified
}

class CompliantGroup {
  @Valid
  private Department department; // Compliant

  @Valid
  private User owner; // Compliant

  @Valid
  private List<User> members; // Compliant
}

class CompliantUserSelection {
  private List<@Valid User> contents; // Compliant; preferred syntax as of Java 8
}

class NonCompliantService {
  public void login(User user) { // Noncompliant {{Add missing "@Valid" on "user" to validate it with "Bean Validation".}}
  }

  public List<User> list(Department department) { // Noncompliant [[sc=26;ec=36]] {{Add missing "@Valid" on "department" to validate it with "Bean Validation".}}
  }
}

class CompliantService {
  public void login(@Valid User user) { // Compliant
  }

  public List<User> list(@Valid Department department) { // Compliant
  }
}

@interface KeepsDoctorAway {
}

@KeepsDoctorAway
class Apple {
}

class Orange {
}

class FoodInventory {
  private List<Apple> apples; // Compliant

  private List<Orange> oranges; // Compliant
}

class Human {
  public void eat(Apple apple) { // Compliant
  }

  public void eat(Orange orange) { // Compliant
  }
}

class Building {
  static class Size<T> { }
}

