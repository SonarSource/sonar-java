package test;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.password.StandardPasswordEncoder;

@EnableWebSecurity
public class SecurityConfig {

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
  public void ldapAuthentication(AuthenticationManagerBuilder auth) throws Exception {
    auth.ldapAuthentication().passwordEncoder(new MessageDigestPasswordEncoder("MD5")); // Noncompliant
  }
}
