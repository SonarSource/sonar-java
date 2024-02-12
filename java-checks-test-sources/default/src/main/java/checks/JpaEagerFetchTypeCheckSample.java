package checks;

import jakarta.persistence.Basic;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;
import static jakarta.persistence.FetchType.EAGER;

public class JpaEagerFetchTypeCheckSample {
  private enum CustomFetchType {
    EAGER, LAZY
  }

  @Target(ElementType.FIELD)
  private @interface CustomAnnotation {
    FetchType value() default FetchType.LAZY;
    CustomFetchType fetch() default CustomFetchType.LAZY;
  }

  @Basic(fetch = FetchType.EAGER) // Noncompliant [[sc=28;ec=33]] {{Use lazy fetching instead.}}
  private String foo;

  @javax.persistence.Basic(fetch = javax.persistence.FetchType.EAGER) // Noncompliant
  private String foo2;

  @Basic(optional = true, fetch = FetchType.EAGER) // Noncompliant
  private String baz;

  @Basic(fetch = EAGER) // Noncompliant
  private String baz4;

  @OneToMany(fetch = FetchType.EAGER) // Noncompliant
  private List<String> foos;

  @javax.persistence.OneToMany(fetch = javax.persistence.FetchType.EAGER) // Noncompliant
  private List<String> foos2;

  @Basic(fetch = FetchType.LAZY) // Compliant
  private String bar;

  @javax.persistence.Basic(fetch = javax.persistence.FetchType.LAZY) // Compliant
  private String bar2;

  @OneToMany(fetch = FetchType.LAZY) // Compliant
  private List<String> bars;

  @javax.persistence.OneToMany(fetch = javax.persistence.FetchType.LAZY) // Compliant
  private List<String> bars2;

  @Basic // Compliant
  private String baz2;

  @OneToOne // Compliant
  private String baz3;

  @CustomAnnotation(fetch = CustomFetchType.EAGER) // Compliant
  private String custom;

  @CustomAnnotation(EAGER) // Compliant
  private String customNoAssignment;
}
