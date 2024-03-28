package checks.spring;

import javax.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

@EnableWebSecurity
public class SpringSecurityDisableCSRFCheck extends WebSecurityConfigurerAdapter {

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

  @Configuration
  public static class CSRFConfig {

    @Bean
    public SecurityFilterChain filterChainCSRF(HttpSecurity http) throws Exception {
      RequestMatcher requestMatcherWhitelist = (HttpServletRequest request) -> request.getRequestURI().contains("whitelist");
      RequestMatcher requestMatcherBlacklist = (HttpServletRequest request) -> request.getRequestURI().contains("blacklist");

      http.csrf(csrf -> csrf.requireCsrfProtectionMatcher(requestMatcherWhitelist));  // Noncompliant
      http.csrf(csrf -> csrf.ignoringRequestMatchers(requestMatcherBlacklist));       // Noncompliant
      http.csrf(csrf -> csrf.ignoringRequestMatchers("/S4502CSRFSpecial"));  // Noncompliant
      http.csrf(csrf -> csrf.ignoringAntMatchers("/ignored/path")); // Noncompliant
      http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
      http.csrf(csrf -> csrf.disable()); // Noncompliant

      http.cors(cors -> cors.disable()); // Compliant - not CSRF
      http.cors(AbstractHttpConfigurer::disable); // Compliant - not CSRF

      return http.build();
    }
  }
}
