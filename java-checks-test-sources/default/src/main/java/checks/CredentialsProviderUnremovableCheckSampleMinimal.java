package checks;

import io.quarkus.credentials.CredentialsProvider;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
class MinimalProvider implements CredentialsProvider { // Noncompliant {{Add the @Unremovable annotation to this CredentialsProvider implementation.}}
//    ^^^^^^^^^^^^^^^

  @Override
  public Map<String, String> getCredentials(String credentialsProviderName) {
    return null;
  }
}
