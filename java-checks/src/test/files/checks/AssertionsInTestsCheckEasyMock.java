import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.easymock.IMocksControl;
import org.junit.Test;

public class AssertionsInTestsCheckEasyMock extends EasyMockSupport {

  Controller controller;

  @Test
  public void contains_no_assertions() { // Noncompliant
    // do something
    replayAll();
  }

  @Test
  public void verify_controller() { // Compliant
    // do something
    controller.verify();
  }

  @Test
  public void super_verify_all_controls() { // Compliant
    // do something
    super.verifyAll();
  }

  @Test
  public void verify_all_controls() { // Compliant
    // do something
    verifyAll();
  }

  @Test
  public void static_verify() { // Compliant
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
