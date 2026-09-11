package checks.tests;

import org.testng.annotations.DataProvider;

public class DataProviderNameUniquenessCheckSample {

  @DataProvider(name = "testData")
  public Object[][] provideData1() {
    return new Object[][]{{1, 2}};
  }

  @DataProvider(name = "testData") // Noncompliant {{Rename this data provider to make it unique within this class.}}
  public Object[][] provideData2() {
    return new Object[][]{{3, 4}};
  }

  @DataProvider(name = "testData") // Noncompliant {{Rename this data provider to make it unique within this class.}}
  public Object[][] provideData3() {
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

    @DataProvider(name = "outerData") // Noncompliant {{Rename this data provider to make it unique within this class.}}
    public Object[][] provideAnother() {
      return new Object[][]{{21, 22}};
    }
  }
}
