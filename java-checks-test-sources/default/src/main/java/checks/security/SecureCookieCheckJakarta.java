package checks.security;

import java.util.Date;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.NewCookie;

class SecureCookieCheckJakarta {

  Cookie field1 = new Cookie("name", "value"); // Noncompliant
  jakarta.ws.rs.core.Cookie field3 = new jakarta.ws.rs.core.Cookie("name", "value"); // Noncompliant
  jakarta.ws.rs.core.Cookie cookie;
  NewCookie secureCookie = new NewCookie(cookie, "2", 3, true);
  NewCookie unsecureCookie = new NewCookie(cookie, "2", 3, false); // Noncompliant
  Cookie field4;
  Cookie field5;

  private static final boolean FALSE_CONSTANT = false;

  void foo(Cookie cookie) {
  }

  Cookie servletCookie(
    Cookie firstParam,
    Cookie secondParam,
    Cookie thirdParam,
    boolean param) {
    firstParam.setSecure(false); // Noncompliant {{Make sure creating this cookie without the "secure" flag is safe here.}}
//                      ^^^^^^^
    secondParam.setSecure(true);

    field5.setSecure(false); // Noncompliant
    this.field4 = new Cookie("name", "value"); // Noncompliant

    Cookie cookie = new Cookie("name", "value");
    cookie.setSecure(true);

    Cookie cookie2 = new Cookie("name", "value"); // Noncompliant {{Make sure creating this cookie without the "secure" flag is safe here.}}
//                       ^^^^^^

    Cookie cookie3 = new Cookie("name", "value");
    cookie3.setSecure(false); // Noncompliant {{Make sure creating this cookie without the "secure" flag is safe here.}}

    Cookie cookie5 = new Cookie("name", "value");
    cookie5.setSecure(FALSE_CONSTANT); // Noncompliant

    Cookie c6 = new Cookie("name", "value");
    if (param) {
      c6.setSecure(false); // Noncompliant
    } else {
      c6.setSecure(true);
    }

    Cookie c7 = new Cookie("name", "value");
    boolean b = false;
    c7.setSecure(b); // Noncompliant

    Cookie c8 = new Cookie("name", "value");
    c8.setSecure(param);

    Object c9 = new Cookie("name", "value"); // Noncompliant

    Cookie c10;
    c10 = new Cookie("name", "value");
    c10.setSecure(true);

    Object c12;
    c12 = new Cookie("name", "value"); // Noncompliant {{Make sure creating this cookie without the "secure" flag is safe here.}}
//            ^^^^^^

    Cookie c13 = new Cookie("name", "value");
    boolean value = false;
    c13.setSecure(!value);

    return new Cookie("name", "value"); // Noncompliant
  }

  NewCookie jaxRsNewCookie(jakarta.ws.rs.core.Cookie cookie) {
    NewCookie c1 = new NewCookie(cookie); // Noncompliant
    NewCookie c2 = new NewCookie(cookie, "2", 3, false); // Noncompliant
    NewCookie c3 = new NewCookie(cookie, "2", 3, true);
    NewCookie c4 = new NewCookie(cookie, "2", 3, new Date(), false, true); // Noncompliant
    NewCookie c5 = new NewCookie(cookie, "2", 3, new Date(), true, false);

    NewCookie c6 = new NewCookie("1", "2"); // Noncompliant

    NewCookie c7 = new NewCookie("1", "2", "3", "4", "5", 6, false, true); // Noncompliant
    NewCookie c8 = new NewCookie("1", "2", "3", "4", "5", 6, true, true);
    NewCookie c9 = new NewCookie("1", "2", "3", "4", 5, "6", 7, new Date(), false, true); // Noncompliant
    NewCookie c10 = new NewCookie("1", "2", "3", "4", 5, "6", 7, new Date(), true, false);

    NewCookie c11 = new NewCookie("1", "2", "3", "4", "5", 6, true);
    NewCookie c12 = new NewCookie("1", "2", "3", "4", "5", 6, false); // Noncompliant
    NewCookie c13 = new NewCookie("1", "2", "3", "4", "5", 6, false, false); // Noncompliant
    NewCookie c14 = new NewCookie("1", "2", "3", "4", "5", 6, true, false);

    return new NewCookie(cookie); // Noncompliant
  }

  class SecureCookieCheckBJakarta extends Cookie {
    public Cookie c;

    public SecureCookieCheckBJakarta(String name, String value) {
      super(name, value);
    }

    public void setSecure(boolean bool) {
    }

    void foo() {
      setSecure(false); // FN (to avoid implementation complexity)
    }

    Date d = new Date();

    void bar(boolean x) {
      setSecure(x);
    }

    void baz() {
      setSecure(true);
      return; // code coverage
    }

    Date codeCoverage(Cookie cookie) {
      SecureCookieCheckJakarta a = new SecureCookieCheckJakarta();
      a.foo(cookie);
      Date d1 = new Date();
      Date d2;
      d2 = d1;
      d2 = new Date();
      d = d1;
      d = new Date();
      return new Date();
    }

    class JavaNet {
      Cookie httpCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("name", "value"); // Noncompliant
        response.addCookie(new Cookie("name", "value")); // Noncompliant
        return new Cookie("name", "value"); // Noncompliant
      }
    }
  }
}
