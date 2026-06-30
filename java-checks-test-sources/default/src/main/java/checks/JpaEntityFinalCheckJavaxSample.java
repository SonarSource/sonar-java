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
class JpaEntityFinalCheckJavaxCompliantEntity { // Compliant
  @Id
  private Long id;

  public Long getId() {
    return id;
  }
}
