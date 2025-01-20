package checks.tests;

import org.junit.platform.suite.api.IncludeEngines;

@IncludeEngines("cucumber")
class NoTestInTestClassCheckCucumberStandardWSTest {}

@IncludeEngines("cucumber")
class NoTestInTestClassCheckCucumberFullyQualifiedWSTest {}

@IncludeEngines("bellpepper")
class NoTestInTestClassCheckBellPepperWSTest {} // FN in automatic analysis

@IncludeEngines({"spring", "cucumber"})
class NoTestInTestClassCheckTwoEnginesWSTest{}

@NoTestInTestClassIncompatibleAnnotations.IncludeEngines(42)
class ClassWithFakeIncludeEnginesAnnotationWSTest {} // FN in automatic analysis
