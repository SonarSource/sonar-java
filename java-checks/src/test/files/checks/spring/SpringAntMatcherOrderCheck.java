package files.checks.spring;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;

public class App {

  protected void configure(HttpSecurity http, String dynamicUrl) throws Exception {

    http
      .authorizeRequests()
        .antMatchers("/login/*me").permitAll()
        .antMatchers("/login/admin").hasRole("ADMIN")
        .antMatchers("/login/home").permitAll() // Noncompliant [[sc=22;ec=35;secondary=14]] {{Reorder the URL patterns from most to less specific, the pattern "/login/home" should occurs before "/login/*me".}}
        .antMatchers(
          "/lo?in",
          dynamicUrl,
          "/login" // Noncompliant [[sc=11;ec=19;secondary=18]] {{Reorder the URL patterns from most to less specific, the pattern "/login" should occurs before "/lo?in".}}
        ).permitAll();

    http
      .authorizeRequests()
        .antMatchers("/login/home").permitAll()
        .antMatchers("/login").hasRole("ADMIN")
        .antMatchers("/login/*me").permitAll();

    http
      .authorizeRequests()
      .antMatchers(HttpMethod.GET,"/home").permitAll()
      .antMatchers(HttpMethod.POST,"/home").permitAll();

    http
      .antMatcher("/root1")
        .authorizeRequests()
          .antMatchers("/level1").permitAll()
    .and()
      .antMatcher("/root2")
        .authorizeRequests()
          .antMatchers("/level1").permitAll();

    http
      .antMatcher("/root1")
        .authorizeRequests()
          .antMatchers("/level1").permitAll()
          .antMatchers("/level1").permitAll(); // Noncompliant

    http
      .authorizeRequests()
        .antMatchers("/", "/index", "/secured/*/**", "/authenticate").permitAll()
        .antMatchers("/secured/*/index", "/secured/socket", "/secured/success").authenticated() // Noncompliant [[sc=22;ec=40;secondary=51]] {{Reorder the URL patterns from most to less specific, the pattern "/secured/*/index" should occurs before "/secured/*/**".}}
        .anyRequest().authenticated();

    ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry authorizeRequests = http.authorizeRequests();
    authorizeRequests.antMatchers("/login").permitAll();
    authorizeRequests.antMatchers("/login").permitAll(); // false-negative, limitation

    // coverage
    object o = http
      .authorizeRequests()
      .antMatchers().urlMappings;

  }

}
