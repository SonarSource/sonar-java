package checks;

import io.quarkus.arc.Unremovable;
import io.quarkus.credentials.CredentialsProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Map;

@ApplicationScoped
class MyCredentialsProvider implements CredentialsProvider { // Noncompliant {{Add the @Unremovable annotation to this CredentialsProvider implementation.}}
//    ^^^^^^^^^^^^^^^^^^^^^

  @Override
  public Map<String, String> getCredentials(String credentialsProviderName) {
    return Map.of(USER_PROPERTY_NAME, "user", PASSWORD_PROPERTY_NAME, "password");
  }
}

@ApplicationScoped
@Unremovable
class CompliantProvider implements CredentialsProvider {

  @Override
  public Map<String, String> getCredentials(String credentialsProviderName) {
    return Map.of(USER_PROPERTY_NAME, "user", PASSWORD_PROPERTY_NAME, "password");
  }
}

@RequestScoped
class RequestScopedProvider implements CredentialsProvider { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^

  @Override
  public Map<String, String> getCredentials(String credentialsProviderName) {
    return Map.of(USER_PROPERTY_NAME, "user", PASSWORD_PROPERTY_NAME, "pass");
  }
}

@Singleton
class SingletonProvider implements CredentialsProvider { // Noncompliant
//    ^^^^^^^^^^^^^^^^^

  @Override
  public Map<String, String> getCredentials(String credentialsProviderName) {
    return Map.of(USER_PROPERTY_NAME, "admin", PASSWORD_PROPERTY_NAME, "secret");
  }
}

@Dependent
class DependentProvider implements CredentialsProvider { // Noncompliant
//    ^^^^^^^^^^^^^^^^^

  @Override
  public Map<String, String> getCredentials(String credentialsProviderName) {
    return Map.of(USER_PROPERTY_NAME, "user", PASSWORD_PROPERTY_NAME, "pwd");
  }
}

@SessionScoped
class SessionScopedProvider implements CredentialsProvider { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^

  @Override
  public Map<String, String> getCredentials(String credentialsProviderName) {
    return Map.of(USER_PROPERTY_NAME, "user", PASSWORD_PROPERTY_NAME, "pass");
  }
}

class NoScopeProvider implements CredentialsProvider {

  @Override
  public Map<String, String> getCredentials(String credentialsProviderName) {
    return Map.of(USER_PROPERTY_NAME, "user", PASSWORD_PROPERTY_NAME, "password");
  }
}

@ApplicationScoped
class SomeOtherService {
  public void doSomething() {
  }
}

@Singleton
@Unremovable
class CompliantSingleton implements CredentialsProvider {

  @Override
  public Map<String, String> getCredentials(String credentialsProviderName) {
    return Map.of(USER_PROPERTY_NAME, "admin", PASSWORD_PROPERTY_NAME, "secret");
  }
}

@ApplicationScoped
class ProviderWithDependencies implements CredentialsProvider { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^^^^

  @Inject
  ConfigService configService;

  @Override
  public Map<String, String> getCredentials(String credentialsProviderName) {
    String username = configService.getUsername();
    String password = configService.getPassword();
    return Map.of(USER_PROPERTY_NAME, username, PASSWORD_PROPERTY_NAME, password);
  }
}

@ApplicationScoped
class MultiInterfaceProvider implements CredentialsProvider, AutoCloseable { // Noncompliant
//    ^^^^^^^^^^^^^^^^^^^^^^

  @Override
  public Map<String, String> getCredentials(String credentialsProviderName) {
    return Map.of(USER_PROPERTY_NAME, "user", PASSWORD_PROPERTY_NAME, "password");
  }

  @Override
  public void close() {
  }
}

class ConfigService {
  String getUsername() {
    return "configUser";
  }

  String getPassword() {
    return "configPass";
  }
}
