package checks;

import io.quarkus.cache.CacheResult;

public class QuarkusCacheResultOnVoidMethodCheckSample {

  @CacheResult(cacheName = "my-cache")
  public void processData(UnknownType key) { // Noncompliant
  }

  @CacheResult(cacheName = "my-cache")
  public UnknownType getData(String key) { // Compliant - unknown return type, cannot determine if void
  }

  @CacheResult(cacheName = "my-cache")
  public unknownReturnType computeValue(String input) { // Compliant - unknown return type
  }

  public void methodWithoutAnnotation(UnknownType param) { // Compliant
  }
}
