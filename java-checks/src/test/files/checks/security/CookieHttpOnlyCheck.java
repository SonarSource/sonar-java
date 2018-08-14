import java.util.Date;
import java.net.HttpCookie;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.NewCookie;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.security.web.savedrequest.SavedCookie;

class S3330 {

  private static final boolean FALSE_CONSTANT = false;
  private static final String XSRF_TOKEN = "XSRF-TOKEN";
  play.mvc.Http.CookieBuilder xsfrTokenProp;
  play.mvc.Http.CookieBuilder xsfrTokenProp2;

  Cookie field1 = new Cookie("name", "value"); // FN
  HttpCookie field2 = new HttpCookie("name", "value"); // FN
  javax.ws.rs.core.Cookie field3 = new javax.ws.rs.core.Cookie("name", "value"); // FN
  Cookie field4;
  HttpCookie field5;
  Cookie field6;
  UnknownCookie field7;

  void servletCookie(boolean param, Cookie c0) {

    c0.setHttpOnly(false); // Noncompliant [[sc=19;ec=26]] {{Add the "HttpOnly" cookie attribute.}}
    field6.setHttpOnly(false); // Noncompliant
    field7.setHttpOnly(false);

    Cookie c1 = new Cookie("name", "value");
    if (param) {
      c1.setHttpOnly(false); // FN
    }
    else {
      c1.setHttpOnly(true);
    }

    Cookie c2 = new Cookie("name", "value"); // Noncompliant [[sc=12;ec=14]]

    Cookie c3 = new Cookie("name", "value"); // Noncompliant
    c3.setHttpOnly(false);

    Cookie c4 = new Cookie("name", "value"); // Noncompliant
    c4.setHttpOnly(FALSE_CONSTANT);

    Cookie c5 = new Cookie("name", "value"); // Noncompliant
    boolean b = false;
    c5.setHttpOnly(b);

    Cookie c6 = new Cookie("name", "value");
    c6.setHttpOnly(param);

    Cookie c7 = new UnknownCookie("name", "value"); // Noncompliant
    Object c8 = new Cookie("name", "value"); // Noncompliant

    Cookie c9; // Noncompliant
    c9 = new Cookie("name", "value");
    c9.setHttpOnly(false);

    Cookie c10;  // Noncompliant
    c10 = new Cookie("name", "value");

    Cookie c11;
    c11 = new Cookie("name", "value");
    c11.setHttpOnly(true);

    Object c12; // Noncompliant
    c12 = new Cookie("name", "value");

    Cookie c13; // Noncompliant
    c13 = new UnknownCookie("name", "value");

    Cookie c14 = new Cookie("name", "value");
    boolean bValue = true;
    c14.setHttpOnly(!bValue);

    field4 = new Cookie("name, value"); // FN

    X x;
    x = new X("name", "value");
  }

  Cookie getC0() {
    return new UnknownCookie("name", "value"); // FN
  }

  Cookie getC1() {
    return new Cookie("name", "value"); // Noncompliant [[sc=12;ec=39]]
  }

  void httpCookie() {
    HttpCookie c1 = new HttpCookie("name", "value");
    c1.setHttpOnly(true);

    HttpCookie c2 = new HttpCookie("name", "value"); // Noncompliant

    HttpCookie c3 = new HttpCookie("name", "value"); // Noncompliant
    c3.setHttpOnly(false);

    HttpCookie c4 = new HttpCookie("name", "value"); // Noncompliant
    c4.setHttpOnly(FALSE_CONSTANT);

    HttpCookie c5; // Noncompliant
    c5 = new HttpCookie("name", "value");
    c3.setHttpOnly(false);

    field5 = new HttpCookie("name, value"); // FN
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
    SimpleCookie c2 = new SimpleCookie(); // Noncompliant
    c2.setHttpOnly(false);
    SimpleCookie c3 = new SimpleCookie(); // Apache Shiro cookies have HttpOnly 'true' value by default
    SimpleCookie c4 = new SimpleCookie("name");
  }

  SimpleCookie getC4() {
    return new SimpleCookie(); // compliant
  }

  void playFw() {
    play.mvc.Http.Cookie c1 = new play.mvc.Http.Cookie("1", "2", 3, "4", "5", true, false); // Noncompliant
    play.mvc.Http.Cookie c2 = new play.mvc.Http.Cookie("1", "2", 3, "4", "5", true, true);
    play.mvc.Http.CookieBuilder cb1 = play.mvc.Http.Cookie.builder("1", "2");
    cb1.withHttpOnly(false); // Noncompliant
    cb1.withHttpOnly(true); // is ignored, so above is a FN
    play.mvc.Http.CookieBuilder cb2 = play.mvc.Http.Cookie.builder("1", "2");
    cb2.withHttpOnly(true);
    play.mvc.Http.Cookie.builder("1", "2")
        .withMaxAge(1)
        .withPath("x")
        .withDomain("x")
        .withSecure(true)
        .withHttpOnly(false) // Noncompliant
        .build();
    play.mvc.Http.Cookie.builder("theme", "blue").withHttpOnly(true);
  }

  play.mvc.Http.Cookie getC5() {
    return new play.mvc.Http.Cookie("1", "2", 3, "4", "5", true, false); // Noncompliant
  }

  play.mvc.Http.Cookie getC6() {
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

    play.mvc.Http.Cookie xsfrToken9 = new play.mvc.Http.Cookie("XSRF-TOKEN", "2", 3, "4", "5", true, false);

    play.mvc.Http.Cookie.builder("XSRF-TOKEN", "2")
      .withMaxAge(1)
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

    this.getXsfrTokenProp2() = play.mvc.Http.Cookie.builder("XSRF-TOKEN", "2");
    this.getXsfrTokenProp2().withHttpOnly(false);

    this.unknown = play.mvc.Http.Cookie.builder("XSRF-TOKEN", "2"); // Coverage
    unknown = play.mvc.Http.Cookie.builder("XSRF-TOKEN", "2"); // Coverage
    boolean secure = play.mvc.Http.Cookie.secure(); // Coverage

    return new Cookie("XSRF-TOKEN", "value");
  }

  play.mvc.Http.Cookie getXsfrTokenProp2() {
    return this.xsfrTokenProp2;
  }

  void compliant(Cookie c1, HttpCookie c2, javax.ws.rs.core.Cookie c3, NewCookie c4, SimpleCookie c5) {
    c1.isHttpOnly();
    c2.isHttpOnly();
    c3.isHttpOnly();
    c4.isHttpOnly();
    c5.isHttpOnly();
    SavedCookie c6 = new SavedCookie(c1); // Spring cookies are HttpOnly, without possibility to change that
    SavedCookie c7 = new SavedCookie("n", "v", "c", "d", 1, "p", false, 1);
  }

  SavedCookie getC7() {
    return new SavedCookie("n", "v", "c", "d", 1, "p", false, 1); // compliant
  }
}

class A extends Cookie {
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
  A a;
  public void setHttpOnly(boolean isHttpOnly) { }
  void foo() {
    setHttpOnly(false);
  }
  void bar() { return; }
  A getA() {
    return new A(); // Noncompliant
  }
  void baw() {
    int i;
    i = 1;
    a.c = new Cookie("1", "2"); // FN
    unknown = new A("1", "2");
  }
}
