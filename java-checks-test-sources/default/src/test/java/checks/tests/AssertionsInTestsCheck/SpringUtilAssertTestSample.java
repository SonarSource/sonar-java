package checks.tests.AssertionsInTestsCheck;

import java.net.http.HttpResponse;
import org.junit.Test;
import org.springframework.util.Assert;

public class SpringUtilAssertTestSample {

  HttpResponse<String> response;

  @Test
  public void test_with_no_assertions() { // Noncompliant
    // no assertions
  }

  @org.junit.Test
  public void test_spring_util_assert() {
    org.springframework.util.Assert.isTrue(response.statusCode() == 200, "Expected 200 ");
  }

  @Test
  public void test_spring_util_assert2() {
    Assert.isNull(response, "Expected null but was not");
  }

  @Test
  public void test_spring_util_assert3() {
    Assert.hasLength(response.body(), "body should not be empty");
  }

}
