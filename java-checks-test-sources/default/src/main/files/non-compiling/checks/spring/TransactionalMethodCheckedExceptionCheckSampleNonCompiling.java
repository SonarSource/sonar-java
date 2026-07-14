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
}
