package checks.security;

import java.util.Collection;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class UserEnumerationCheck {

  public String authenticate(String username, String password) {

    MyUserDetailsService s1 = new MyUserDetailsService();
    UserDetails u1 = s1.loadUserByUsername(username);

    if(u1 == null) {
      throw new BadCredentialsException(username+" doesn't exist in our database"); // Noncompliant
    }

    return "";
  }

  public String compliantAuthenticate(String username, String password) throws AuthenticationException {
    MyUserDetails user = null;
    try {
      MyUserDetailsService s1 = new MyUserDetailsService();
      user = s1.loadUserByUsername(username);
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
  }

  public void compliantConfig() {
    boolean b = false;

    DaoAuthenticationProvider daoauth = new DaoAuthenticationProvider();
    daoauth.setUserDetailsService(new MyUserDetailsService());
    daoauth.setPasswordEncoder(new BCryptPasswordEncoder());
    daoauth.setHideUserNotFoundExceptions(true); // Compliant
    daoauth.setHideUserNotFoundExceptions(b); // Compliant
  }



  public static class MyUserDetailsService implements UserDetailsService {
    @Override
    public MyUserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
      return null;
    }
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
  }
}
