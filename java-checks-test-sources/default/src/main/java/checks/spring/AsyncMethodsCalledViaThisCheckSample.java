package checks.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AsyncMethodsCalledViaThisCheckSample {

  @Autowired
  private AsyncMethodsCalledViaThisCheckSample self;

  @Async
  void asyncMethod() {
  }

  @Transactional
  public void transactionalMethod() {
  }

  @Cacheable
  public void cacheableMethod() {
  }

  void normal() {
  }

  void asyncNoncompliant1() {
    asyncMethod(); // Noncompliant {{Call async methods via an injected dependency instead of directly via 'this'.}}
//  ^^^^^^^^^^^^^
  }

  void asyncNoncompliant2() {
    this.asyncMethod(); // Noncompliant
  }

  void asyncCompliant1() {
    self.asyncMethod();
  }

  void transactionalNoncompliant1() {
    transactionalMethod(); // Noncompliant {{Call transactional methods via an injected dependency instead of directly via 'this'.}}
  }

  void transactionalNoncompliant2() {
    this.transactionalMethod(); // Noncompliant
  }

  void transactionalCompliant1() {
    self.transactionalMethod();
  }

  void cacheableNoncompliant1() {
    cacheableMethod();  // Noncompliant {{Call cacheable methods via an injected dependency instead of directly via 'this'.}}
  }

  void cacheableNoncompliant2() {
    this.cacheableMethod();  // Noncompliant {{Call cacheable methods via an injected dependency instead of directly via 'this'.}}
  }

  void cacheableCompliant() {
    self.cacheableMethod();
  }

  void compliant() {
    normal();
  }
}
