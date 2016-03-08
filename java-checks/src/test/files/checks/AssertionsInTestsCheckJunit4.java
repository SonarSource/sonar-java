import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.Rule;
import org.fest.assertions.Assertions;

import java.lang.IllegalStateException;
import java.lang.Override;
import java.util.List;
import org.mockito.Mockito;
import org.junit.rules.ExpectedException;
import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AssertionsInTestsCheckTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static int staticMethod() {}

  private static int VAL = staticMethod();

  @Test
  public void compliant1() {
    org.fest.assertions.Fail.fail("foo");
  }

  @Test
  public void compliant2() {
    org.junit.Assert.assertEquals(true, true);
  }

  @Test
  public void compliant3() {
    org.junit.Assert.assertTrue(true);
    org.junit.Assert.assertTrue(true); // Coverage
  }

  @Test
  public void compliant4() {
    org.junit.Assert.assertThat("aaa", org.junit.matchers.JUnitMatchers.containsString("a"));
  }

  @Test
  public void compliant4bis() {
    org.junit.Assert.<String>assertThat("aaa", org.junit.matchers.JUnitMatchers.containsString("a"));
  }

  @Test
  public void compliant5() {
    org.fest.assertions.Assertions.assertThat(true).isEqualTo(true);
  }

  @Test
  public void compliant6() {
    assertThat("").isEmpty();
  }

  @Test
  public void compliant7() {
    assertThat("").hasSize(0);
  }

  @Test
  public void compliant8() {
    assertThat("").as("").isEqualTo("");
  }

  @Test
  public void compliant9() {
    List mockedList = Mockito.mock(List.class);
    verifyNoMoreInteractions(mockedList);
  }

  @Test
  public void compliant10() {
    List mockedList = Mockito.mock(List.class);
    Mockito.verifyNoMoreInteractions(mockedList);
  }

  @Test
  public void compliant11() {
    List mockedList = Mockito.mock(List.class);
    mockedList.add("one");
    verify(mockedList).add("one");
  }

  @Test
  public void compliant12() {
    List mockedList = Mockito.mock(List.class);
    mockedList.clear();
    verify(mockedList).clear();
  }

  @Test
  public void compliant13() {
    thrown.expect(IllegalStateException.class);
    throw new IllegalStateException("message");
  }

  @Test
  public void compliant14() {
    thrown.expectMessage("message");
    throw new IllegalStateException("message");
  }

  @Test(expected = IllegalStateException.class)
  public void compliant15() {
    throw new IllegalStateException("message");
  }

  @Test
  public void nonCompliant1() { // Noncompliant [[sc=15;ec=28]] {{Add at least one assertion to this test case.}}
  }

  @Test
  public void nonCompliant2() { // Noncompliant
    org.fest.assertions.Assertions.assertThat(true);  // Fest assertion stub with no checks
  }

  @Test
  public void nonCompliant3() { // Noncompliant
    assertThat(true);  // Fest assertion stub with no checks
  }

  @Test
  public void nonCompliant4() { // Noncompliant
    org.fest.assertions.Assertions.assertThat(true).as("foo");  // Fest assertion stub with no checks
  }

  @Test
  public void nonCompliant5() { // Noncompliant
    org.fest.assertions.Assertions.assertThat(true).describedAs("foo");  // Fest assertion stub with no checks
  }

  @Test
  public void nonCompliant6() { // Noncompliant
    org.fest.assertions.Assertions.assertThat(true).overridingErrorMessage("foo");  // Fest assertion stub with no checks
  }

  @Test
  public void nonCompliant7() { // Noncompliant
    List mockedList = Mockito.mock(List.class);
    mockedList.add("one");
    mockedList.clear();
    Mockito.verify(mockedList); // verify alone is noncompliant
  }

  @Test
  public void nonCompliant8() { // Noncompliant
    List mockedList = Mockito.mock(List.class);
    mockedList.add("one");
    mockedList.clear();
    verify(mockedList); // verify alone is noncompliant
  }

  @Test(timeout = 0L)
  public void nonCompliant9() { // Noncompliant
    throw new IllegalStateException("message");
  }

  @Nullable
  public Test notAtest() {
    compliant1();
  }

}

abstract class AbstractTest {
  @Test
  public abstract void nonCompliant1();
}

class ImplTest extends AbstractTest {
  @Override
  public void nonCompliant1() { // Noncompliant
    // overridden test
  }
}

class OtherTest extends TestCase {
  @Test
  public void test() {
    assertEquals(true, true);
  }
  @Test
  public void testFail() {
    fail("message");
  }
}
