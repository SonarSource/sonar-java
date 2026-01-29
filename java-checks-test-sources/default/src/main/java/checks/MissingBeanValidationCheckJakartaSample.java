package checks;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Valid;
import java.util.List;

@Constraint(validatedBy = ConstraintChecker.class)
@interface WellNamed {
}

class ConstraintChecker implements ConstraintValidator<WellNamed, Occupant> {
  @Override
  public boolean isValid(Occupant occupant, ConstraintValidatorContext constraintValidatorContext) {
    return occupant.name != null;
  }
}

@WellNamed
class Occupant {
  public String name;
}

class Floor {
  private Id id; // Noncompliant {{Add missing "@Valid" on "id" to validate it with "Bean Validation".}}
//        ^^

  private List<Occupant> occupants; // Noncompliant {{Add missing "@Valid" on "occupants" to validate it with "Bean Validation".}}
//        ^^^^^^^^^^^^^^

  @Valid
  private List<Occupant> validatedOccupants; // Compliant

  // Preferred style as of Bean Validation 2.0
  private List<@Valid Occupant> validatedOccupants2; // Compliant

  public void ignore(Occupant occupant) { // Noncompliant {{Add missing "@Valid" on "occupant" to validate it with "Bean Validation".}}
//                   ^^^^^^^^
  }

  public void validate(@Valid Occupant occupant) { // Compliant
  }
}

class Id {
  @jakarta.validation.constraints.NotNull
  private String name;
}
