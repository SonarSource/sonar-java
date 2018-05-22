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

  Cookie c1 = new Cookie("name", "value"); // FN
  HttpCookie c3 = new HttpCookie("name", "value"); // FN
  javax.ws.rs.core.Cookie c2 = new javax.ws.rs.core.Cookie("name", "value"); // FN

  void servletCookie(boolean param, Cookie c0) {
    c0.setHttpOnly(false); // Noncompliant [[sc=19;ec=26]] {{Add the "HttpOnly" cookie attribute.}}

    Cookie c1 = new Cookie("name", "value");
    c1.setHttpOnly(true);

    Cookie c2 = new Cookie("name", "value"); // Noncompliant [[sc=12;ec=14]]

    Cookie c3 = new Cookie("name", "value"); // Noncompliant
    c3.setHttpOnly(false);

    Cookie c4 = new Cookie("name", "value");
    c4.setHttpOnly(FALSE_CONSTANT); // FN, would require SE

    Cookie c5 = new Cookie("name", "value");
    boolean b = true;
    c5.setHttpOnly(b); // FN, would require SE

    Cookie c6 = new Cookie("name", "value");
    c6.setHttpOnly(param);
  }

  Cookie getC1() {
    return new Cookie("name", "value"); // FN
  }

  void httpCookie() {
    HttpCookie c1 = new HttpCookie("name", "value");
    c1.setHttpOnly(true);

    HttpCookie c2 = new HttpCookie("name", "value"); // Noncompliant

    HttpCookie c3 = new HttpCookie("name", "value"); // Noncompliant
    c3.setHttpOnly(false);

    HttpCookie c4 = new HttpCookie("name", "value");
    c4.setHttpOnly(FALSE_CONSTANT); // FN, would require SE
  }

  HttpCookie getC2() {
    return new HttpCookie("name", "value"); // FN
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
    return new NewCookie("name", "value", "path", "domain", "comment", 1, true); // FN
  }

  void apacheShiro(SimpleCookie unknownCookie) {
    SimpleCookie c1 = new SimpleCookie(unknownCookie); // Noncompliant
    SimpleCookie c2 = new SimpleCookie(); // Noncompliant
    c2.setHttpOnly(false);
    SimpleCookie c3 = new SimpleCookie(); // Apache Shiro cookies have HttpOnly 'true' value by default
    SimpleCookie c4 = new SimpleCookie("name");
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
}
