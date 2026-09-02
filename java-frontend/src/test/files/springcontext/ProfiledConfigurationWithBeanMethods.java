package checks.spring.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@Configuration
class ProfiledConfigurationWithBeanMethods {

  @Bean
  ApplicationContext inheritedProfileBean() {
    return null;
  }

  @Profile("test")
  @Bean
  ApplicationContext ownProfileBean() {
    return null;
  }
}
