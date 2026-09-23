package checks.spring.context;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod | staging")
@Configuration
class ProfileExpressionConfiguration {

  @Profile("!cloud")
  @Bean
  ApplicationContext profiledBean() {
    return null;
  }

  @Bean
  ApplicationContext unprofiledBean() {
    return null;
  }
}
