package checks.spring;

import org.springframework.transaction.annotation.Transactional;

class TransactionalMethodCheckedExceptionCheckSampleNonCompiling {

  // Test unknown/unresolved exception type
  @Transactional
  public void unknownException() throws UnresolvedCheckedException { // No issue - type is unknown
  }

  @Transactional
  public void knownException() throws java.io.IOException { // Noncompliant [[secondary=12]]
//            ^^^^^^^^^^^^^^
  }

  // Propagation set to an unresolvable expression (not an identifier or member select)
  @Transactional(propagation = getPropagation())
  public void propagationFromMethodCall() throws java.io.IOException { // Noncompliant [[secondary=18]]
//            ^^^^^^^^^^^^^^^^^^^^^^^^^
  }

  @Transactional
  public void unknownThrownTypeAfterWrite(Repository repository) throws java.io.IOException { // Noncompliant [[secondary=24]]
//            ^^^^^^^^^^^^^^^^^^^^^^^^^^^
    repository.save(null);
    throw new UnknownException();
  }

  @Transactional
  public void unknownMethodAfterWrite(Repository repository) throws java.io.IOException { // Noncompliant [[secondary=31]]
//            ^^^^^^^^^^^^^^^^^^^^^^^
    repository.save(null);
    unknownMethod();
  }

  @Transactional
  public void checkedThrowBeforeWrite(Repository repository) throws java.io.IOException { // Compliant
    if (repository == null) {
      throw new java.io.IOException();
    }
    repository.save(null);
  }

  static class Repository {
    void save(Object o) {
    }
  }
}
