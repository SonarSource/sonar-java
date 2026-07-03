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

  @NotNull
  private static String staticField; // Noncompliant {{Remove the "static" modifier from this field.}}
  //      ^^^^^^

  @Size(min = 1, max = 100)
  static String anotherStaticField; // Noncompliant

  @Min(1)
  private static int minStaticField; // Noncompliant

  @Pattern(regexp = ".*")
  private static String patternStaticField; // Noncompliant

  @NotNull // Compliant - instance field
  private String instanceField;

  @Size(min = 1, max = 100) // Compliant - instance field
  String anotherInstanceField;

  private static String unannotatedStaticField; // Compliant - no constraint annotation

  @SuppressWarnings("unused") // Compliant - not a Bean Validation constraint
  private static String suppressedStaticField;

  @JavaxCustomConstraint
  private static String customConstraintStaticField; // Noncompliant

  @JavaxCustomConstraint // Compliant - instance field
  private String customConstraintInstanceField;

  @Pattern.List({@Pattern(regexp = "[a-z]+"), @Pattern(regexp = ".{1,10}")}) // Compliant - container annotations are not detected (limitation)
  private static String patternListStaticField;
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
