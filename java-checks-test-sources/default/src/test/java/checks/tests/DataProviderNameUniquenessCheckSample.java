package checks.tests;

import org.testng.annotations.DataProvider;

public class DataProviderNameUniquenessCheckSample {

  @DataProvider(name = "testData")
  public Object[][] provideData1() {
    return new Object[][]{{1, 2}};
  }

  @DataProvider(name = "testData")
  public Object[][] provideData2() { // Noncompliant {{Rename this data provider to make it unique within this class.}}
    return new Object[][]{{3, 4}};
  }

  @DataProvider(name = "testData")
  public Object[][] provideData3() { // Noncompliant
    return new Object[][]{{5, 6}};
  }

  @DataProvider(name = "uniqueData")
  public Object[][] provideUnique() {
    return new Object[][]{{7, 8}};
  }

  @DataProvider
  public Object[][] provideDefaultName() {
    return new Object[][]{{9, 10}};
  }

  @DataProvider(name = "alpha")
  public Object[][] provideAlpha() {
    return new Object[][]{{11, 12}};
  }

  @DataProvider(name = "beta")
  public Object[][] provideBeta() {
    return new Object[][]{{13, 14}};
  }

  @DataProvider(name = "provideDefaultName")
  public Object[][] implicitExplicitCollision() { // Noncompliant
    return new Object[][]{{15, 16}};
  }
}

class SecondClass {
  @DataProvider(name = "testData")
  public Object[][] provideData() {
    return new Object[][]{{15, 16}};
  }
}

class OuterClass {
  @DataProvider(name = "outerData")
  public Object[][] provideOuter() {
    return new Object[][]{{17, 18}};
  }

  class InnerClass {
    @DataProvider(name = "outerData")
    public Object[][] provideInner() {
      return new Object[][]{{19, 20}};
    }

    @DataProvider(name = "outerData")
    public Object[][] provideAnother() { // Noncompliant
      return new Object[][]{{21, 22}};
    }
  }
}

class NonStringNameClass {
  static final String CONSTANT_NAME = "constantData";

  @DataProvider(name = CONSTANT_NAME)
  public Object[][] provideWithConstant() {
    return new Object[][]{{1, 2}};
  }

  @DataProvider(parallel = true)
  public Object[][] provideParallelOnly() {
    return new Object[][]{{1, 2}};
  }
}

record DataProviderRecord(int value) {
  @DataProvider(name = "recordData")
  public Object[][] provideRecordData1() {
    return new Object[][]{{1, 2}};
  }

  @DataProvider(name = "recordData")
  public Object[][] provideRecordData2() { // Noncompliant
    return new Object[][]{{3, 4}};
  }
}
