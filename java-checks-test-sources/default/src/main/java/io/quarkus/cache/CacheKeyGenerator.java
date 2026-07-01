package io.quarkus.cache;

import java.lang.reflect.Method;

/**
 * Mock-up interface for Quarkus CacheKeyGenerator for testing purposes
 */
public interface CacheKeyGenerator {
  Object generate(Method method, Object... methodParams);
}
