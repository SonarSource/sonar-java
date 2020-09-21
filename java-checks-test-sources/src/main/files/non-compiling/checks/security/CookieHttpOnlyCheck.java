package checks.security;

import java.net.HttpCookie;
import javax.servlet.http.Cookie;
import play.mvc.Http;

class CookieHttpOnlyCheck {

  private static final boolean FALSE_CONSTANT = false;
  play.mvc.Http.CookieBuilder xsfrTokenProp2;

  Cookie field4;
  HttpCookie field5;
  Cookie field6;
  UnknownCookie field7;

  void servletCookie(boolean param, Cookie c0) {

    field6.setHttpOnly(false); // Noncompliant
    field7.setHttpOnly(false);

    Cookie c7 = new UnknownCookie("name", "value"); // Noncompliant
    Object c8 = new Cookie("name", "value"); // Noncompliant

    Cookie c13;
    c13 = new UnknownCookie("name", "value"); // Noncompliant

    field4 = new Cookie("name, value"); // FN

    X x;
    x = new X("name", "value");
  }

  Cookie getC0() {
    return new UnknownCookie("name", "value"); // FN
  }

  public HttpCookie getCookie() {
    return null;
  }

  void httpCookie() {
    field5 = new HttpCookie("name, value"); // FN
  }

  // SONARJAVA-2772
  Cookie xsfrToken() {
    this.getXsfrTokenProp2().withHttpOnly(false);

    this.unknown = play.mvc.Http.Cookie.builder("XSRF-TOKEN", "2"); // Coverage
    unknown = play.mvc.Http.Cookie.builder("XSRF-TOKEN", "2"); // Coverage
    boolean secure = play.mvc.Http.Cookie.secure(); // Coverage

    return new Cookie("XSRF-TOKEN", "value");
  }

  play.mvc.Http.Cookie getXsfrTokenProp2() {
    return this.xsfrTokenProp2;
  }

  void compliant(javax.ws.rs.core.Cookie c) {
    c.isHttpOnly();
  }
}

class CookieHttpOnlyCheckCookie extends Cookie {
  public Cookie c;
  public void setHttpOnly(boolean isHttpOnly) { }
  void foo() {
    setHttpOnly(false); // Noncompliant
  }
  void bar(boolean x) {
    setHttpOnly(x);
  }
  void baz() {
    setHttpOnly(true);
  }
}

class B {
  void baw() {
    unknown = new CookieHttpOnlyCheckCookie("1", "2");
    Unknown.unkown(() -> { Class<String> v = unknown(); });
  }
}
