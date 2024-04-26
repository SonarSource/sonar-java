package checks;

import java.util.Enumeration;
import java.util.Iterator;

class S1150_A implements S1150_Foo,
                   Enumeration, // Noncompliant {{Implement Iterator rather than Enumeration.}}
//                 ^^^^^^^^^^^
                   Iterable {

  @Override public Iterator iterator() { return null; }
  @Override public boolean hasMoreElements() { return false; }
  @Override public Object nextElement() { return null; }
}

class S1150_B implements S1150_Foo {  // Compliant
  @Override public boolean hasMoreElements() { return false; }
  @Override public Object nextElement() { return null; }
}

class S1150_C {} // Compliant

enum S1150_D implements Enumeration<Integer> { // Noncompliant
  ;

  @Override public boolean hasMoreElements() { return false; }
  @Override public Integer nextElement() { return null; }
}

class S1150_E implements java.util.Enumeration { // Compliant - limitation
  @Override public boolean hasMoreElements() { return false; }
  @Override public Object nextElement() { return null; }
}

class S1150_F implements java.util.function.Function<String, String> {
  @Override public String apply(String t) { return null; }
}

interface S1150_Foo extends Enumeration {} // Noncompliant
