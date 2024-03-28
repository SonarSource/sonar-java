package checks.spring;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;

@EnableWebSecurity
public class SpringSecurityDisableCSRFCheckSample extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable(); // Noncompliant [[sc=17;ec=24]] {{Make sure disabling Spring Security's CSRF protection is safe here.}}
    http.csrf().getClass();

    http
      .logout().disable()
      .csrf().disable() // Noncompliant
      .cors().disable();

    CsrfConfigurer<HttpSecurity> csrf = http.csrf();
    csrf.disable(); // Noncompliant

    LogoutConfigurer<HttpSecurity> logout = http.logout();
    logout.disable();

    http.csrf().ignoringAntMatchers("/ignored/path"); // Noncompliant [[sc=17;ec=36]] {{Make sure disabling Spring Security's CSRF protection is safe here.}}
    CsrfConfigurer<HttpSecurity> csrf2 = http.csrf();
    csrf2.ignoringAntMatchers("/ignored/path"); // Noncompliant [[sc=11;ec=30]] {{Make sure disabling Spring Security's CSRF protection is safe here.}}
  }
}
