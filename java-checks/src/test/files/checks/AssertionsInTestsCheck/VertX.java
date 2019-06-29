import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.TestSuite;
import org.junit.Test;

class A {
  Vertx vertx;
  int port;
  String requestURI;
  String urlString;
  String host;

  @Test
  public void test_with_nothing(TestContext contextVertx) { // Noncompliant
    Async async = contextVertx.async();
    async.complete();
  }

  @Test
  public void test_fail1(TestContext contextVertx) {
    contextVertx.fail();
  }

  @Test
  public void test_fail2(TestContext contextVertx) {
    contextVertx.fail("failure!");
  }

  @Test
  public void test_assertTrue1(TestContext contextVertx) {
    contextVertx.assertTrue(false);
  }

  @Test
  public void test_assertTrue2(TestContext contextVertx) {
    contextVertx.assertTrue(true, "message");
  }

  @Test
  public void test_assertFalse1(TestContext contextVertx) {
    contextVertx.assertFalse(false);
  }

  @Test
  public void test_assertFalse2(TestContext contextVertx) {
    contextVertx.assertFalse(false, "message");
  }

  @Test
  public void test_assertNull1(TestContext contextVertx) {
    contextVertx.assertNull(new Object());
  }

  @Test
  public void test_assertNull2(TestContext contextVertx) {
    contextVertx.assertNull(new Object(), "message");
  }

  @Test
  public void test_assertNotNull1(TestContext contextVertx) {
    contextVertx.assertNotNull(new Object());
  }

  @Test
  public void test_assertNotNull2(TestContext contextVertx) {
    contextVertx.assertNotNull(new Object(), "message");
  }

  @Test
  public void test_assertInRange1(TestContext contextVertx) {
    contextVertx.assertInRange(0, 0, 0);
  }

  @Test
  public void test_assertInRange2(TestContext contextVertx) {
    contextVertx.assertInRange(0, 0, 0, "message");
  }

  @Test
  public void test_assertEquals1(TestContext contextVertx) {
    contextVertx.assertEquals(0, 0);
  }

  @Test
  public void test_assertEquals2(TestContext contextVertx) {
    contextVertx.assertEquals(0, 0, "message");
  }

  @Test
  public void test_assertNotEquals1(TestContext contextVertx) {
    contextVertx.assertNotEquals(0, 0);
  }

  @Test
  public void test_assertNotEquals2(TestContext contextVertx) {
    contextVertx.assertNotEquals(0, 0, "message");
  }

  @Test
  public void test_asyncAssertSuccess1(TestContext contextVertx) {
    contextVertx.asyncAssertSuccess();
  }

  @Test
  public void test_asyncAssertSuccess2(TestContext contextVertx) { // Noncompliant
    contextVertx.asyncAssertSuccess(result -> {
    });
  }

  @Test
  public void test_asyncAssertFailure1(TestContext contextVertx) {
    contextVertx.asyncAssertFailure();
  }

  @Test
  public void test_asyncAssertFailure2(TestContext contextVertx) { // Noncompliant
    contextVertx.asyncAssertFailure(throwable -> {
    });
  }

  @Test
  public void test_saveUser() { // Compliant even if this test may not be run correctly - assertion is present
    TestSuite suite = TestSuite.create("suite_user_save");
    HttpClient client = vertx.createHttpClient();

    suite.test("user_save", context -> {

      client.getNow(port, host, requestURI, resp -> {
        resp.bodyHandler(body -> {

          context.assertNotEquals("created", body.toString()); // assertion
          client.close();
        });
      });

    });

  }

  @Test
  public void test_getAuthURL(TestContext contextVertx) { // Compliant
    HttpClient client = vertx.createHttpClient();
    Async async = contextVertx.async();

    client.getNow(port, "localhost", requestURI, resp -> {
      resp.bodyHandler(body -> {
        contextVertx.assertEquals(urlString, body.toString()); // assertion
        client.close();
        async.complete();
      });
    });

  }
}
