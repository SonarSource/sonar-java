package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Configuration
public class ProfiledConsumersConfig {

  @Bean
  public Runnable plainCollaborator() {
    return null;
  }

  @Bean
  @Profile("staging")
  public Runnable stagingCollaborator() {
    return null;
  }
}

@Component
@Profile("dev")
class DevProfileConsumer {

  @Autowired
  private Runnable collaborator;
}

@Component
@Profile("staging")
class StagingProfileConsumer {

  @Autowired
  private Runnable collaborator;
}
