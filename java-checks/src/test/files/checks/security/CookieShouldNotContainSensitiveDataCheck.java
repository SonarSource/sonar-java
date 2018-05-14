import java.net.HttpCookie;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.NewCookie;

class S2255 {

  void servletCookie(){
    Cookie cookie = new Cookie("name", "value"); // Noncompliant {{If the data stored in this cookie is sensitive, it should be stored internally in the user session.}}
//                      ^^^^^^
    cookie.setValue("value"); // Noncompliant
//         ^^^^^^^^
  }

  void jaxRsCookie() {
    javax.ws.rs.core.Cookie cookie=new javax.ws.rs.core.Cookie("name", "value"); // Noncompliant
    cookie = new javax.ws.rs.core.Cookie("name", "value", "path", "domain"); // Noncompliant
    NewCookie nc = new NewCookie("name", "value", "path", "domain", "comment", 1, true); // Noncompliant
  }

  void httpCookie() {
    HttpCookie cookie = new HttpCookie("name", "value"); // Noncompliant
    cookie.setValue("value"); // Noncompliant
  }

  void compliant(Cookie servletCookie, HttpCookie httpCookie, javax.ws.rs.core.Cookie jaxRsCookie) {
    servletCookie.getValue();
    httpCookie.getValue();
    jaxRsCookie.getValue();
    NewCookie newCookie = new NewCookie(jaxRsCookie, "comment", 2, true);
    newCookie.getValue();
  }
}
