import javax.annotation.CheckForNull;
import org.assertj.core.api.AbstractObjectAssert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.assertj.core.api.Assertions.assertThat;

abstract class AssertionsInTests {

  @Test
  public void testAssertj() throws Exception {
    Object o = getObject();
    assertThat(o).isNotNull();
    assertThat(o.toString()).isEqualTo("Hello");  // Compliant
  }

  @Test
  public void testAssertj_no_nullness_assertion() throws Exception {
    Object o = getObject();
    assertThat(o.toString()).isEqualTo("Hello");  // Noncompliant
  }

  @Test
  public void testAssertj_chained() throws Exception {
    Object o = getObject();
    assertThat(o).isEqualTo("Hello").isNotNull();
    assertThat(o.toString()); // Noncompliant - FP - not following chain of assert
  }

  @Test
  public void testAssertj_chained_frm_unrelated_method() throws Exception {
    Object o = getObject();
    getAssert(o).isNotNull();
    assertThat(o.toString()); // Noncompliant - FP ? - we do not know what getAssert is doing
  }

  @Test
  public void testAssertj_chained_from_variable() {
    Object o = getObject();
    AbstractObjectAssert equalTo = assertThat(o).isEqualTo("Hello");
    equalTo.isNotNull();
    assertThat(o.toString()).isEqualTo("Hello"); // Noncompliant - FP
  }


  @Test
  public void testJunit() throws Exception {
    Object o = getObject();
    assertNotNull(o);
    assertEquals(o.toString(), "hello"); // Compliant
  }

  @Test
  public void testJunit_no_nullness_assertion() throws Exception {
    Object o = getObject();
    assertEquals(o.toString(), "hello"); // Noncompliant
  }

  @Test
  public void testJunit5() {
    Object o = getObject();
    org.junit.jupiter.api.Assertions.assertNotNull(o);
    o.toString(); // Compliant
  }

  @CheckForNull
  abstract Object getObject();
  abstract AbstractObjectAssert getAssert(Object o);
}
