package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class DistinctProfileCandidates {

  @Autowired
  private Runnable collaborator;

  @Bean
  @Profile("dev")
  public Runnable devCollaborator() {
    return null;
  }

  @Bean
  @Profile("prod")
  public Runnable prodCollaborator() {
    return null;
  }
}
