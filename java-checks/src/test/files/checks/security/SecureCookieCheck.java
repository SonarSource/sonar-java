import java.net.HttpCookie;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.NewCookie;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.security.web.savedrequest.SavedCookie;
import play.mvc.Http.CookieBuilder;

class A {

  Cookie field1 = new Cookie("name", "value"); // FN
  HttpCookie field2 = new HttpCookie("name", "value"); // FN
  javax.ws.rs.core.Cookie field3 = new javax.ws.rs.core.Cookie("name", "value"); // FN
  Cookie field4;
  Cookie field5;
  HttpCookie field6;
  UnknownCookie field7;
  private static final boolean FALSE_CONSTANT = false;

  void foo(Cookie cookie) {
    int age = cookie.getMaxAge();
  }

  Cookie servletCookie(Cookie cookie4) {
    Cookie cookie = new Cookie("name", "value");
    cookie.setSecure(true);
    Cookie cookie2 = new Cookie("name", "value"); // Noncompliant [[sc=12;ec=19]] {{Add the "secure" attribute to this cookie}}
    Cookie cookie3 = new Cookie("name", "value"); // Noncompliant {{Add the "secure" attribute to this cookie}}
    cookie3.setSecure(false);
    cookie4.setSecure(false); // FN
    Cookie cookie5 = new Cookie("name", "value");
    cookie5.setSecure(FALSE_CONSTANT); // FN
    return new Cookie("name", "value"); // FN
  }
}
