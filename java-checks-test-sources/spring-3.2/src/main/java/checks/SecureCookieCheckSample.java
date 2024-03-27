package checks;

import org.springframework.boot.web.server.Cookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseCookie.ResponseCookieBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecureCookieCheckSample {

  @GetMapping(value = "/cookie")
  public String cookie() {
    Cookie cookie = new Cookie();
    cookie.setSecure(false); // Noncompliant
    cookie.setName("X-Test-Cookie");

    ResponseCookieBuilder builder = ResponseCookie.from("test");
    builder.secure(false); // Noncompliant
    ResponseCookie responseCookie = builder.build();

    return "Enjoy your Cookie!";
  }

}
