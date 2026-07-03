package checks;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

class BeanValidationConstraintOnStaticFieldCheckSample {

  @NotNull
  private static String staticField; // Noncompliant {{Remove the "static" modifier from this field.}}
  //      ^^^^^^

  @Size(min = 1, max = 100)
  static String anotherStaticField; // Noncompliant

  @Min(1)
  private static int minStaticField; // Noncompliant

  @Pattern(regexp = ".*")
  private static String patternStaticField; // Noncompliant

  @NotBlank
  private static String notBlankStaticField; // Noncompliant

  @NotNull // Compliant - instance field
  private String instanceField;

  @Size(min = 1, max = 100) // Compliant - instance field
  String anotherInstanceField;

  private static String unannotatedStaticField; // Compliant - no constraint annotation

  @SuppressWarnings("unused") // Compliant - not a Bean Validation constraint
  private static String suppressedStaticField;

  @CustomConstraint
  private static String customConstraintStaticField; // Noncompliant

  @CustomConstraint // Compliant - instance field
  private String customConstraintInstanceField;

  @Pattern.List({@Pattern(regexp = "[a-z]+"), @Pattern(regexp = ".{1,10}")}) // Compliant - container annotations are not detected (limitation)
  private static String patternListStaticField;
}

@Constraint(validatedBy = CustomConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface CustomConstraint {
  String message() default "custom constraint violated";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}

class CustomConstraintValidator implements ConstraintValidator<CustomConstraint, String> {
  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value != null && !value.isEmpty();
  }
}
