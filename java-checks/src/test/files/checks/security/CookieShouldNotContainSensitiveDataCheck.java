import java.net.HttpCookie;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.NewCookie;
import org.apache.shiro.web.servlet.SimpleCookie;
import org.springframework.security.web.savedrequest.SavedCookie;

class S2255 {
  private static final String VALUE = "value";

  void servletCookie(Cookie c){
    Cookie cookie = new Cookie("name", "value"); // Noncompliant [[sc=40;ec=47]] {{Make sure that this cookie is used safely.}}
    cookie.setValue("value"); // Noncompliant [[sc=21;ec=28]]
    String x = "value";
    cookie.setValue(x); // Noncompliant
    cookie.setValue(VALUE); // Noncompliant
    c.setValue("x"); // Noncompliant
    cookie.getValue(); // Noncompliant [[sc=12;ec=20]]
  }

  void jaxRsCookie() {
    javax.ws.rs.core.Cookie cookie=new javax.ws.rs.core.Cookie("name", "value"); // Noncompliant
    cookie = new javax.ws.rs.core.Cookie("name", "value", "path", "domain"); // Noncompliant
    new NewCookie("name", "value", "path", "domain", "comment", 1, true); // Noncompliant
    new NewCookie(cookie, "comment", 2, true); // Noncompliant
    new NewCookie(cookie); // Noncompliant
    cookie.getValue(); // Noncompliant [[sc=12;ec=20]]
  }

  void httpCookie(HttpCookie hc) {
    HttpCookie cookie = new HttpCookie("name", "value"); // Noncompliant
    cookie.setValue("value"); // Noncompliant
    hc.setValue("x"); // Noncompliant
    cookie.getValue(); // Noncompliant [[sc=12;ec=20]]
  }

  void shiroCookie(SimpleCookie cookie) {
    SimpleCookie sc = new SimpleCookie(cookie); // Noncompliant
    cookie.setValue("value"); // Noncompliant
    sc.setValue("value"); // Noncompliant
    cookie.getValue(); // Noncompliant [[sc=12;ec=20]]
  }

  void springCookie(Cookie c, SavedCookie cookie) {
    new SavedCookie(c); // Noncompliant
    new SavedCookie("n", "v", "c", "d", 1, "p", true, 1); // Noncompliant
    cookie.getValue(); // Noncompliant [[sc=12;ec=20]]
  }

  public String myPage(@org.springframework.web.bind.annotation.CookieValue("cookieName") String myCookie) { // Noncompliant [[sc=24;ec=90]]
    return "test";
  }

  void playCookie(play.mvc.Http.Cookie cookie) {
    play.mvc.Http.Cookie.builder("name", "value"); // Noncompliant [[sc=42;ec=49]]
    play.mvc.Http.Cookie.builder("name", "");

    new play.mvc.Http.CookieBuilder()
      .withName("name")
      .withValue("value") // Noncompliant [[sc=18;ec=25]]
      .build();

    new play.mvc.Http.CookieBuilder()
      .withName("name")
      .withValue(null)
      .build();
    cookie.value(); // Noncompliant [[sc=12;ec=17]]
  }

  void foo(HttpServletRequest request, HttpServletResponse response){
    response.addCookie(request.getCookies()[0]); // FN, needs symbolic execution
  }

  void compliant(Cookie c1, HttpCookie c2, javax.ws.rs.core.Cookie c3, NewCookie c4, SimpleCookie c5, SavedCookie c6) {
    c1.getValue(); // Noncompliant
    c2.getValue(); // Noncompliant
    c3.getValue(); // Noncompliant
    c4.getValue(); // Noncompliant
    c5.getValue(); // Noncompliant
    c6.getValue(); // Noncompliant
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
