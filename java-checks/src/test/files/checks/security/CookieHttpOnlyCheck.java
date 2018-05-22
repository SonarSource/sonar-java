import javax.servlet.http.Cookie;

class S3330 {

  Cookie cookie = new Cookie("name", "value");

  void foo(Cookie cookie) {
    int age = cookie.getHttpOnly();
  }

  void bar() {
    Cookie cookie = new Cookie("name", "value");
    cookie.setHttpOnly(true);
  }

  void baz() {
    Cookie cookie = new Cookie("name", "value"); // Noncompliant {{Add the "HttpOnly" cookie attribute.}}
  }

  void qix() {
    Cookie cookie = new Cookie("name", "value"); // Noncompliant {{Add the "HttpOnly" cookie attribute.}}
    cookie.setHttpOnly(false);
  }
}
