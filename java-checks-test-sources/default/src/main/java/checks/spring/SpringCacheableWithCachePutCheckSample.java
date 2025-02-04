package checks.spring;

import jakarta.annotation.Nullable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

public class SpringCacheableWithCachePutCheckSample {

  @Cacheable
//^^^^^^^^^^> {{Cacheable}}
  @CachePut
//^^^^^^^^^> {{CachePut}}
  public void foo() { // Noncompliant {{Remove the "@CachePut" annotation or the "@Cacheable" annotation located on the same method.}}
//            ^^^
  }

  @Cacheable
  public void goo() {
  }

  @CachePut
  public void zoo() {
  }

  @Cacheable
//^^^^^^^^^^> {{Cacheable}}
  @CachePut
//^^^^^^^^^> {{CachePut}}
  class CacheableAndCachePutClass { // Noncompliant {{Remove the "@CachePut" annotation or the "@Cacheable" annotation located on the same class.}}
//      ^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  @Cacheable
  @Component
  class CacheableClass {

    @CachePut
    public void foo() { // Noncompliant {{Methods of a @Cacheable type should not be annotated with "@CachePut".}}
    }

  }

  @CachePut
  record Record(String name) {
    @Cacheable
    public String prettyName() { // Noncompliant {{Methods of a @CachePut type should not be annotated with "@Cacheable".}}
      return "~" + name + "~";
    }
  }

  @CachePut
  class CachePutClass {

    @Cacheable
    @Nullable
    public String foo() { // Noncompliant {{Methods of a @CachePut type should not be annotated with "@Cacheable".}}
      return null;
    }

  }

  @Cacheable
  enum CacheableEnum {
    FOO;

    @CachePut
    public CacheableEnum foo() { // Noncompliant {{Methods of a @Cacheable type should not be annotated with "@CachePut".}}
      return FOO;
    }

  }

  @Component
  class NoCacheAnnotationClass {

    @Cacheable
    public void foo() {
    }

    @CachePut
    @Nullable
    public String bar() {
      return null;
    }

  }

  @Cacheable
  class OkCacheableClass {

  }

  @CachePut
  class OkCachePutClass {

  }

}
