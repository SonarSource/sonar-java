package checks.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

public class SpringSessionFixationCheck {
  @Configuration
  @EnableWebSecurity
  public static class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {

      http.sessionManagement()
        .sessionFixation()
          .none(); // Noncompliant [[sc=12;ec=16]] {{Create a new session during user authentication to prevent session fixation attacks.}}

      http.sessionManagement()
        .sessionFixation()
          .newSession(); // Compliant

    }
  }
}
