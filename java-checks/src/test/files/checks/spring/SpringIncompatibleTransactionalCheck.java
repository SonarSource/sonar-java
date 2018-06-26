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

  @Transactional
  public static void unexpectedStaticMethodAnnotatedWithTransactional() {
  }

  public void nonTransactional() {
    springTransactionalDefault();     // Noncompliant [[sc=5;ec=31;secondary=14]] {{"springTransactionalDefault's" @Transactional requirement is incompatible with the one for this method.}}
    unexpectedStaticMethodAnnotatedWithTransactional(); // ignore static methods, Spring does not support @Transactional on static methods.

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
public class ClassInheritanceNotSupported {

  public void nonTransactional() {
    transactional();
  }

  @Transactional // inherits NOT_SUPPORTED
  public void transactional() {
  }

}

@Transactional(propagation = Propagation.REQUIRED)
class ClassInheritanceRequired {

  // even if not anotated with @Transactional, this "public" method inherit form the class transactional propagation.
  public void methodA() {
    methodB();
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodB() {

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

public class ComplexMethodInvocation {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String methodA() {
    return "";
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public int methodB() {
    methodA().length(); // Noncompliant
  }

}

public interface BaseInterface {

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String methodA();

}

public class DerivedClass implements BaseInterface {

  // Knonwn limitation, Spring also look at the "interface that the invoked method has been called through" to determine
  // the "propagation" value, and this rule ignore super classes/interfaces.
  // @see <a href="https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/transaction/interceptor/AbstractFallbackTransactionAttributeSource.html">AbstractFallbackTransactionAttributeSource.html</a>
  @Override
  public String methodA() {
    return "";
  }

  @Transactional(propagation = Propagation.REQUIRES)
  public int methodB() {
    methodA(); // false-negative, see above
  }

}

public class IntermediatePrivateMethodA {

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodA() {
    intermediatePrivateMethod();
  }

  private void intermediatePrivateMethod() {
    methodB();
  }

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodB() {

  }

}

class IntermediatePrivateMethodFalseNegative {

  @Transactional(propagation = Propagation.REQUIRED)
  public void methodA() {
    intermediatePrivateMethod(); // false-negative, limitation, the rule only at the first level of the call hierachy
  }

  private void intermediatePrivateMethod() {
    methodB(); // no issue here because the private method can't be called from outside this class.
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void methodB() {

  }

}
