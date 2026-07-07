package checks;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Stateful;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.xml.ws.WebServiceRef;

@Stateless
class StatelessWithMutableFieldsJakarta {
  private int requestCount;           // Noncompliant {{Remove this mutable instance field or replace it with a local variable, a "static final" constant, or an injected resource.}}
  private String lastClientId;        // Noncompliant
  private Object cachedResult = null; // Noncompliant
}

@Stateless
class StatelessWithSafeFieldsJakarta {
  private static final int MAX_RETRIES = 3; // Compliant - static final constant
  private static int sharedCounter;          // Compliant - static field
  private final String name = "service";     // Compliant - final field

  @EJB
  private StatelessWithSafeFieldsJakarta ejbRef; // Compliant - injected EJB

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
class StatefulWithMutableFieldsJakarta {
  private int requestCount; // Compliant - @Stateful beans are designed to hold state
  private String lastClientId;
}

@Singleton
class SingletonWithMutableFieldsJakarta {
  private int requestCount; // Compliant - not a @Stateless bean
}
