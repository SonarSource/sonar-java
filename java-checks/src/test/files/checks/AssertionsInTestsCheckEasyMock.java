import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IMocksControl;
import org.foo.ATest.Controller;
import org.junit.Test;

public class AssertionsInTestsCheckTest extends EasyMockSupport {

  Controller controller;

  @Test
  public void test0() { // Noncompliant
    // do something
    replayAll();
  }

  @Test
  public void test1() { // Compliant
    // do something
    controller.verify();
  }

  @Test
  public void test2() { // Compliant
    // do something
    super.verifyAll();
  }

  @Test
  public void test3() { // Compliant
    // do something
    verifyAll();
  }

  @Test
  public void test4() { // Compliant
    Object o1 = EasyMock.createMock(Object.class);
    String s1 = EasyMock.createMock(String.class);

    // do something

    EasyMock.verify(o1, s1);
  }

  @Override
  public void verifyAll() {
    // do something
    super.verifyAll();
  }

  abstract static class Controller implements IMocksControl {
  }
}
