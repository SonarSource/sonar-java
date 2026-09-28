package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class NegatedProfileCandidates {

  @Autowired
  private Runnable collaborator;

  @Bean
  @Profile("cloud")
  public Runnable cloudCollaborator() {
    return null;
  }

  @Bean
  @Profile("!cloud")
  public Runnable onPremiseCollaborator() {
    return null;
  }
}
