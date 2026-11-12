package checks.spring;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@EnableWebSecurity
public class SpringSecurityDisableCSRFCheckSample extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.csrf().disable(); // Noncompliant {{Make sure disabling Spring Security's CSRF protection is safe here.}}
//              ^^^^^^^
    http.csrf().getClass();

    http
      .logout().disable()
      .csrf().disable() // Noncompliant
      .cors().disable();

    CsrfConfigurer<HttpSecurity> csrf = http.csrf();
    csrf.disable(); // Noncompliant

    LogoutConfigurer<HttpSecurity> logout = http.logout();
    logout.disable();

    http.csrf().ignoringAntMatchers("/ignored/path"); // Noncompliant {{Make sure disabling Spring Security's CSRF protection is safe here.}}
//              ^^^^^^^^^^^^^^^^^^^
    CsrfConfigurer<HttpSecurity> csrf2 = http.csrf();
    csrf2.ignoringAntMatchers("/ignored/path"); // Noncompliant {{Make sure disabling Spring Security's CSRF protection is safe here.}}
//        ^^^^^^^^^^^^^^^^^^^
  }

  @Configuration
  public static class CSRFConfig {

    @Bean
    public SecurityFilterChain filterChainCSRF(HttpSecurity http) throws Exception {
      RequestMatcher requestMatcherWhitelist = (HttpServletRequest request) -> request.getRequestURI().contains("whitelist");
      RequestMatcher requestMatcherBlacklist = (HttpServletRequest request) -> request.getRequestURI().contains("blacklist");

      http.csrf(csrf -> csrf.requireCsrfProtectionMatcher(requestMatcherWhitelist)); // Noncompliant
      http.csrf(csrf -> csrf.ignoringRequestMatchers(requestMatcherBlacklist)); // Noncompliant
      http.csrf(csrf -> csrf.ignoringRequestMatchers("/S4502CSRFSpecial")); // Noncompliant
      http.csrf(csrf -> csrf.ignoringAntMatchers("/ignored/path")); // Noncompliant
      http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
      http.csrf(csrf -> csrf.disable()); // Noncompliant

      http.cors(cors -> cors.disable()); // Compliant - not CSRF
      http.cors(AbstractHttpConfigurer::disable); // Compliant - not CSRF

      return http.build();
    }
  }

  @Bean
  SecurityFilterChain statelessResourceServer(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
      .oauth2ResourceServer(o -> o.jwt(withDefaults()));
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessResourceServerLambda(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**");
    http.csrf(c -> c.disable());
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessResourceServerFluent(HttpSecurity http) throws Exception {
    return http.csrf().disable()
      .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
      .oauth2ResourceServer().jwt().and().and().build();
  }

  @Bean
  SecurityFilterChain statelessFalseMatcher(HttpSecurity http) throws Exception {
    http.csrf(c -> c.requireCsrfProtectionMatcher(request -> false));
    http.csrf(c -> c.requireCsrfProtectionMatcher(request -> { return (false); }));
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain resourceServerWithoutStateless(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^

    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessBasic(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.httpBasic();
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessCustomFilter(HttpSecurity http, Filter filter) throws Exception {
    http.addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    return http.build();
  }

  @Bean
  SecurityFilterChain statelessResourceServerBasic(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    http.httpBasic();
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessResourceServerFormLogin(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    http.formLogin();
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessResourceServerOauthLogin(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    http.oauth2Login();
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessResourceServerRememberMe(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    http.rememberMe();
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessResourceServerX509(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    http.x509(withDefaults());
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessPartialExemptions(HttpSecurity http) throws Exception {
    http.csrf(c -> c.ignoringAntMatchers("/ignored")); // Noncompliant
//                   ^^^^^^^^^^^^^^^^^^^
    http.csrf(c -> c.requireCsrfProtectionMatcher(request -> request.isSecure())); // Noncompliant
//                   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain differentReceivers(HttpSecurity http, HttpSecurity other) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    other.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    other.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain disabledResourceServer(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    http.oauth2ResourceServer().disable();
    return http.build();
  }

  @Bean
  SecurityFilterChain overriddenSessionPolicy(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.ALWAYS));
    return http.build();
  }

  @Bean
  SecurityFilterChain conditionalSessionPolicy(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^

    http.oauth2ResourceServer(o -> o.jwt());
    http.sessionManagement(s -> { if (System.currentTimeMillis() > 0) s.sessionCreationPolicy(SessionCreationPolicy.STATELESS); });
    return http.build();
  }

  @Bean
  SecurityFilterChain deferredSessionPolicy(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^

    http.oauth2ResourceServer(o -> o.jwt());
    http.sessionManagement(s -> { Runnable deferred = () -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS); });
    return http.build();
  }

  @Bean
  SecurityFilterChain aliasedBuilder(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    HttpSecurity alias = http;
    alias.httpBasic();
    return http.build();
  }

  @Bean
  SecurityFilterChain disabledResourceServerReference(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(AbstractHttpConfigurer::disable);
    return http.build();
  }

  @Bean
  SecurityFilterChain disabledSessionManagement(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    http.sessionManagement().disable();
    return http.build();
  }

  @Bean
  SecurityFilterChain earlyReturnSessionPolicy(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^

    http.oauth2ResourceServer(o -> o.jwt());
    http.sessionManagement(s -> { if (System.currentTimeMillis() > 0) return; s.sessionCreationPolicy(SessionCreationPolicy.STATELESS); });
    return http.build();
  }

  @Bean
  SecurityFilterChain escapedSessionConfigurer(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> { s.sessionCreationPolicy(SessionCreationPolicy.STATELESS); makeStateful(s); });
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  void makeStateful(SessionManagementConfigurer<HttpSecurity> session) {
    session.sessionCreationPolicy(SessionCreationPolicy.ALWAYS);
  }

  @Bean
  SecurityFilterChain disabledSessionReference(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.sessionManagement(AbstractHttpConfigurer::disable);
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain opaqueSessionCustomizer(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.sessionManagement(this::makeStateful);
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain escapedResourceServerConfigurer(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> { o.jwt(); removeResourceServer(o); });
    return http.build();
  }

  void removeResourceServer(OAuth2ResourceServerConfigurer<HttpSecurity> resourceServer) {
    resourceServer.disable();
  }

  @Bean
  SecurityFilterChain escapedFluentSessionConfigurer(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> makeStateful(s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)));
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain escapedLegacySessionConfigurer(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    makeStateful(http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain escapedFluentResourceServerConfigurer(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> removeResourceServer(o.jwt().and()));
    return http.build();
  }

  @Bean
  SecurityFilterChain sharedOtherBuilder(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.getSharedObject(HttpSecurity.class)
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain deferredMatcherPolicy(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.securityMatcher(request -> {
      try {
        return http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS) == null;
      } catch (Exception exception) {
        return false;
      }
    });
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessStaticImport(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable);
    http.sessionManagement(s -> s.sessionCreationPolicy(STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessCsrfLast(HttpSecurity http) throws Exception {
    http.oauth2ResourceServer(o -> o.jwt());
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.csrf(AbstractHttpConfigurer::disable);
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessDisabledBrowserAuthentication(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
      .httpBasic(AbstractHttpConfigurer::disable)
      .formLogin(f -> f.disable())
      .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
      .oauth2ResourceServer(o -> o.jwt());
    http.rememberMe().disable();
    http.oauth2Login(AbstractHttpConfigurer::disable);
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessResourceServerDefaultBasic(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    http.httpBasic(withDefaults());
    return http.build();
  }

  @Bean
  SecurityFilterChain statelessResourceServerConfiguredBasic(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    http.httpBasic(b -> b.realmName("api"));
    return http.build();
  }

  @Bean
  SecurityFilterChain neverSessionPolicy(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.NEVER));
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain builderPassedToHelper(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    configureCommon(http);
    return http.build();
  }

  void configureCommon(HttpSecurity http) throws Exception {
    http.httpBasic();
  }

  SecurityFilterChain notABean(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  void notAFilterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
  }

  @Bean
  SecurityFilterChain csrfConfigurerVariable(HttpSecurity http) throws Exception {
    CsrfConfigurer<HttpSecurity> csrf = http.csrf();
    csrf.disable(); // Noncompliant
//       ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  private SecurityFilterChain cachedChain;
  private static final SessionCreationPolicy POLICY = SessionCreationPolicy.STATELESS;

  @Bean
  SecurityFilterChain statelessDefaultResourceServer(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable);
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(withDefaults());
    return http.build();
  }

  @Bean
  SecurityFilterChain opaqueResourceServerCustomizer(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(resourceServerCustomizer());
    return http.build();
  }

  Customizer<OAuth2ResourceServerConfigurer<HttpSecurity>> resourceServerCustomizer() {
    return o -> o.jwt();
  }

  @Bean
  SecurityFilterChain returnsAnotherChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    return cachedChain;
  }

  @Bean
  SecurityFilterChain statelessNotFalseMatchers(HttpSecurity http) throws Exception {
    http.csrf(c -> c.requireCsrfProtectionMatcher(request -> true)); // Noncompliant
//                   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    http.csrf(c -> c.requireCsrfProtectionMatcher(request -> { boolean secure = request.isSecure(); return false; })); // Noncompliant
//                   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

  @Bean
  SecurityFilterChain constantSessionPolicy(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable); // Noncompliant
//                                    ^^^^^^^
    http.sessionManagement(s -> s.sessionCreationPolicy(POLICY));
    http.oauth2ResourceServer(o -> o.jwt());
    return http.build();
  }

}
