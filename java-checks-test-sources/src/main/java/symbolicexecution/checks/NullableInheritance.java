package symbolicexecution.checks;

import org.eclipse.jdt.annotation.NonNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

public class NullableInheritance {

  public Long jdbcTemplate() {
    // Method "org.springframework.jdbc.core.JdbcTemplate.queryForObject(String, Class<Long>, Object...)"
    // In spring-jdbc < 5.0.0: Not annotated as Nullable but described as nullable in javadoc
    // In spring-jdbc >= 5.0.0: super-implementation (defined in interface org.springframework.jdbc.core.JdbcOperations), annotated with spring's @Nullable
    final Long a = new JdbcTemplate().queryForObject("a_query", Long.class, new Object[1]);
    return a == null ? Long.MIN_VALUE : a.longValue(); // Compliant - no issue; null-check is required
  }

  private class MultipleLevelOfInheritance {
    interface ILevel1 {
      @Nullable
      Object m();
    }

    interface ILevel2 extends ILevel1 {
      Object m();
    }

    @org.eclipse.jdt.annotation.NonNullByDefault
    class C implements ILevel2 {
      @Override
      public Object m() { // No @Nullable annotation, but should be considered inherited from ILevel1
        return null;
      }
    }

    class Foo {
      int foo() {
        C c = new C();
        Object o = c.m();
        return o == null ? -1 : 42; // Compliant - here the null-check is mandatory
      }
    }
  }

  private class OneUpInInheritanceAndUpOwnership {
    interface ILevel1 {
      @NonNull
      Object m();
    }

    interface ILevel2 extends ILevel1 {
      Object m();
    }

    class C implements ILevel2 {
      @Override
      public Object m() { // No @NonNull annotation, but should be considered inherited from ILevel1
        return null;
      }
    }

    class Foo {
      int foo() {
        C c = new C();
        Object o = c.m();
        return o == null ? -1 : 42; // Noncompliant
      }
    }
  }
}
