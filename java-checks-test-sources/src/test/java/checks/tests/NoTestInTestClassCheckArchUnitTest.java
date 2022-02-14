package checks.tests;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

// missing @RunWith or @AnalyzeClasses
public class NoTestInTestClassCheckArchUnitTest { // Noncompliant [[sc=14;ec=48]] {{Add some tests to this class.}}

  @ArchTest
  public static final ArchRule noJodatime = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;

}

// junit4
@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "checks")
class NoTestInTestClassCheckArchUnit1Test {

  @ArchTest
  public static final ArchRule noGenericExceptions = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

}

@RunWith(ArchUnitRunner.class)
@AnalyzeClasses(packages = "checks")
class NoTestInTestClassCheckArchUnit2Test { // Noncompliant [[sc=7;ec=42]] {{Add some tests to this class.}}

  // missing @ArchTest
  public static final ArchRule noJavaUtilLogging = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

}

// junit5
@AnalyzeClasses(packages = "checks")
class NoTestInTestClassCheckArchUnit3Test {

  @ArchTest
  public static final ArchRule noJodatime = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;

}

@AnalyzeClasses(packages = "checks")
class NoTestInTestClassCheckArchUnit4Test {

  @ArchTest
  private void no_access_to_standard_streams_as_method(JavaClasses classes) {
    noClasses().should(GeneralCodingRules.ACCESS_STANDARD_STREAMS).check(classes);
  }

}

@AnalyzeClasses(packages = "checks")
class NoTestInTestClassCheckArchUnit5Test { // Noncompliant [[sc=7;ec=42] {{Add some tests to this class.}}

  // missing @ArchTest
  public static final ArchRule noCycles = slices().matching("checks.(**)").should().beFreeOfCycles();

}

@AnalyzeClasses(packages = "checks")
class NoTestInTestClassCheckArchUnit6Test { // Noncompliant [[sc=7;ec=42]] {{Add some tests to this class.}}

  // missing @ArchTest
  private void no_access_to_standard_streams_as_method(JavaClasses classes) {
    noClasses().should(GeneralCodingRules.ACCESS_STANDARD_STREAMS).check(classes);
  }

}

@AnalyzeClasses(packages = "checks")
class NoTestInTestClassCheckArchUnit7Test extends NoTestInTestClassCheckArchUnit4Test {

  // inherit tests from base class
}

// interfaces are ignored
interface NoTestInTestClassCheckInterfaceTest {

  @ArchTest
   ArchRule noJodatime = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JODATIME;

}

@AnalyzeClasses(packages = "checks")
class NoTestInTestClassCheckArchUnit9Test implements NoTestInTestClassCheckInterfaceTest { // Noncompliant [[sc=7;ec=42]] {{Add some tests to this class.}}

  // does not inherit tests from interface fields

}
