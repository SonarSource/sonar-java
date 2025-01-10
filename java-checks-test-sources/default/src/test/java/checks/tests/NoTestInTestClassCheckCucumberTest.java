package checks.tests;

import org.junit.platform.suite.api.IncludeEngines;

@IncludeEngines("cucumber")
class NoTestInTestClassCheckCucumberStandardTest {}

@org.junit.platform.suite.api.IncludeEngines("cucumber")
class NoTestInTestClassCheckCucumberFullyQualifiedTest {}

@IncludeEngines("bellpepper")
class NoTestInTestClassCheckBellPepperTest {} // Noncompliant

@IncludeEngines({"spring", "cucumber"})
class NoTestInTestClassCheckTwoEnginesTest{}

@NoTestInTestClassIncompatibleAnnotations.IncludeEngines(42)
class ClassWithFakeIncludeEnginesAnnotationTest {} // Noncompliant

class NoTestInTestClassIncompatibleAnnotations {
  @interface IncludeEngines {
    int value();
  }
}
