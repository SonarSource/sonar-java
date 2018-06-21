package files.checks.spring;

import javax.transaction.Transactional.TxType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static javax.transaction.Transactional.TxType.REQUIRED;

public class CheckMessage {

  CheckMessage other;

  @Transactional
  public void springTransactionalDefault() {
  }

  public void nonTransactional() {
    springTransactionalDefault();     // Noncompliant [[sc=5;ec=31;secondary=14]] {{"springTransactionalDefault's" @Transactional requirement is incompatible with the one for this method.}}

    other.springTransactionalDefault();
    getOther().springTransactionalDefault();
    this.springTransactionalDefault();  // Noncompliant

    equals(other);
    this.equals(other);
  }

  CheckMessage getOther() {
    return other;
  }

}

public class IncompatibilityMatrix {

  public void nonTransactional() {
    nonTransactional();
    transactional();    // Noncompliant
    mandatory();        // Noncompliant
    nested();           // Noncompliant
    never();
    notSupported();
    required();         // Noncompliant
    requiresNew();      // Noncompliant
    supports();
  }

  @Transactional
  public void transactional() {
    nonTransactional();
    transactional();
    mandatory();
    nested();       // Noncompliant
    never();        // Noncompliant
    notSupported(); // Noncompliant
    required();
    requiresNew();  // Noncompliant
    supports();
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void mandatory() {
    nonTransactional();
    transactional();
    mandatory();
    nested();       // Noncompliant
    never();        // Noncompliant
    notSupported(); // Noncompliant
    required();
    requiresNew();  // Noncompliant
    supports();
  }

  @Transactional(propagation = Propagation.NESTED)
  public void nested() {
    nonTransactional();
    transactional();
    mandatory();
    nested();       // Noncompliant
    never();        // Noncompliant
    notSupported(); // Noncompliant
    required();
    requiresNew();  // Noncompliant
    supports();
  }

  @Transactional(propagation = Propagation.NEVER)
  public void never() {
    nonTransactional();
    transactional(); // Noncompliant
    mandatory();     // Noncompliant
    nested();        // Noncompliant
    never();
    notSupported();
    required();      // Noncompliant
    requiresNew();   // Noncompliant
    supports();
  }

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void notSupported() {
    nonTransactional();
    transactional(); // Noncompliant
    mandatory();     // Noncompliant
    nested();        // Noncompliant
    never();
    notSupported();
    required();      // Noncompliant
    requiresNew();   // Noncompliant
    supports();
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void required() {
    nonTransactional();
    transactional();
    mandatory();
    nested();       // Noncompliant
    never();        // Noncompliant
    notSupported(); // Noncompliant
    required();
    requiresNew();  // Noncompliant
    supports();
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void requiresNew() {
    nonTransactional();
    transactional();
    mandatory();
    nested();       // Noncompliant
    never();        // Noncompliant
    notSupported(); // Noncompliant
    required();
    requiresNew();  // Noncompliant
    supports();
  }

  @Transactional(propagation = Propagation.SUPPORTS)
  public void supports() {
    nonTransactional();
    transactional(); // Noncompliant
    mandatory();     // Noncompliant
    nested();        // Noncompliant
    never();         // Noncompliant
    notSupported();  // Noncompliant
    required();      // Noncompliant
    requiresNew();   // Noncompliant
    supports();
  }

}

public class SupportJavaxTransactional {

  public void nonTransactional() {
    javaxTransactionalDefault();      // Noncompliant
    javaxTransactionalRequired();     // Noncompliant
    javaxTransactionalNotSupported();
    javaxTransactionalNever();
  }

  @javax.transaction.Transactional
  public void javaxTransactionalDefault() {
  }

  @javax.transaction.Transactional(value = REQUIRED)
  public void javaxTransactionalRequired() {
  }

  @javax.transaction.Transactional(TxType.NOT_SUPPORTED)
  public void javaxTransactionalNotSupported() {
  }

  @javax.transaction.Transactional(value = TxType.NEVER)
  public void javaxTransactionalNever() {
  }

}

@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ClassInheritance {

  public void nonTransactional() {
    transactional();
  }

  @Transactional // inherits NOT_SUPPORTED
  public void transactional() {
  }

}

public class ClassWithoutTransactionnal {

  public void method() {
  }

}

public class ClassWithOneTransactionnal {

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodA() {
  }

}

public class ClassWithSameTransactionnal {

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodA() {
    methodB();
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodB() {
  }

}

public abstract class AbstractClass {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public abstract void methodA();

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodB() {
    methodA(); // Noncompliant
  }

}

public class InvalidPropagation {

  @Transactional(propagation = null)
  public void methodA() {
    methodB();
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodB() {
  }

}
