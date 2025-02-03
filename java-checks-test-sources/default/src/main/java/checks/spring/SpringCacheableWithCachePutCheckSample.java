package checks.spring;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

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
  class CacheableClass {

    @CachePut
    public void foo() { // Noncompliant {{Methods of a @Cacheable class should not be annotated with "@CachePut".}}
    }

  }

  @CachePut
  class CachePutClass {

    @Cacheable
    public void foo() { // Noncompliant {{Methods of a @CachePut class should not be annotated with "@Cacheable".}}
    }

  }

  class NoCacheAnnotationClass {

    @Cacheable
    public void foo() {
    }

    @CachePut
    public void bar() {
    }

  }

  @Cacheable
  class OkCacheableClass {

  }

  @CachePut
  class OkCachePutClass {

  }

}
