package checks.VisibleForTestingProtectedUsageCheck;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

@Retention(value = CLASS)
@Target(value = {TYPE, METHOD, FIELD})
public @interface VisibleForTesting {
  int otherwise();
  int othertestcase();
  String othertypecase();
  int PROTECTED = 4;
}
