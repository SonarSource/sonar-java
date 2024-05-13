import java.util.List;
import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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

public class CheckMyConstraint implements ConstraintValidator<MyConstraint, Department>
{
  public void initialize(MyConstraint constraintAnnotation) {
  }

  public boolean isValid(Department bean, ConstraintValidatorContext context) { // Compliant - @Valid inside constraint validators would be nonsense
  }
}

class NonCompliantGroup {
  private Department department; // Noncompliant

  private User owner; // Noncompliant

  private List<User> members; // Noncompliant {{Add missing "@Valid" on "members" to validate it with "Bean Validation".}}
//        ^^^^^^^^^^

  private Building.Size<User> office; // Noncompliant
//        ^^^^^^^^^^^^^^^^^^^
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

  public List<User> list(Department department) { // Noncompliant {{Add missing "@Valid" on "department" to validate it with "Bean Validation".}}
//                       ^^^^^^^^^^
  }
}

class CompliantService {
  public void login(@Valid User user) { // Compliant
    // ...
    updateLastLoginDate(user);
  }

  private void updateLastLoginDate(User user) { // Compliant - Bean Validation does not intercept private methods
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

