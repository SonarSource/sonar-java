import javax.annotation.meta.When;
import org.jspecify.annotations.NullMarked;

@NullMarked
class RedundantNullabilityNoPackage {

  public void methodNonNullParam(@javax.annotation.Nonnull(when= When.ALWAYS) Object o) { // Noncompliant {{Remove redundant annotation @Nonnull(when=ALWAYS) as inside scope annotation @NullMarked at class level.}}
    // ...
  }

  @javax.annotation.Nonnull(when= When.ALWAYS) // Noncompliant {{Remove redundant annotation @Nonnull(when=ALWAYS) as inside scope annotation @NullMarked at class level.}}
  public Integer methodNonNullReturn(Object o) {
    return 0;
  }

  public void methodOkay(Object o) { // Compliant
    // ...
  }

  @NullMarked // Noncompliant {{Remove redundant annotation @NullMarked at class level as inside scope annotation @NullMarked at class level.}}
  static class InnerClass {

    public void methodOkay(Object o) { // Compliant
      // ...
    }

  }

}

enum TEST_COVERAGE {
  ABACUS,
  BABA,
  CIRCUS
}
