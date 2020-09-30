package checks.security;

import java.util.Arrays;
import java.util.Collection;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class UserEnumerationCheck {

  public static final boolean MY_CONSTANT = false;

  public String authenticate(String username, String password) {

    MyUserDetailsService s1 = new MyUserDetailsService();
    UserDetails u1 = s1.loadUserByUsername(username);
    UserDetails u2 = s1.loadUserByUsername(username());


    if(u1 == null) {
      throw new BadCredentialsException(username + " doesn't exist in our database"); // Noncompliant
    }

    throw new RuntimeException(new StringBuffer(username + " doesn't exist in our database").toString()); // Noncompliant
  }

  private String username() {
    return "username";
  }

  public String authenticate2(String username, String password) {

    MyUserDetailsService s1 = new MyUserDetailsService();
    UserDetails u1 = s1.loadUserByUsername(username);

    if(u1 == null) {
      throw new RuntimeException(new StringBuffer(username + " doesn't exist in our database").toString()); // Noncompliant
    }

    try {
      doSomethingThrowingUsernameNotFoundException();
    } catch (UsernameNotFoundException e) {
      if (u1 == null) {
        doSomething();
      } else {
        throw e; // Noncompliant
      }

    }

    return "";
  }

  private void doSomething() {
    System.out.println("Hello!");
  }

  private void doSomethingThrowingUsernameNotFoundException() {
    throw new UsernameNotFoundException(""); // Noncompliant
  }

  public String compliantAuthenticate(String username, String password) throws AuthenticationException {
    MyUserDetails user = null;
    try {
      MyUserDetailsService s1 = new MyUserDetailsService();
      user = s1.loadUserByUsername(username);
      user.new NestedClass();
    } catch (UsernameNotFoundException | DataAccessException e) {
      // Hide this exception reason to not disclose that the username doesn't exist
    }
    if (user == null || !user.isPasswordCorrect(password)) {
      // User should not be able to guess if the bad credentials message is related to the username or the password
      throw new BadCredentialsException("Bad credentials"); // Compliant
    }
    return "Compliant";
  }

  public void config() {
    DaoAuthenticationProvider daoauth = new DaoAuthenticationProvider();
    daoauth.setUserDetailsService(new MyUserDetailsService());
    daoauth.setPasswordEncoder(new BCryptPasswordEncoder());
    daoauth.setHideUserNotFoundExceptions(false); // Noncompliant {{Make sure allowing user enumeration is safe here.}}
    daoauth.setHideUserNotFoundExceptions(MY_CONSTANT); // Noncompliant {{Make sure allowing user enumeration is safe here.}}
    boolean variableFalse = false;
    daoauth.setHideUserNotFoundExceptions(variableFalse); // Compliant, not a constant
    throw new UsernameNotFoundException("userName not found"); // Noncompliant
  }

  public void compliantConfig() {
    boolean b = false;

    DaoAuthenticationProvider daoauth = new DaoAuthenticationProvider();
    daoauth.setUserDetailsService(new MyUserDetailsService());
    daoauth.setPasswordEncoder(new BCryptPasswordEncoder());
    daoauth.setHideUserNotFoundExceptions(true); // Compliant
    daoauth.setHideUserNotFoundExceptions(b); // Compliant

    throw new AuthenticationCredentialsNotFoundException("username not found"); // Compliant
  }

  public static class MyUserDetailsService implements UserDetailsService {
    @Override
    public MyUserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
      return null;
    }
  }

  public static class MyUserDetailsService2 implements UserDetailsService {
    @Override
    public MyUserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
      throw new UsernameNotFoundException(""); // Compliant
    }
  }


  public static class MyUserDetailsService3 implements UserDetailsService {
    @Override
    public MyUserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
       throwUserNotFoundException();
       return null;
    }
  }

  public static class MyUserDetailsService4 implements UserDetailsService {
    @Override
    public MyUserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
      Arrays.asList("1", "2").stream().map(str -> {
        return str + "123";
      });

      if (s.equals("yolo"))
        return null;

      if (s.equals("yolo"))
        throw new UsernameNotFoundException(""); // Compliant

      switch(s) {
        case "admin":
          throw new UsernameNotFoundException(""); // Compliant
        case "batman":
          break;
      }
      return null;
    }
  }

  public static void throwUserNotFoundException() {
    throw new UsernameNotFoundException(""); // Noncompliant
  }

  public static class MyUserDetails implements UserDetails {

    public boolean isPasswordCorrect(String pass) {
      return true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      return null;
    }

    @Override
    public String getPassword() {
      return null;
    }

    @Override
    public String getUsername() {
      return null;
    }

    @Override
    public boolean isAccountNonExpired() {
      return false;
    }

    @Override
    public boolean isAccountNonLocked() {
      return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
      return false;
    }

    @Override
    public boolean isEnabled() {
      return false;
    }

    public class NestedClass {}
  }
}
