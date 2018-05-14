import java.net.HttpCookie;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

class S2255 {

  void createServletCookie(HttpServletRequest request, HttpServletResponse response){
    Cookie cookie = new Cookie("userAccountID", "1234"); // Noncompliant {{If the data stored in this cookie is sensitive, it should be stored internally in the user session.}}
//                      ^^^^^^
    response.addCookie(cookie);
  }

  void setPasswordInCookie(Cookie cookie) {
    cookie.setValue("my password"); // Noncompliant
//         ^^^^^^^^
  }

  void createRestfulCookie() {
    javax.ws.rs.core.Cookie cookie=new javax.ws.rs.core.Cookie("sessionId", "HEX1234ABCD"); // Noncompliant
    cookie = new javax.ws.rs.core.Cookie("sessionId", "HEX1234ABCD", "path", "domain"); // Noncompliant
    cookie = new javax.ws.rs.core.Cookie("sessionId", "HEX1234ABCD", "path", "domain", 1); // Noncompliant
  }

  HttpCookie createHttpCookie() {
    return new HttpCookie("password", "my password"); // Noncompliant
  }

  void setPasswordInHttpCookie(HttpCookie cookie) {
    cookie.setValue("my password"); // Noncompliant
  }

  String getValue(Cookie cookie) {
    return cookie.getValue();
  }

  String getValue(javax.ws.rs.core.Cookie cookie) {
    return cookie.getValue();
  }

  String getValue(HttpCookie cookie) {
    return cookie.getValue();
  }
}
