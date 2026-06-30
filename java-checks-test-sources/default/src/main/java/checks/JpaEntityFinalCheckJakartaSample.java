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
