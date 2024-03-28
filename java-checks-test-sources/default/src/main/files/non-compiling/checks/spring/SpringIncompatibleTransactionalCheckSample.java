package checks.spring;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

class SpringIncompatibleTransactionalCheckSampleAbstractClass {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void methodA() {
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodB() {
    methodA(); // Noncompliant
  }

}

/**
 * Unknown type behavior:
 * We report an issue when a symbol is in the "methodsPropagationMap", and the propagation is incompatible with the one from the current method.
 * It means that we have to make sure a method dealing with unknown types (annotations, ...) is not added to the map. (The same way we do not add private method)
 */
class SpringIncompatibleTransactionalCheckSampleUnknownTypes {

  @Transactional(propagation = Propagation.NEVER)
  public void methodA() {
    methodB(); // Compliant, methodB is not REQUIRED
    methodC(); // Compliant, not REQUIRED
    methodD(); // Compliant, annotated with something unknown
    unknownMethod(); // Compliant

    methodE(); // Noncompliant
  }

  @Transactional(propagation = Propagation.UNKNOWN)
  public void methodB() {

  }

  @Unknown(propagation = Propagation.REQUIRED)
  public void methodC() {

  }

  @Transactional(propagation = Propagation.UNKNOWN)
  @Unknown
  public void methodD() {

  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodE() {

  }

}
@Transactional(propagation = Propagation.UNKNOWN)
class SpringIncompatibleTransactionalCheckSampleClassAnnotatedUnknown {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void methodA() {
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodB() {
    methodA(); // Compliant, class annotation is unknown
  }

}

class SpringIncompatibleTransactionalCheckSampleSupportJavaxTransactional {

  public void nonTransactional() {
    javaxTransactionalDefault();      // Noncompliant
    javaxTransactionalRequired();     // Compliant, REQUIRED has unknown type
    javaxTransactionalNotSupported();
    javaxTransactionalNever();
  }

  @javax.transaction.Transactional
  public void javaxTransactionalDefault() {
  }

  @javax.transaction.Transactional(value = REQUIRED)
  public void javaxTransactionalRequired() {
  }

  @javax.transaction.Transactional(UnknownType.NOT_SUPPORTED)
  public void javaxTransactionalNotSupported() {
  }

  @javax.transaction.Transactional(value = UnknownType.NEVER)
  public void javaxTransactionalNever() {
  }

}

