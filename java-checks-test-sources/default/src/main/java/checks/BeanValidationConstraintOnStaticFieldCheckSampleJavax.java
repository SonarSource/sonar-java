package checks;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

class BeanValidationConstraintOnStaticFieldCheckSampleJavax {

  @NotNull // Noncompliant {{Remove this Bean Validation constraint from this static field, as it will be ignored by the Bean Validation framework.}}
  private static String staticField;

  @Size(min = 1, max = 100) // Noncompliant
  static String anotherStaticField;

  @Min(1) // Noncompliant
  private static int minStaticField;

  @Pattern(regexp = ".*") // Noncompliant
  private static String patternStaticField;

  @NotNull // Compliant - instance field
  private String instanceField;

  @Size(min = 1, max = 100) // Compliant - instance field
  String anotherInstanceField;

  private static String unannotatedStaticField; // Compliant - no constraint annotation

  @SuppressWarnings("unused") // Compliant - not a Bean Validation constraint
  private static String suppressedStaticField;

  @JavaxCustomConstraint // Noncompliant
  private static String customConstraintStaticField;

  @JavaxCustomConstraint // Compliant - instance field
  private String customConstraintInstanceField;
}

@Constraint(validatedBy = JavaxCustomConstraintValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@interface JavaxCustomConstraint {
  String message() default "custom constraint violated";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}

class JavaxCustomConstraintValidator implements ConstraintValidator<JavaxCustomConstraint, String> {
  @Override
  public void initialize(JavaxCustomConstraint constraintAnnotation) {
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return value != null && !value.isEmpty();
  }
}
