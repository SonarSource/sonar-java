package checks;

import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

public class CookieHttpOnlyCheckSample {

  public void cookie() {
    CookieCsrfTokenRepository cookieCsrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse(); // Noncompliant
    cookieCsrfTokenRepository.setCookieDomain("Domain");

    org.springframework.boot.web.server.Cookie cookie = new org.springframework.boot.web.server.Cookie();
    cookie.setHttpOnly(false); // Noncompliant

    ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from("test", "test");
    builder.httpOnly(false); // Noncompliant
  }

}
