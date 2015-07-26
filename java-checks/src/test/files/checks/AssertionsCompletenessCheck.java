import org.fest.assertions.BooleanAssert;
import org.junit.Test;
import org.mockito.Mockito;
import org.fest.assertions.Assertions;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

class AssertionsCompleteness {

  @Test
  public void noncompliant() {
    // Fest
    org.fest.assertions.Assertions.assertThat(false).as("foo"); // Noncompliant {{Complete the assertion.}}
    org.fest.assertions.Assertions.assertThat(false); // Noncompliant
    Assertions.assertThat(false); // Noncompliant

    // Mockito
    List mockedList = Mockito.mock(List.class);
    mockedList.add("one");
    mockedList.clear();
    Mockito.verify(mockedList); // Noncompliant
  }

  @Test
  public void compliant() {
    org.fest.assertions.Assertions.assertThat(true).isTrue(); // Compliant
    assertThat(AssertionsCompleteness.toString()).hasSize(0); // Compliant
    assertThat(AssertionsCompleteness.toString()).as("aa").hasSize(0); // Compliant

    // junit
    assertThat(3, is(3));

    // Mockito
    List mockedList = Mockito.mock(List.class);
    mockedList.add("one");
    mockedList.clear();
    Mockito.verify(mockedList).add("one"); // Compliant
    Mockito.verify(mockedList).clear(); // Compliant
  }

  private BooleanAssert check(String filename, String key) {
    // Compliant, no issue is raised for return statements and variable assignments to allow helper methods
    BooleanAssert result = org.fest.assertions.Assertions.assertThat(filename.contains(key));
    return org.fest.assertions.Assertions.assertThat(filename.contains(key));
  }

  @Test
  public void test() {
    check("foo.txt", "key1").isTrue();
    check("bar.txt", "key2").isTrue();
  }
}
