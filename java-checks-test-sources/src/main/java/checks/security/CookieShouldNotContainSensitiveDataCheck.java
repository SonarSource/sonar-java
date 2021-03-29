package checks.security;

import java.net.HttpCookie;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.NewCookie;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.security.web.savedrequest.SavedCookie;

class CookieShouldNotContainSensitiveDataCheck {
  private static final String VALUE = "value";
  private static final String EMPTY_STRING = "";

  void servletCookie(Cookie c){
    Cookie cookie = new Cookie("name", "value"); // Noncompliant [[sc=40;ec=47]] {{Make sure that this cookie is used safely.}}
    cookie.setValue("value"); // Noncompliant [[sc=21;ec=28]]
    String x = "value";
    cookie.setValue(x); // Noncompliant
    cookie.setValue(VALUE); // Noncompliant
    c.setValue("x"); // Noncompliant
    cookie.getValue(); // compliant
    Cookie cookie2 = new Cookie("name", ""); // Compliant
    Cookie cookie3 = new Cookie("name", EMPTY_STRING); // Compliant
  }

  void jaxRsCookie() {
    javax.ws.rs.core.Cookie cookie=new javax.ws.rs.core.Cookie("name", "value"); // Noncompliant
    cookie = new javax.ws.rs.core.Cookie("name", "value", "path", "domain"); // Noncompliant
    new NewCookie("name", "value", "path", "domain", "comment", 1, true); // Noncompliant
    new NewCookie(cookie, "comment", 2, true); // Noncompliant
    new NewCookie(cookie); // Noncompliant
    cookie.getValue(); // compliant
  }

  void httpCookie(HttpCookie hc) {
    HttpCookie cookie = new HttpCookie("name", "value"); // Noncompliant
    cookie.setValue("value"); // Noncompliant
    hc.setValue("x"); // Noncompliant
    cookie.getValue(); // compliant
  }

  void shiroCookie(SimpleCookie cookie) {
    SimpleCookie sc = new SimpleCookie(cookie); // Noncompliant
    cookie.setValue("value"); // Noncompliant
    sc.setValue("value"); // Noncompliant
    cookie.getValue(); // compliant
  }

  void springCookie(Cookie c, SavedCookie cookie) {
    new SavedCookie(c); // compliant
    new SavedCookie("n", "v", "c", "d", 1, "p", true, 1); // compliant
    cookie.getValue(); // compliant
  }

  public String myPage(@org.springframework.web.bind.annotation.CookieValue("cookieName") String myCookie) { // compliant
    return "test";
  }

  void foo(HttpServletRequest request, HttpServletResponse response){
    response.addCookie(request.getCookies()[0]); // FN, needs symbolic execution
  }

  void compliant(Cookie c1, HttpCookie c2, javax.ws.rs.core.Cookie c3, NewCookie c4, SimpleCookie c5, SavedCookie c6) {
    c1.getValue(); // compliant
    c2.getValue(); // compliant
    c3.getValue(); // compliant
    c4.getValue(); // compliant
    c5.getValue(); // compliant
    c6.getValue(); // compliant
    c1.setValue(null);
    c1.setValue("");
    c1.setValue("   ");
    c2.setValue(null);
    c2.setValue("");
    c2.setValue("   ");
    c5.setValue(null);
    c5.setValue("");
    c5.setValue("    ");
    new SimpleCookie();
    new SimpleCookie("name");
    new Cookie("name", "");
    new Cookie("name", "  ");
    new Cookie("name", null);
    new javax.ws.rs.core.Cookie("name", "");
    new HttpCookie("name", null);
    new HttpCookie("name", "");
    new SavedCookie("n", "", "c", "d", 1, "p", true, 1);
    new SavedCookie("n", "   ", "c", "d", 1, "p", true, 1);
  }
}
