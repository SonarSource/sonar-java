package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class CombinedProfilesCandidates {

  @Autowired
  private Runnable collaborator;

  @Bean
  @Profile("cloud & eu")
  public Runnable cloudEuCollaboratorA() {
    return null;
  }

  @Bean
  @Profile("cloud & eu")
  public Runnable cloudEuCollaboratorB() {
    return null;
  }
}
