package checks.tests.AssertionsInTestsCheck;

import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultHandler;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.ws.test.server.MockWebServiceClient;

abstract class SpringTest {

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

  @Test
  public void andExpectAllTest() throws Exception { // Compliant
    getResultActions().andDo(getResultHandler()).andExpectAll(getResultMatcher(), getResultMatcher()).andReturn();
  }

  @Test
  public void mockWebServiceClient(){ // Compliant
    MockWebServiceClient server = null;
    server.sendRequest(null)
      .andExpect(null)
      .andExpect(null);
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
