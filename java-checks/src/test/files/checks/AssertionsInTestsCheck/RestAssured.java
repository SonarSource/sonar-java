import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.ResponseSpecification;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

public class AssertionsInTestsCheckTest {
  
  @Test
  public void incomplete() { // Noncompliant
    // do nothing
  }

  @Test
  public void test_body() { // Compliant
    RestAssured
      .given()
      .get("http://url.com")
      .then()
      .body("[0].userId", equalTo(5));
  }

  @Test
  public void test_content() { // Compliant
    RestAssured
      .given()
      .get("http://url.com")
      .then()
      .content(is("body"));
  }

  @Test
  public void test_contentType() { // Compliant
    RestAssured
      .given()
      .get("http://url.com")
      .then()
      .contentType("application/json");
  }

  @Test
  public void test_cookie() { // Compliant
    RestAssured
      .given()
      .get("http://url.com")
      .then()
      .cookie("Cookie1");
  }

  @Test
  public void test_cookies() { // Compliant
    RestAssured
      .given()
      .get("http://url.com")
      .then()
      .cookies("Cookie1", "Cookie");
  }

  @Test
  public void test_header() { // Compliant
    RestAssured
      .given()
      .get("http://url.com")
      .then()
      .header("Header1", "Test");
  }

  @Test
  public void test_headers() { // Compliant
    RestAssured
      .given()
      .get("http://url.com")
      .then()
      .headers("a", "a");
  }

  @Test
  public void test_spec() { // Compliant
    ResponseSpecification spec = new ResponseSpecBuilder().expectStatusCode(200).expectBody("content", equalTo("Hello, Johan!")).build();

    RestAssured
      .given()
      .get("http://url.com")
      .then()
      .spec(spec);
  }

  @Test
  public void test_specification() { // Compliant

    ResponseSpecification spec = new ResponseSpecBuilder().expectStatusCode(200).expectBody("content", equalTo("Hello, Johan!")).build();

    RestAssured
      .given()
      .get("http://url.com")
      .then()
      .specification(spec);
  }

  @Test
  public void test_statuscode() { // Compliant
    ValidatableResponse validatableResponse = RestAssured
      .given()
      .get("http://url.com")
      .then()
      .statusCode(200);
  }

  @Test
  public void test_statusline() { // Compliant
    RestAssured
      .given()
      .get("http://url.com")
      .then()
      .statusLine("statusline");
  }

  @Test
  public void test_time() { // Compliant
    RestAssured
      .given()
      .get("http://url.com")
      .then()
      .time(lessThan(2L), SECONDS);
  }
}
