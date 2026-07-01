package checks;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@Entity
final class JpaEntityFinalCheckJavaxFinalEntity { // Noncompliant {{Remove this "final" modifier from this JPA entity class.}}
//^[sc=1;ec=5]
  @Id
  private Long id;

  public Long getId() {
    return id;
  }
}

@MappedSuperclass
final class JpaEntityFinalCheckJavaxFinalMappedSuperclass { // Noncompliant {{Remove this "final" modifier from this JPA entity class.}}
//^[sc=1;ec=5]
  @Id
  private Long id;
}

@Entity
class JpaEntityFinalCheckJavaxEntityWithFinalMethod { // Compliant - class itself is not final
  @Id
  private Long id;

  public final Long getId() { // Noncompliant {{Remove this "final" modifier from this JPA entity method.}}
  //     ^^^^^
    return id;
  }

  private final Long getIdPrivate() { // Compliant - private methods cannot be overridden by proxies
    return id;
  }

  public static final Long getIdStatic() { // Compliant - static methods cannot be overridden by proxies
    return 0L;
  }
}

@Entity
class JpaEntityFinalCheckJavaxCompliantEntity { // Compliant
  @Id
  private Long id;

  public Long getId() {
    return id;
  }
}
