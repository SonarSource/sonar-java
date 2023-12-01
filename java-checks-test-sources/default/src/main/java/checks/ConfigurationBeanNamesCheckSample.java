package checks;

import javax.annotation.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;

public class ConfigurationBeanNamesCheckSample {

  @Configuration
  class Config1 {
    @Bean
    public User user() {
      return new User("user", "password", true, true, true, true, null);
    }

    @Bean
    public User user(String name) { // Noncompliant
      return new User(name, "password", true, true, true, true, null);
    }
  }

  @Configuration
  class Config2 {
    @Bean
    public User user() {
      return new User("user", "password", true, true, true, true, null);
    }

    @Bean
    public User userWithName(String name) { // Compliant
      return new User(name, "password", true, true, true, true, null);
    }
  }

  @Configuration
  class Config3 {
    @Bean
    public User user() { // Compliant
      return new User("user", "password", true, true, true, true, null);
    }
  }

  @Configuration
  class Config4 {
    @Bean
    public User user() { // Compliant
      return new User("user", "password", true, true, true, true, null);
    }

    @Nullable
    public User user(String name) {
      return new User(name, "password", true, true, true, true, null);
    }
  }

  @Configuration
  class Config5 {
    @Bean
    public User user() {
      return new User("user", "password", true, true, true, true, null);
    }

    @Bean
    public User userWithName(String name) { // Compliant
      return new User(name, "password", true, true, true, true, null);
    }

    @Bean
    public User user(String name, String password) { // Noncompliant
      return new User(name, password, true, true, true, true, null);
    }

    @Bean
    public User user(String name, String password, boolean enabled) { // Noncompliant
      return new User(name, password, enabled, true, true, true, null);
    }
  }

}
