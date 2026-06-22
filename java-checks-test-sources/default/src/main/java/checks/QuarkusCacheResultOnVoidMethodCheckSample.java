package checks;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import java.util.List;
import java.util.Optional;

public class QuarkusCacheResultOnVoidMethodCheckSample {

  @CacheResult(cacheName = "my-cache")
  public void processData(String key) { // Noncompliant {{Methods annotated with "@CacheResult" should not return void.}}
//       ^^^^
  }

  @CacheResult(cacheName = "calc-cache", lockTimeout = 1000)
  public void performCalculation(String input) { // Noncompliant
  }

  @CacheResult(cacheName = "cache")
  void packagePrivateVoid(String data) { // Noncompliant
  }

  @CacheResult(cacheName = "cache")
  protected void protectedVoid() { // Noncompliant
  }

  @CacheResult(cacheName = "cache")
  private void privateVoid(int value) { // Noncompliant
  }

  @CacheResult(cacheName = "my-cache")
  public String getData(String key) { // Compliant
    return "result-" + key;
  }

  @CacheResult(cacheName = "user-cache")
  public User getUser(Long userId) { // Compliant
    return new User();
  }

  @CacheResult(cacheName = "int-cache")
  public Integer calculate(String input) { // Compliant
    return input.length();
  }

  @CacheResult(cacheName = "optional-cache")
  public Optional<String> findValue(String key) { // Compliant
    return Optional.empty();
  }

  @CacheResult(cacheName = "list-cache")
  public List<String> getList(String item) { // Compliant
    return List.of(item);
  }

  @CacheResult(cacheName = "array-cache")
  public String[] getArray() { // Compliant
    return new String[]{"a", "b"};
  }

  public void voidMethodNoAnnotation(String key) { // Compliant
  }

  @CacheInvalidate(cacheName = "my-cache")
  public void invalidateCache(String key) { // Compliant
  }

  @CacheInvalidateAll(cacheName = "my-cache")
  public void clearCache() { // Compliant
  }

  public String methodNoAnnotation(String input) { // Compliant
    return input;
  }

  private static class User {
  }
}
