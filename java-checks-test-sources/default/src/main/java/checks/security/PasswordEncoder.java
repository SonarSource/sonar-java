package checks.security;

import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.security.crypto.password.Md4PasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@EnableWebSecurity
class SecurityConfig {

  @Autowired
  private DataSource dataSource;

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth, DataSource dataSource, UserDetailsService userDetailsService) throws Exception {
    auth.jdbcAuthentication()
      .dataSource(dataSource)
      .usersByUsernameQuery("SELECT * FROM users WHERE username = ?")
      .passwordEncoder(new StandardPasswordEncoder()); // Noncompliant {{Use secure "PasswordEncoder" implementation.}}

    auth.userDetailsService(userDetailsService).passwordEncoder(new StandardPasswordEncoder()); // Noncompliant
  }

  @Autowired
  public void jdbcAuthentication(AuthenticationManagerBuilder auth, DataSource dataSource, UserDetailsService userDetailsService) throws Exception {
    auth.jdbcAuthentication()  // Noncompliant {{Don't use the default "PasswordEncoder" relying on plain-text.}}
      .dataSource(dataSource)
      .usersByUsernameQuery("SELECT * FROM users WHERE username = ?");
  }

  @Autowired
  public void jdbcAuthenticationCompliant(AuthenticationManagerBuilder auth, DataSource dataSource, UserDetailsService userDetailsService) throws Exception {
    auth.jdbcAuthentication()
      .dataSource(dataSource)
      .usersByUsernameQuery("SELECT * FROM users WHERE username = ?")
      .passwordEncoder(new BCryptPasswordEncoder());  // Compliant - secure password encoder
  }

  @Autowired
  public void userDetailsService(AuthenticationManagerBuilder auth, DataSource dataSource, UserDetailsService userDetailsService) throws Exception {
    auth.userDetailsService(userDetailsService); // Noncompliant {Don't use the default "PasswordEncoder" relying on plain-text.}}
  }

  @Autowired
  public void userDetailsService2(AuthenticationManagerBuilder auth, DataSource dataSource, UserDetailsService userDetailsService) throws Exception {
    auth.userDetailsService(userDetailsService).passwordEncoder(new Pbkdf2PasswordEncoder()); // Compliant - secure encoder
  }

  @Autowired
  public void userDetailsService3(AuthenticationManagerBuilder auth, DataSource dataSource, UserDetailsService userDetailsService) throws Exception {
    auth.userDetailsService(null); // Noncompliant
  }

  @Autowired
  public void userDetailsService4(AuthenticationManagerBuilder auth, DataSource dataSource, UserDetailsService userDetailsService) throws Exception {
    auth.userDetailsService(null).passwordEncoder(new Pbkdf2PasswordEncoder());  // Compliant
  }

  @Autowired
  public void ldapAuthentication(AuthenticationManagerBuilder auth) throws Exception {
    auth.ldapAuthentication().passwordEncoder(new MessageDigestPasswordEncoder("MD5")); // Noncompliant
  }
}

@RestController
class RestControllerPasswordEncoderSample {

  @GetMapping(value = "/passwordEncoder2")
  public String passwordEncoder() {
    Map<String, PasswordEncoder> encoders = new HashMap<>();
    encoders.put("noop", NoOpPasswordEncoder.getInstance()); // Noncompliant
    encoders.put("md4", new Md4PasswordEncoder()); // Noncompliant
    encoders.put("md5", new MessageDigestPasswordEncoder("md5")); // Noncompliant
    encoders.put("SHA-1", new MessageDigestPasswordEncoder("SHA-1")); // Noncompliant
    encoders.put("ldap", new LdapShaPasswordEncoder()); // Noncompliant
    encoders.put("sha-256", new StandardPasswordEncoder()); // Noncompliant
    encoders.put("scrypt", new SCryptPasswordEncoder(10,10,10,10,10)); // Noncompliant

    PasswordEncoder passwordEncoder = new DelegatingPasswordEncoder("noop", encoders);

    return passwordEncoder.encode("Password");
  }
}
