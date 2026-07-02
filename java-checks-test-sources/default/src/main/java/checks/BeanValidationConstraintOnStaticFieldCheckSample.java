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

  @NotNull // Noncompliant {{Remove this Bean Validation constraint from this static field, as it will be ignored by the Bean Validation framework.}}
//^^^^^^^^
  private static String staticField;

  @Size(min = 1, max = 100) // Noncompliant
  static String anotherStaticField;

  @Min(1) // Noncompliant
  private static int minStaticField;

  @Pattern(regexp = ".*") // Noncompliant
  private static String patternStaticField;

  @NotBlank // Noncompliant
  private static String notBlankStaticField;

  @NotNull // Compliant - instance field
  private String instanceField;

  @Size(min = 1, max = 100) // Compliant - instance field
  String anotherInstanceField;

  private static String unannotatedStaticField; // Compliant - no constraint annotation

  @SuppressWarnings("unused") // Compliant - not a Bean Validation constraint
  private static String suppressedStaticField;

  @CustomConstraint // Noncompliant
  private static String customConstraintStaticField;

  @CustomConstraint // Compliant - instance field
  private String customConstraintInstanceField;

  @Pattern.List({@Pattern(regexp = "[a-z]+"), @Pattern(regexp = ".{1,10}")}) // Noncompliant
  private static String patternListStaticField;

  @Pattern.List({@Pattern(regexp = "[a-z]+"), @Pattern(regexp = ".{1,10}")}) // Compliant - instance field
  private String patternListInstanceField;
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
