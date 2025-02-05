package checks.spring;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

public class CacheAnnotationsShouldOnlyBeAppliedToConcreteClassesCheckSample {

  @Cacheable("aCache") // Noncompliant
  public interface InterfaceCacheable {
    @Cacheable("aCache") // Noncompliant {{Move this "@Cacheable" annotation to concrete class.}}
  //^^^^^^^^^^^^^^^^^^^^
    String getData(String id);
  }


  @Cacheable("aCache")
  abstract class AbstractClassCacheable {
    @Cacheable("aCache")
    String getData(String id){return "";}
  }

  @Cacheable("aCache")
  class ClassCacheable {
    @Cacheable("aCache")
    String getData(String id){return "";}
  }


  @Cacheable("aCache")
  record RecordCacheable() {
    @Cacheable("aCache")
    String getData(String id){return "";}
  }

  @Cacheable("aCache")
  enum EnumCacheable {
    FOO;
    @Cacheable("aCache")
    String getData(String id){return "";}
  }

  @Cacheable("aCache")
  @interface MyAnnotation {}

  @CacheConfig // Noncompliant {{Move this "@CacheConfig" annotation to a concrete class.}}
  public interface InterfaceCacheConfig { }

  @CacheConfig
  class ClassCacheConfig { }

  @CachePut("aCache") // Noncompliant {{Move this "@CachePut" annotation to a concrete class.}}
  public interface InterfaceCachePut {
    @CachePut("aCache")
    String getData(String id);
  }

  @CachePut("aCache")
  class ClassCachePut {
    @CachePut("aCache")
    String getData(String id){return "";}
  }

  @CacheEvict("aCache") // Noncompliant {{Move this "@CacheEvict" annotation to a concrete class.}}
  public interface InterfaceCacheEvict {
    @CacheEvict("aCache")
    String getData(String id);
  }

  @CacheEvict("aCache")
  class ClassCacheEvict {
    @CacheEvict("aCache")
    String getData(String id){return "";}
  }

  @Caching // Noncompliant {{Move this "@Caching" annotation to a concrete class.}}
  public interface InterfaceCaching {
    @Caching
    String getData(String id);
  }

  @Caching
  class ClassCaching {
    @Caching
    String getData(String id){return "";}
  }
}
