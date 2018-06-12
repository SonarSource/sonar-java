package files.checks.spring;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable(); // Noncompliant [[sc=17;ec=24]] {{Activate Spring Security's CSRF protection.}}
    http.csrf().getClass();

    http
      .logout().disable()
      .csrf().disable() // Noncompliant
      .cors().disable();

    // coverage
    http.csrf();
    http.csrf().disable;
  }
}
