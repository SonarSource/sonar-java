package checks.tests.AssertionsInTestsCheck;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

import org.junit.Test;

class WireMockTest {

  WireMockServer wireMockServer;

  @Test
  public void myTest() { // Noncompliant
    // do nothing
  }

  @Test
  public void verifyClient() { // Compliant
    RequestPatternBuilder requestPatternBuilder = new RequestPatternBuilder()
      .allRequests()
      .withUrl("/hello/world/*");
    WireMock.verify(requestPatternBuilder);
  }

  @Test
  public void verifyServer() { // Compliant
    RequestPatternBuilder requestPatternBuilder = new RequestPatternBuilder()
      .allRequests()
      .withUrl("/hello/world/*");
    wireMockServer.verify(requestPatternBuilder);
  }
}
