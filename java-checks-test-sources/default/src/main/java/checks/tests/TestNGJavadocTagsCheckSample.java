package checks.tests;

class TestNGJavadocTagsCheckSample {

  /**
   * @test
   */
  public void shouldValidateInput() { // Noncompliant {{Replace this "@test" Javadoc tag with the TestNG "@Test" annotation.}}
  }

  /**
   * @beforeMethod
   */
  public void setUp() { // Noncompliant {{Replace this "@beforeMethod" Javadoc tag with the TestNG "@BeforeMethod" annotation.}}
  }

  /**
   * @afterMethod
   */
  public void tearDown() { // Noncompliant {{Replace this "@afterMethod" Javadoc tag with the TestNG "@AfterMethod" annotation.}}
  }

  /**
   * @beforeSuite
   */
  public void suiteSetUp() { // Noncompliant {{Replace this "@beforeSuite" Javadoc tag with the TestNG "@BeforeSuite" annotation.}}
  }

  /**
   * @afterSuite
   */
  public void suiteTearDown() { // Noncompliant {{Replace this "@afterSuite" Javadoc tag with the TestNG "@AfterSuite" annotation.}}
  }

  /**
   * @beforeTest
   */
  public void testSetUp() { // Noncompliant {{Replace this "@beforeTest" Javadoc tag with the TestNG "@BeforeTest" annotation.}}
  }

  /**
   * @afterTest
   */
  public void testTearDown() { // Noncompliant {{Replace this "@afterTest" Javadoc tag with the TestNG "@AfterTest" annotation.}}
  }

  /**
   * @beforeGroups
   */
  public void groupSetUp() { // Noncompliant {{Replace this "@beforeGroups" Javadoc tag with the TestNG "@BeforeGroups" annotation.}}
  }

  /**
   * @afterGroups
   */
  public void groupTearDown() { // Noncompliant {{Replace this "@afterGroups" Javadoc tag with the TestNG "@AfterGroups" annotation.}}
  }

  /**
   * @dataProvider
   */
  public Object[][] provideData() { // Noncompliant {{Replace this "@dataProvider" Javadoc tag with the TestNG "@DataProvider" annotation.}}
    return new Object[][] {};
  }

  /**
   * @factory
   */
  public Object[] createInstances() { // Noncompliant {{Replace this "@factory" Javadoc tag with the TestNG "@Factory" annotation.}}
    return new Object[] {};
  }

  /**
   * @Test
   */
  public void caseInsensitiveUpperCase() { // Noncompliant {{Replace this "@Test" Javadoc tag with the TestNG "@Test" annotation.}}
  }

  /**
   * @TEST
   */
  public void caseInsensitiveAllCaps() { // Noncompliant {{Replace this "@TEST" Javadoc tag with the TestNG "@Test" annotation.}}
  }

  /**
   * Validates user input.
   * @param input the input string
   * @test
   */
  public void mixedWithStandardTags(String input) { // Noncompliant {{Replace this "@test" Javadoc tag with the TestNG "@Test" annotation.}}
  }

  /**
   * Sets up resources.
   * @test
   * @beforeMethod
   */
  public void multipleTestNGTags() { // Noncompliant {{Replace this "@test" Javadoc tag with the TestNG "@Test" annotation.}} [[secondary=98]]
  }

  /** @test */
  public void singleLineJavadoc() { // Noncompliant {{Replace this "@test" Javadoc tag with the TestNG "@Test" annotation.}}
  }

  /**
   * @beforeClass
   */
  public void classSetUp() { // Noncompliant {{Replace this "@beforeClass" Javadoc tag with the TestNG "@BeforeClass" annotation.}}
  }

  /**
   * @afterClass
   */
  public void classTearDown() { // Noncompliant {{Replace this "@afterClass" Javadoc tag with the TestNG "@AfterClass" annotation.}}
  }

  /**
   * Example of TestNG usage:
   * <pre>
   * @Test
   * public void exampleTest() {}
   * </pre>
   */
  public void withPreBlock() { // compliant
  }

  /**
   * How to use annotations:
   * {@code @Test public void test() {}}
   */
  public void withCodeTag() { // compliant
  }

  // --- Compliant cases ---

  /**
   * @param input the input string
   * @return the result
   * @throws IllegalArgumentException if input is invalid
   */
  public String standardJavadocTags(String input) { // compliant
    return input;
  }

  /**
   * @see String
   * @since 1.0
   * @deprecated Use another method
   */
  public void otherStandardTags() { // compliant
  }

  public void noJavadoc() { // compliant
  }

  /* @test - not a Javadoc comment */
  public void blockComment() { // compliant
  }

  // @test - not a Javadoc comment
  public void lineComment() { // compliant
  }

  /**
   * Processes input data.
   */
  public void javadocWithoutTags() { // compliant
  }

  /**
   * @author John Doe
   * @version 1.0
   */
  public void authorAndVersionTags() { // compliant
  }

  /**
   * @listeners - Type-only annotation, not applicable to methods
   */
  public void typeOnlyTag() { // compliant
  }

  /**
   * @parameters - Type-only annotation, not applicable to methods
   */
  public void anotherTypeOnlyTag() { // compliant
  }
}
