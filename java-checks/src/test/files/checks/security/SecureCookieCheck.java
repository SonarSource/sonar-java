import javax.servlet.http.Cookie;

class A {

  Cookie cookie = new Cookie("name", "value");

  private static final boolean FALSE_CONSTANT = false;

  void foo(Cookie cookie) {
    int age = cookie.getMaxAge();
  }

  void bar() {
    Cookie cookie = new Cookie("name", "value");
    cookie.setSecure(true);
  }
  void baz() {
    Cookie cookie = new Cookie("name", "value"); // Noncompliant [[sc=12;ec=18]] {{Add the "secure" attribute to this cookie}}
  }
  void qix() {
    Cookie cookie = new Cookie("name", "value"); // Noncompliant {{Add the "secure" attribute to this cookie}}
    cookie.setSecure(false);
  }
  void baw(Cookie cookie) {
    cookie.setSecure(false); // FN
  }
  void qiz() {
    Cookie cookie = new Cookie("name", "value");
    cookie.setSecure(FALSE_CONSTANT); // FN
  }
  Cookie qfn() {
    return new Cookie("name", "value"); // FN
  }
}
