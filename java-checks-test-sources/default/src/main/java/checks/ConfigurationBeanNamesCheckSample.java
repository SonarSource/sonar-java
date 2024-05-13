package checks;

import javax.annotation.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class ConfigurationBeanNamesCheckSample {

  class User {
  }

  @Configuration
  class Config1 {
    @Bean
    public User user() {
      return new User();
    }

    @Bean
    public User user(String name) { // Noncompliant {{Rename this bean method to prevent any conflict with other beans.}}
//              ^^^^
      return new User();
    }
  }

  @Configuration
  class Config2 {
    @Bean
    public User user() {
      return new User();
    }

    @Bean
    public User userWithName(String name) { // Compliant
      return new User();
    }
  }

  @Configuration
  class Config3 {
    @Bean
    public User user() { // Compliant
      return new User();
    }
  }

  @Configuration
  class Config4 {
    @Bean
    public User user() { // Compliant
      return new User();
    }

    @Nullable
    public User user(String name) {
      return new User();
    }
  }

  @Configuration
  class Config5 {
    @Bean
    public User user() {
      return new User();
    }

    @Bean
    public User userWithName(String name) { // Compliant
      return new User();
    }

    @Bean
    public User user(String name, String password) { // Noncompliant
      return new User();
    }

    @Bean
    public User user(String name, String password, boolean enabled) { // Noncompliant
      return new User();
    }
  }

  @Configuration
  class Config6 {
  }

  @Configuration
  class Config7 {
    @Bean
    public User user1() {
      return new User();
    }

    @Bean
    public User user1(String name) { // Noncompliant
      return new User();
    }

    @Bean
    public User user2() {
      return new User();
    }

    @Bean
    public User user2(String name) { // Noncompliant
      return new User();
    }

  }

}
