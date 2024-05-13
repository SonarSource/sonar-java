package checks.spring;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.web.SecurityFilterChain;

public class SpringSessionFixationCheckSample {
  @Configuration
  @EnableWebSecurity
  public static class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {

      http.sessionManagement()
        .sessionFixation()
          .none(); // Noncompliant {{Create a new session during user authentication to prevent session fixation attacks.}}
//         ^^^^

      http.sessionManagement()
        .sessionFixation()
          .newSession(); // Compliant

    }

    public SecurityFilterChain filterChainSessionFixation(HttpSecurity http) throws Exception {

      // Noncompliant@+1 {{Create a new session during user authentication to prevent session fixation attacks.}}
      http.sessionManagement(sessionConfigurer -> sessionConfigurer.sessionFixation(fixationConfigurer -> fixationConfigurer.none()));
//                                                                                                                           ^^^^

      // Noncompliant@+1 {{Create a new session during user authentication to prevent session fixation attacks.}}
      http.sessionManagement(sessionConfigurer -> sessionConfigurer.sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::none));
//                                                                                                                                         ^^^^
      return http.build();
    }

  }
}
