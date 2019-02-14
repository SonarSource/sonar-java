import org.junit.Test;

public abstract class CustomTest extends UnknownAbstractSuperClassTest {

  @Test
  public void method_called_match1() {
    super.validateSomething();
  }

  @Test
  public void method_called_match2() {
    validateSomething();
  }

  @Test
  public void method_called_match3() {
    super.testSomething();
  }

  @Test
  public void method_called_match4() {
    testSomething();
  }

  @Test
  public void method_called_match5() {
    super.doTheTest();
  }

  @Test
  public void method_called_match6() {
    doTheTest();
  }

  @Test
  public void method_called_no_match1() { // Noncompliant
    super.doSomething();
  }

  @Test
  public void method_called_no_match2() { // Noncompliant
    doSomething();
  }

}
