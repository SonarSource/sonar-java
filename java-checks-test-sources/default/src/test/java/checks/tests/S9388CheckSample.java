package checks.tests;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import org.testng.annotations.DataProvider;

class S9388CheckSample {

  @DataProvider
  public Object[][] validTwoDimensionalArray() { // Compliant
    return new Object[][] {{"a", 1}, {"b", 2}};
  }

  @DataProvider
  public Iterator<Object[]> validIterator() { // Compliant
    return Arrays.asList(new Object[] {"a"}, new Object[] {"b"}).iterator();
  }

  @DataProvider
  public Object[] validOneDimensionalArray() { // Compliant
    return new Object[] {new Object[] {"a"}, new Object[] {"b"}};
  }

  @DataProvider
  public List<String> returnsList() { // Noncompliant {{Change this return type to "Object[][]", "Iterator<Object[]>", or "Object[]".}}
//       ^^^^^^^^^^^^
    return Arrays.asList("a", "b");
  }

  @DataProvider
  public Stream<String> returnsStream() { // Noncompliant {{Change this return type to "Object[][]", "Iterator<Object[]>", or "Object[]".}}
//       ^^^^^^^^^^^^^^
    return Stream.of("a", "b");
  }

  @DataProvider
  public String[] returnsStringArray() { // Noncompliant {{Change this return type to "Object[][]", "Iterator<Object[]>", or "Object[]".}}
//       ^^^^^^^^
    return new String[] {"a", "b"};
  }

  @DataProvider
  public int[][] returnsIntArray2D() { // Noncompliant {{Change this return type to "Object[][]", "Iterator<Object[]>", or "Object[]".}}
//       ^^^^^^^
    return new int[][] {{1, 2}, {3, 4}};
  }

  @DataProvider
  public String[][] returnsStringArray2D() { // Noncompliant {{Change this return type to "Object[][]", "Iterator<Object[]>", or "Object[]".}}
//       ^^^^^^^^^^
    return new String[][] {{"a"}, {"b"}};
  }

  @DataProvider
  public Iterator<String> returnsIteratorOfString() { // Noncompliant {{Change this return type to "Object[][]", "Iterator<Object[]>", or "Object[]".}}
//       ^^^^^^^^^^^^^^^^
    return Arrays.asList("a", "b").iterator();
  }

  @DataProvider
  public Iterator<Object> returnsIteratorOfObject() { // Noncompliant {{Change this return type to "Object[][]", "Iterator<Object[]>", or "Object[]".}}
//       ^^^^^^^^^^^^^^^^
    return Arrays.<Object>asList("a", "b").iterator();
  }

  @DataProvider
  public Object returnsObject() { // Noncompliant {{Change this return type to "Object[][]", "Iterator<Object[]>", or "Object[]".}}
//       ^^^^^^
    return new Object();
  }

  @DataProvider
  public void returnsVoid() { // Noncompliant {{Change this return type to "Object[][]", "Iterator<Object[]>", or "Object[]".}}
//       ^^^^
  }

  @DataProvider
  public Collection<Object[]> returnsCollection() { // Noncompliant {{Change this return type to "Object[][]", "Iterator<Object[]>", or "Object[]".}}
//       ^^^^^^^^^^^^^^^^^^^^
    return Arrays.asList(new Object[] {"a"});
  }

  @DataProvider
  public List<Object[]> returnsListOfObjectArray() { // Noncompliant {{Change this return type to "Object[][]", "Iterator<Object[]>", or "Object[]".}}
//       ^^^^^^^^^^^^^^
    return Arrays.asList(new Object[] {"a"});
  }

  public List<String> notAnnotated() { // Compliant - no @DataProvider
    return Arrays.asList("a", "b");
  }

  public String[] notAnnotatedArray() { // Compliant - no @DataProvider
    return new String[] {"a", "b"};
  }
}
