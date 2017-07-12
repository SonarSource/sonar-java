import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

import org.junit.Test;

class WireMockTest {
  @Test
  public void myTest() { // Noncompliant
    // do nothing
  }

  @Test
  public void verify() { // Compliant
    RequestPatternBuilder requestPatternBuilder = new RequestPatternBuilder()
      .allRequests()
      .withUrl("/hello/world/*");
    WireMock.verify(requestPatternBuilder);
  }
}
