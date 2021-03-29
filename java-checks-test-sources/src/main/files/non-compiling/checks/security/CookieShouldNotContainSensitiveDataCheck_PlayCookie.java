package checks.security;

class CookieShouldNotContainSensitiveDataCheck_PlayCookie {
  void playCookie(play.mvc.Http.Cookie cookie) {
    play.mvc.Http.CookieBuilder builder = play.mvc.Http.Cookie.builder("name", "value"); // Noncompliant [[sc=80;ec=87]]
    play.mvc.Http.Cookie.builder("name", "");

    builder.withName("name")
      .withValue("value")  // Noncompliant
      .build();

    new play.mvc.Http.CookieBuilder()
      .withName("name")
      .withValue("value") // Noncompliant [[sc=18;ec=25]]
      .build();

    new play.mvc.Http.CookieBuilder()
      .withName("name")
      .withValue(null)
      .build();

    cookie.value(); // compliant
  }
}
