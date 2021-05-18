package checks.spring;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class SpringIncompatibleTransactionalCheckInvalidPropagation {

  @Transactional(propagation = null)
  public void methodA() {
    methodB();
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodB() {
  }

}

class SpringIncompatibleTransactionalCheckAbstractClass {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void methodA() {
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodB() {
    methodA(); // Noncompliant
  }

}

