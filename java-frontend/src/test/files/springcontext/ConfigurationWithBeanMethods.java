package checks.spring.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ConfigurationWithBeanMethods {

  @Bean
  ApplicationContext simpleServiceBean() {
    return null;
  }

  @Bean(name = "namedBean")
  ApplicationContext namedBeanMethod() {
    return null;
  }

  @Bean(name = {"arrayNamedBean", "alias"})
  ApplicationContext arrayNamedBeanMethod() {
    return null;
  }

  @Bean(name = {})
  ApplicationContext emptyNameArrayMethod() {
    return null;
  }
}
