package checks.tests;

import static io.cucumber.junit.platform.engine.Constants.*;

import org.junit.platform.suite.api.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("com.project.class.path")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value="com.project.class.path")
class NoTestInTestClassCheckCucumberStandardTest {}

@Suite
@org.junit.platform.suite.api.IncludeEngines("cucumber")
@SelectClasspathResource("com.project.class.path")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value="com.project.class.path")
class NoTestInTestClassCheckCucumberFullyQualifiedTest {}

@Suite
@IncludeEngines("bellpepper")
@SelectClasspathResource("com.project.class.path")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value="com.project.class.path")
class NoTestInTestClassCheckBellPepperTest {} // Noncompliant
