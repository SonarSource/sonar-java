import java.net.HttpCookie;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.NewCookie;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.security.web.savedrequest.SavedCookie;

class S2255 {
  private static final String VALUE = "value";

  void servletCookie(){
    Cookie cookie = new Cookie("name", "value"); // Noncompliant [[sc=40;ec=47]] {{If the data stored in this cookie is sensitive, it should be stored internally in the user session.}}
    cookie.setValue("value"); // Noncompliant [[sc=20;ec=29]]
    String x = "value";
    cookie.setValue(x); // Noncompliant
    cookie.setValue(VALUE); // Noncompliant
  }

  void jaxRsCookie() {
    javax.ws.rs.core.Cookie cookie=new javax.ws.rs.core.Cookie("name", "value"); // Noncompliant
    cookie = new javax.ws.rs.core.Cookie("name", "value", "path", "domain"); // Noncompliant
    new NewCookie("name", "value", "path", "domain", "comment", 1, true); // Noncompliant
    new NewCookie(cookie, "comment", 2, true); // Noncompliant
    new NewCookie(cookie); // Noncompliant
  }

  void httpCookie() {
    HttpCookie cookie = new HttpCookie("name", "value"); // Noncompliant
    cookie.setValue("value"); // Noncompliant
  }

  void shiroCookie(SimpleCookie cookie) {
    SimpleCookie sc = new SimpleCookie(cookie); // Noncompliant
    sc.setValue("value"); // Noncompliant
  }

  void springCookie(Cookie cookie) {
    new SavedCookie(cookie); // Noncompliant
    new SavedCookie("n", "v", "c", "d", 1, "p", true, 1); // Noncompliant
  }

  void foo(HttpServletRequest request, HttpServletResponse response){
    response.addCookie(request.getCookies()[0]); // FN, needs simbolic execution
  }

  void compliant(Cookie c1, HttpCookie c2, javax.ws.rs.core.Cookie c3, NewCookie c4, SimpleCookie c5, SavedCookie c6) {
    c1.getValue();
    c2.getValue();
    c3.getValue();
    c4.getValue();
    c5.getValue();
    c6.getValue();
    new SimpleCookie();
    new SimpleCookie("name");
  }
}
