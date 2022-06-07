package checks.security;

import java.net.HttpCookie;
import java.util.Date;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.NewCookie;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.security.web.savedrequest.SavedCookie;

class CookieHttpOnlyCheck {

  private static final boolean FALSE_CONSTANT = false;
  private static final String XSRF_TOKEN = "XSRF-TOKEN";
  play.mvc.Http.CookieBuilder xsfrTokenProp;

  Cookie field1 = new Cookie("name", "value"); // FN
  HttpCookie field2 = new HttpCookie("name", "value"); // FN
  javax.ws.rs.core.Cookie field3 = new javax.ws.rs.core.Cookie("name", "value"); // FN
  Cookie field4;
  HttpCookie field5;
  Cookie field6;

  void servletCookie(boolean param, Cookie c0) {

    c0.setHttpOnly(false); // Noncompliant [[sc=19;ec=26]] {{Make sure creating this cookie without the "HttpOnly" flag is safe.}}
    field6.setHttpOnly(false); // Noncompliant

    Cookie c1 = new Cookie("name", "value");
    if (param) {
      c1.setHttpOnly(false); // Noncompliant
    }
    else {
      c1.setHttpOnly(true);
    }

    Cookie c2 = new Cookie("name", "value"); // Noncompliant [[sc=21;ec=27]]

    Cookie c3 = new Cookie("name", "value");
    c3.setHttpOnly(false); // Noncompliant

    Cookie c4 = new Cookie("name", "value");
    c4.setHttpOnly(FALSE_CONSTANT); // Noncompliant

    Cookie c5 = new Cookie("name", "value");
    boolean b = false;
    c5.setHttpOnly(b); // Noncompliant

    Cookie c6 = new Cookie("name", "value");
    c6.setHttpOnly(param);

    Object c8 = new Cookie("name", "value"); // Noncompliant

    Cookie c9;
    c9 = new Cookie("name", "value");
    c9.setHttpOnly(false); // Noncompliant

    Cookie c10;
    c10 = new Cookie("name", "value");  // Noncompliant

    Cookie c11;
    c11 = new Cookie("name", "value");
    c11.setHttpOnly(true);

    Object c12;
    c12 = new Cookie("name", "value"); // Noncompliant

    Cookie c14 = new Cookie("name", "value");
    boolean bValue = true;
    c14.setHttpOnly(!bValue); // FN
  }

  Cookie getC1() {
    return new Cookie("name", "value"); // Noncompliant [[sc=16;ec=22]]
  }

  Cookie returnHttpCookie(HttpServletResponse response) {
    Cookie cookie = new Cookie("name", "value"); // Noncompliant
    response.addCookie(new Cookie("name", "value")); // Noncompliant
    return new Cookie("name", "value"); // Noncompliant
  }

  public HttpCookie getCookie() {
    return null;
  }

  void httpCookie() {
    HttpCookie cookie = getCookie();
    if (cookie == null) {
      cookie = new HttpCookie("name", "value"); // Noncompliant
    }

    HttpCookie c1 = new HttpCookie("name", "value");
    c1.setHttpOnly(true);

    HttpCookie c2 = new HttpCookie("name", "value"); // Noncompliant

    HttpCookie c3 = new HttpCookie("name", "value");
    c3.setHttpOnly(false); // Noncompliant

    HttpCookie c4 = new HttpCookie("name", "value");
    c4.setHttpOnly(FALSE_CONSTANT); // Noncompliant

    HttpCookie c5;
    c5 = new HttpCookie("name", "value");
    c5.setHttpOnly(false); // Noncompliant
  }

  HttpCookie getC2() {
    return new HttpCookie("name", "value"); // Noncompliant
  }

  void jaxRsCookie() {
    javax.ws.rs.core.Cookie c1 = new javax.ws.rs.core.Cookie("name", "value"); // Noncompliant
    javax.ws.rs.core.Cookie c2 = new javax.ws.rs.core.Cookie("name", "value", "path", "domain"); // Noncompliant
  }

  void jaxRsNewCookie(javax.ws.rs.core.Cookie cookie) {
    NewCookie c1 = new NewCookie("name", "value", "path", "domain", "comment", 1, true); // Noncompliant
    NewCookie c2 = new NewCookie(cookie, "comment", 2, true); // Noncompliant
    NewCookie c3 = new NewCookie(cookie); // Noncompliant
    NewCookie c4 = new NewCookie(cookie, "c", 1, true); // Noncompliant

    NewCookie c5 = new NewCookie(cookie, "c", 1, new Date(), false, true); // last param is HttpOnly
    NewCookie c6 = new NewCookie("1", "2", "3", "4", 5, "6", 7, new Date(), false, true);
    NewCookie c7 = new NewCookie("1", "2", "3", "4", "5", 6, false, true);
  }

  NewCookie getC3() {
    return new NewCookie("name", "value", "path", "domain", "comment", 1, true); // Noncompliant
  }

  void apacheShiro(SimpleCookie unknownCookie) {
    SimpleCookie c1 = new SimpleCookie(unknownCookie); // Noncompliant
    SimpleCookie c2 = new SimpleCookie();
    c2.setHttpOnly(false); // Noncompliant
    SimpleCookie c3 = new SimpleCookie(); // Apache Shiro cookies have HttpOnly 'true' value by default
    SimpleCookie c4 = new SimpleCookie("name");
  }

  SimpleCookie getC4() {
    return new SimpleCookie(); // compliant
  }

  void playFw(play.mvc.Http.Cookie.SameSite sameSite) {
    play.mvc.Http.Cookie c21 = new play.mvc.Http.Cookie("1", "2", 3, "4", "5", true, false, sameSite); // Noncompliant
    play.mvc.Http.Cookie c22 = new play.mvc.Http.Cookie("1", "2", 3, "4", "5", true, true, sameSite);
    play.mvc.Http.CookieBuilder cb1 = play.mvc.Http.Cookie.builder("1", "2");
    cb1.withHttpOnly(false); // Noncompliant
    cb1.withHttpOnly(true); // is ignored, so above is a FN
    play.mvc.Http.CookieBuilder cb2 = play.mvc.Http.Cookie.builder("1", "2");
    cb2.withHttpOnly(true);
    play.mvc.Http.Cookie.builder("1", "2")
        .withName("yolo")
        .withPath("x")
        .withDomain("x")
        .withSecure(true)
        .withHttpOnly(false) // Noncompliant
        .build();
    play.mvc.Http.Cookie.builder("theme", "blue").withHttpOnly(true);
  }

  play.mvc.Http.Cookie getC5() {
    return new play.mvc.Http.Cookie("1", "2", 3, "4", "5", true, false, play.mvc.Http.Cookie.SameSite.NONE); // Noncompliant
  }

  play.mvc.Http.CookieBuilder getC6() {
    return play.mvc.Http.Cookie.builder("theme", "blue").withHttpOnly(false); // Noncompliant
  }

  // SONARJAVA-2772
  Cookie xsfrToken() {
    String cookieName = "XSRF-TOKEN";

    Cookie xsfrToken = new Cookie("XSRF-TOKEN", "value"); // OK, used to implement XSRF
    xsfrToken.setHttpOnly(false);

    Cookie xsfrToken2 = new Cookie("XSRF-TOKEN", "value");
    xsfrToken2.setHttpOnly(true);

    Cookie xsfrToken3 = new Cookie("XSRF-TOKEN", "value");

    Cookie xsfrToken4 = new Cookie(XSRF_TOKEN, "value");

    HttpCookie xsfrToken5 = new HttpCookie("XSRF-TOKEN", "value");

    javax.ws.rs.core.Cookie xsfrToken6 = new javax.ws.rs.core.Cookie("XSRF-TOKEN", "value");

    NewCookie xsfrToken7 = new NewCookie("XSRF-TOKEN", "value", "path", "domain", "comment", 1, true);

    SimpleCookie xsfrToken8 = new SimpleCookie("XSRF-TOKEN");

    play.mvc.Http.Cookie xsfrToken9 = new play.mvc.Http.Cookie("XSRF-TOKEN", "2", 3, "4", "5", true, false, play.mvc.Http.Cookie.SameSite.NONE);

    play.mvc.Http.Cookie.builder("XSRF-TOKEN", "2")
      .withName("yolo")
      .withPath("x")
      .withDomain("x")
      .withSecure(true)
      .withHttpOnly(false)
      .build();

    play.mvc.Http.CookieBuilder xsfrToken10;
    xsfrToken10 = play.mvc.Http.Cookie.builder("XSRF-TOKEN", "2");
    xsfrToken10.withHttpOnly(false);

    play.mvc.Http.CookieBuilder xsfrToken11 = play.mvc.Http.Cookie.builder("XSRF-TOKEN", "2");
    xsfrToken11.withHttpOnly(false);

    Cookie xsfrToken12 = new Cookie("CSRFToken", "value");
    xsfrToken12.setHttpOnly(false);

    Cookie xsfrToken13 = new Cookie("Csrf-token", "value");
    xsfrToken13.setHttpOnly(false);

    this.xsfrTokenProp = play.mvc.Http.Cookie.builder("XSRF-TOKEN", "2");
    this.xsfrTokenProp.withHttpOnly(false);

    return new Cookie("XSRF-TOKEN", "value");
  }

  void compliant(Cookie c1, HttpCookie c2, NewCookie c4, SimpleCookie c5) {
    c1.isHttpOnly();
    c2.isHttpOnly();
    c4.isHttpOnly();
    c5.isHttpOnly();
    SavedCookie c6 = new SavedCookie(c1); // Spring cookies are HttpOnly, without possibility to change that
    SavedCookie c7 = new SavedCookie("n", "v", "c", "d", 1, "p", false, 1);
  }

  SavedCookie getC7() {
    return new SavedCookie("n", "v", "c", "d", 1, "p", false, 1); // compliant
  }
}

class CookieHttpOnlyCheckCookieA extends Cookie {
  public Cookie c;

  public CookieHttpOnlyCheckCookieA() {
    super("name", "value");
  }

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

class CookieHttpOnlyCheckCookieB {
  CookieHttpOnlyCheckCookieA a;
  public void setHttpOnly(boolean isHttpOnly) { }
  void foo() {
    setHttpOnly(false);
  }
  void bar() { return; }
  CookieHttpOnlyCheckCookieA getA() {
    return new CookieHttpOnlyCheckCookieA(); // Noncompliant
  }
  void baw() {
    int i;
    i = 1;
    a.c = new Cookie("1", "2"); // FN
  }
}
