import javax.servlet.http.Cookie;

class A {

  Cookie cookie = new Cookie("name", "value");

  void foo(Cookie cookie) {
    int age = cookie.getMaxAge();
  }

  void bar() {
    Cookie cookie = new Cookie("name", "value");
    cookie.setSecure(true);
  }
  void baz() {
    Cookie cookie = new Cookie("name", "value"); //Non Compliant
  }
  void qix() {
    Cookie cookie = new Cookie("name", "value"); //Non Compliant
    cookie.setSecure(false);
  }

}