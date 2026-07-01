package checks;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@Entity
final class JpaEntityFinalCheckJakartaFinalEntity { // Noncompliant {{Remove this "final" modifier from this JPA entity class.}}
//^[sc=1;ec=5]
  @Id
  private Long id;

  public Long getId() {
    return id;
  }
}

@MappedSuperclass
final class JpaEntityFinalCheckJakartaFinalMappedSuperclass { // Noncompliant {{Remove this "final" modifier from this JPA entity class.}}
//^[sc=1;ec=5]
  @Id
  private Long id;
}

@Entity
class JpaEntityFinalCheckJakartaEntityWithFinalMethod { // Compliant - class itself is not final
  @Id
  private Long id;

  public final Long getId() { // Noncompliant {{Remove this "final" modifier from this JPA entity method.}}
  //     ^^^^^
    return id;
  }

  public Long getIdCompliant() { // Compliant
    return id;
  }

  private final Long getIdPrivate() { // Compliant - private methods cannot be overridden by proxies
    return id;
  }

  public static final Long getIdStatic() { // Compliant - static methods cannot be overridden by proxies
    return 0L;
  }
}

@MappedSuperclass
class JpaEntityFinalCheckJakartaMappedSuperclassWithFinalMethod { // Compliant - class itself is not final
  @Id
  private Long id;

  public final Long getId() { // Noncompliant {{Remove this "final" modifier from this JPA entity method.}}
  //     ^^^^^
    return id;
  }
}

@Entity
class JpaEntityFinalCheckJakartaCompliantEntity { // Compliant
  @Id
  private Long id;

  public Long getId() {
    return id;
  }
}

@MappedSuperclass
class JpaEntityFinalCheckJakartaCompliantMappedSuperclass { // Compliant
  @Id
  private Long id;
}

class JpaEntityFinalCheckJakartaNotAnEntity { // Compliant - not a JPA entity
}

final class JpaEntityFinalCheckJakartaFinalNotAnEntity { // Compliant - not a JPA entity
}

class JpaEntityFinalCheckJakartaNotAnEntityWithFinalMethod { // Compliant - not a JPA entity
  public final void doSomething() { // Compliant - not a JPA entity
  }
}
