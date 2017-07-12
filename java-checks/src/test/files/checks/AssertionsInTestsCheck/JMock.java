import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.Sequence;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Test;

class JMock {
  Mockery context = new Mockery();
  Mockery otherContext = new JUnit4Mockery();

  @Test
  public void test() { // Noncompliant
    // do nothing
  }

  @Test
  public void assertIsSatisfied() { // Compliant
    SomeUtil someUtil = new SomeUtil();
    ServiceProvider serviceProvider = new ServiceProvider();
    context.checking(new Expectations() {{
      Sequence sequence = context.sequence("pipeline");
      oneOf(someUtil).doSomething(with(any(String.class)));
      inSequence(sequence);
      oneOf(someUtil).doSomething(with(any(String.class)));
      inSequence(sequence);
      will(returnValue("a successful reply"));
      inSequence(sequence);
    }});

    serviceProvider.processRequest("any ole string");
    context.assertIsSatisfied();
  }

  @Test
  public void juni4AssertIsSatisfied() { // Compliant
    SomeUtil someUtil = new SomeUtil();
    ServiceProvider serviceProvider = new ServiceProvider();
    otherContext.checking(new Expectations() {{
        Sequence sequence = context.sequence("pipeline");
        oneOf(someUtil).doSomething(with(any(String.class)));
        inSequence(sequence);
        oneOf(someUtil).doSomething(with(any(String.class)));
        inSequence(sequence);
        will(returnValue("a successful reply"));
        inSequence(sequence);
      }});

    serviceProvider.processRequest("any ole string");
    otherContext.assertIsSatisfied();
  }

  static class SomeUtil {
    public void doSomething(String with) { }
  }

  static class ServiceProvider {
    public void processRequest(String request) { }
  }
}
