package checks;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@Entity
class AllFieldAnnotationsWS { // Compliant - all annotations on fields
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
class AllGetterAnnotationsWS { // Compliant - all annotations on getters
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

// Not a persistence entity - mixed annotations should not be flagged
class NotAnEntityWS {
  @Id
  private Long id;

  @Column(name = "name")
  public String getName() {
    return null;
  }
}

@Entity
class NoAnnotationsWS { // Compliant - no persistence annotations on members
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
class OnlyFieldAnnotationWS { // Compliant - only field annotated, no getter annotated
  @Id
  private Long id;

  public Long getId() {
    return id;
  }
}

@Entity
class OnlyGetterAnnotationWS { // Compliant - only getter annotated, no field annotated
  private Long id;

  @Id
  public Long getId() {
    return id;
  }
}

@javax.persistence.Entity
class NoncompliantJavaxEntityWS { // Noncompliant
  @javax.persistence.Id
  private Long id;

  @javax.persistence.Column(name = "name")
  public String getName() {
    return null;
  }
}

@javax.persistence.Entity
class CompliantJavaxEntityWS { // Compliant - all annotations on fields
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
class AllMixedMembersHaveAccessWS { // Compliant - only the @Access-annotated getter is mixed
  @Id
  private Long id;

  @Access(AccessType.PROPERTY)
  @Column(name = "name")
  public String getName() {
    return null;
  }
}

@Entity
class FieldOverrideWithAccessWS { // Compliant - only the @Access-annotated field is mixed
  @Access(AccessType.FIELD)
  @Id
  private Long id;

  @Column(name = "name")
  public String getName() {
    return null;
  }
}

