package checks.spring.s9352;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class UnreadableProfileCandidates {

  @Autowired
  private Runnable collaborator;

  @Bean
  @Profile("dev &")
  public Runnable unreadableCollaborator() {
    return null;
  }

  @Bean
  public Runnable plainCollaborator() {
    return null;
  }
}
