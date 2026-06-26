package checks;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@Entity
class PersistenceAnnotationsMixedCheckSample { // Noncompliant {{Annotate either fields or getters for persistence, but not both.}}
  @Id
  private Long id;
  private String name;

  public Long getId() {
    return id;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }
}

@Entity
class AllFieldAnnotations { // Compliant - all annotations on fields
  @Id
  private Long id;
  @Column(name = "name")
  private String name;

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}

@Entity
class AllGetterAnnotations { // Compliant - all annotations on getters
  private Long id;
  private String name;

  @Id
  public Long getId() {
    return id;
  }

  @Column(name = "name")
  public String getName() {
    return name;
  }
}

@Embeddable
class NoncompliantEmbeddable { // Noncompliant
  @Column(name = "street")
  private String street;

  @Column(name = "city")
  public String getCity() {
    return null;
  }
}

@MappedSuperclass
class NoncompliantMappedSuperclass { // Noncompliant
  @Id
  private Long id;

  @Column(name = "type")
  public String getType() {
    return null;
  }
}

// Not a persistence entity - mixed annotations should not be flagged
class NotAnEntity {
  @Id
  private Long id;

  @Column(name = "name")
  public String getName() {
    return null;
  }
}

@Entity
class NoAnnotations { // Compliant - no persistence annotations on members
  private Long id;
  private String name;

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}

@Entity
class OnlyFieldAnnotation { // Compliant - only field annotated, no getter annotated
  @Id
  private Long id;

  public Long getId() {
    return id;
  }
}

@Entity
class OnlyGetterAnnotation { // Compliant - only getter annotated, no field annotated
  private Long id;

  @Id
  public Long getId() {
    return id;
  }
}

// javax variant
@javax.persistence.Entity
class NoncompliantJavaxEntity { // Noncompliant
  @javax.persistence.Id
  private Long id;

  @javax.persistence.Column(name = "name")
  public String getName() {
    return null;
  }
}

@javax.persistence.Entity
class CompliantJavaxEntity { // Compliant - all annotations on fields
  @javax.persistence.Id
  private Long id;
  @javax.persistence.Column(name = "name")
  private String name;

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}

// @Access on the only mixed member: the overriding member is excluded, no remaining mix - compliant
@Entity
class AllMixedMembersHaveAccess { // Compliant - only the @Access-annotated getter is mixed
  @Id
  private Long id;

  @Access(AccessType.PROPERTY)
  @Column(name = "name")
  public String getName() {
    return null;
  }
}

@Entity
class FieldOverrideWithAccess { // Compliant - only the @Access-annotated field is mixed
  @Access(AccessType.FIELD)
  @Id
  private Long id;

  @Column(name = "name")
  public String getName() {
    return null;
  }
}

// @Access only covers one member but another non-overridden member still mixes - noncompliant
@Entity
class PartialAccessOverride { // Noncompliant
  @Id
  private Long id;

  @Access(AccessType.PROPERTY)
  @Column(name = "name")
  public String getName() {
    return null;
  }

  @Column(name = "email")
  public String getEmail() { // wrong: no @Access, should be a field annotation
    return null;
  }
}
