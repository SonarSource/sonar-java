import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;

public class AssertionsInTestsCheckTest {

  @Test
  public void testWithNoAssertion1() { // Noncompliant
  }

  @Test
  public void testWithNoAssertion2() throws Exception { // Noncompliant
    getResultActions().andDo(getResultHandler());
  }

  @Test
  public void testWithNoAssertion3() throws Exception { // Noncompliant
    getResultActions().andReturn();
  }

  @Test
  public void fakeTest1() throws Exception { // Compliant
    getResultActions().andExpect(getResultMatcher());
  }
  
  @Test
  public void fakeTest2() throws Exception { // Compliant
    getResultActions().andDo(getResultHandler()).andExpect(getResultMatcher()).andReturn();
  }
  
  private ResultActions getResultActions() {
    return null;
  }

  private ResultMatcher getResultMatcher() {
    return null;
  }

  private ResultHandler getResultHandler() {
    return null;
  }

}
