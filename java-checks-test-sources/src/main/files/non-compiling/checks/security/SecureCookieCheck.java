package checks.security;

import java.net.HttpCookie;
import java.util.Date;
import javax.servlet.http.Cookie;

class SecureCookieCheck {

  Cookie field4;
  HttpCookie field6;
  UnknownCookie field7;

  Cookie servletCookie() {
    field4 = new Cookie("name, value"); // Noncompliant
    return new Cookie("name", "value"); // Noncompliant
  }

  HttpCookie getHttpCookie() {
    field6 = new HttpCookie("name, value"); // Noncompliant
    unknown = new HttpCookie("name, value"); // Noncompliant
    return new HttpCookie("name", "value"); // Noncompliant
  }

}

class SecureCookieCheckB extends Cookie {
  public SecureCookieCheckB(String name, String value) {
    super(name, value);
  }

  Date codeCoverage(Cookie cookie) {
    UnknownClass c = new UnknownClass();
    c.setSecure(true);
    return new Date();
  }
}
