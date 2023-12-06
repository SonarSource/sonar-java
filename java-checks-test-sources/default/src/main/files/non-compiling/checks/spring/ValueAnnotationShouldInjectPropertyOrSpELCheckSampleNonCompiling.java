package checks;

import org.springframework.beans.factory.annotation.Value;

class ValueAnnotationShouldInjectPropertyOrSpELCheckSampleNonCompiling {
  @org.springframework.beans.factory.annotation.Value(null) // compliant
  String wrongannotationValue;
}
