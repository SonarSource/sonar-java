import javax.servlet.http.Cookie;

class A {

  Cookie cookie = new Cookie("name", "value");

  private static final boolean TRUE_CONSTANT = true;

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
  void qiz() {
    Cookie cookie = new Cookie("name", "value"); // Noncompliant
    cookie.setSecure(TRUE_CONSTANT); // would require SE to check value
  }
  Cookie qfn() {
    return new Cookie("name", "value"); // FN
  }
}
