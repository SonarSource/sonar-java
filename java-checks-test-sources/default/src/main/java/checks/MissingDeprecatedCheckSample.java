package checks;

import java.lang.reflect.Method;
import org.junit.Ignore;

class MissingDeprecatedCheckSample {

  @Deprecated
  public int foo; // Noncompliant {{Add the missing @deprecated Javadoc tag.}}
//           ^^^

  public void foo1() {
  }

  @Deprecated
  public void foo2() { // Noncompliant
  }

  /**
   * @deprecated
   */
  public void foo3() { // Noncompliant {{Add the missing @Deprecated annotation.}}

  }

  /**
   * @deprecated
   */
  @Ignore
  @Deprecated
  public void foo4() {
  }

  @Deprecated
  /**
   * @deprecated
   */
  public void foo5() { // Noncompliant
  }

  /*
   * @deprecated
   */
  @Deprecated
  public int foo7() { // Noncompliant
    return 42;
  }

  /**
   *
   */
  @Deprecated
  public void foo8() { // Noncompliant
  }

  @java.lang.Deprecated
  public void foo9() {

    @Deprecated
    int local1 = 0;

  }

  /**
   * @deprecated
   */
  /**
   * Unrelated javadoc
   */
  @Deprecated
  public int foo10() { // Noncompliant
    return 42;
  }

  /**
   * Unrelated javadoc (can be copyright for example)
   */
  /**
   * @deprecated
   */
  @Deprecated
  public int foo11() { // Compliant
    return 42;
  }

}

interface MissingDeprecatedCheckSample_Bar {

  @Deprecated
  int foo(); // Noncompliant

}

/**
* @deprecated
*/
class MissingDeprecatedCheckSample_Qix  { // Noncompliant

  /**
   * @deprecated
   */
  public void foo() {}

  public void foo1() {}

  @Deprecated
  public void foo2() {}
}

@Deprecated
interface Plop { // Noncompliant
  /**
   * @deprecated
   */
  public void foo();

}
interface mockito {
  /**
   * ...
   *
   * @deprecated Use {@link EasyMock#createMockBuilder(Class)} instead
   */
  @Deprecated
  <T> T createMock(Class<T> toMock, Method... mockedMethods);
}
