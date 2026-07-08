package checks;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Stateful;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.xml.ws.WebServiceRef;

@Stateless
class StatelessWithMutableFields {
  private int requestCount;           // Noncompliant {{Remove this mutable instance field or replace it with a local variable, a "static final" constant, or an injected resource.}}
  //          ^^^^^^^^^^^^
  private String lastClientId;        // Noncompliant
  private Object cachedResult = null; // Noncompliant
  public int publicField;             // Noncompliant
  protected int protectedField;       // Noncompliant
}

@Stateless
class StatelessWithSafeFields {
  private static final int MAX_RETRIES = 3; // Compliant - static final constant
  private static int sharedCounter;          // Compliant - static field
  private final String name = "service";     // Compliant - final field

  @EJB
  private StatelessWithSafeFields ejbRef;    // Compliant - injected EJB

  @Inject
  private Object cdiBean;                    // Compliant - CDI injection

  @PersistenceContext
  private EntityManager em;                  // Compliant - injected persistence context

  @PersistenceUnit
  private EntityManagerFactory emf;          // Compliant - injected persistence unit

  @Resource
  private javax.sql.DataSource dataSource;   // Compliant - injected resource

  @WebServiceRef
  private Object wsRef;                      // Compliant - injected web service reference
}

@Stateful
class StatefulWithMutableFields {
  private int requestCount; // Compliant - @Stateful beans are designed to hold state
  private String lastClientId;
}

@Singleton
class SingletonWithMutableFields {
  private int requestCount; // Compliant - not a @Stateless bean
}

class PlainClassWithMutableFields {
  private int requestCount; // Compliant - not an EJB at all
}
