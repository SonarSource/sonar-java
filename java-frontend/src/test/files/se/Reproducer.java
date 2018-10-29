package javax.annotation;

@interface CheckForNull {}

@interface Nonnull {}

@interface Nullable {}

class A {
 public void testCheckNotNull2(@CheckForNull Object parameter) {
    long remainingNanos = 0;
    final long endNanos = remainingNanos > 0 ? System.nanoTime() + remainingNanos : 0; // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
    label :
    do{
      if(remainingNanos <0 ){ // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
        break label;
      }
    } while ( remainingNanos >0); // Noncompliant {{Remove this expression which always evaluates to "false"}}
  }
  public void testCheckNotNull(@CheckForNull Object parameter) {
    int i;
    Object o;

    Object[] array1 = checkForNullField;
    i = array1.length; // False negative

    i = checkForNullField.length; // False negative, instance and static fields are not checked

    Object[] array2 = checkForNullMethod();
    i = array2.length; // Noncompliant {{A "NullPointerException" could be thrown; "array2" is nullable here.}}
  }

  void testArrayAccess() {
    Object[] foo = new Object[10];
    if (foo[0] == null) {

    }
  }

  @CheckForNull
  public Object[] checkForNullMethod() {
    return null;
  }

  private boolean shouldClosePendingTags(CharactersReader charactersReader) {
    return charactersReader.getCurrentValue() == CR_END_OF_LINE
        || (charactersReader.getCurrentValue() == LF_END_OF_LINE && charactersReader.getPreviousValue() != CR_END_OF_LINE)
        || (charactersReader.getCurrentValue() == CharactersReader.END_OF_STREAM && charactersReader.getPreviousValue() != LF_END_OF_LINE);
  }

  private static final char CR_END_OF_LINE = 'A';
  private static final char LF_END_OF_LINE = 'B';

  public static interface CharactersReader {
    char END_OF_STREAM = 'C';
    char getCurrentValue();
    char getPreviousValue();
  }

  private void foo(boolean b) {
    boolean plop = bar() || b;
    printState();
    if(plop) {
      printState();

    }
    printState();
  }

  public void continue_foreach(boolean a, boolean b, Map<String, String> map) {
    for (String prop : map.keySet()) {
      if (b) {
        continue;
      }
      String.format("  - %s=%s", prop, a ? "******" : "");
    }
  }

  private void increment(int index, int index2) {
    int start = index;
    index++;
    if(start == index) {

    }
    start = index2;
    if(start == index2++) { // Noncompliant
    }
  }

  private boolean sizesDontMatch(boolean bool, boolean a, boolean b) {
    return (!bool && a) || (bool && b);
  }

  private static void zip(Object dir, String s) throws IOException {
    for (String n : dir.list(barqix() ? "**" : "")) {
      if (s.isEmpty()) {
        relativePath = n;
      }
    }
  }

  private void try_finally() {
    boolean success = false;
    try {
      foo();
      success = true;
    } finally {
      if(success) {

      }
    }
  }

  void foo() {
    Object object2;
    try{
      object2 = potentiallyRaiseException();
    } finally {
      System.out.println("foo");
    }
    object2.toString(); // not accessible with null value
  }

  void assignUnknownSymbol() {
    unknown |= false;
  }
  
  void testDoWhile() {
    Object check = null;
    do {
      
    } while (check == null);  // Noncompliant
  }

  public void exitPathWithBranch(boolean fooCalled) {
    Object bar;
    try {
      bar = new Bar();
    } finally {
      if (fooCalled) {
        foo();
      }
    }
    bar.toString();
  }


    List<String> sList;
    List<String> sList2;

    public void SONARJAVA_1531(boolean a, boolean b, boolean c, boolean d) {
      try {
        for (String s : sList) {
          System.out.println("");
          try {
            if (a) {

            }
          } finally {

          }
        }
      } finally {
        for (String s2 : sList2) {

        }
      }
    }

  void foo(int i, int j, int k) {
    switch (i==-1 ? j:k) {
      default:;
    }
  }

  boolean foo(boolean foo) {
    boolean identifier = true;
    return (boolean) !identifier && foo; // Noncompliant {{Remove this expression which always evaluates to "false"}}
  }
}

final class B {
  Object foo(Object a) {
    if(a ==  null) {
      return null;
    }
    return a;
  }

  void bar(Object p) {
    // check that concatenating a string literal to an object gives a non null SV.
    Object b = foo("fpp"+p);
    b.toString();
  }
}

final class C {
  private void bar(Object u) {
    badRequestIfNullResult(u, "", "");
    return u.toString();
  }

  private Object badRequestIfNullResult(@Nullable Object component, String objectType, String objectKey) {
    if (component == null) {
      throw new IllegalArgumentException(String.format(NOT_FOUND_FORMAT, objectType, objectKey));
    }
    return component;
  }

}

abstract class S3655noInterruption {

  public void foo(boolean b) {
    if (bar(b)) {
      doSomething(42);
    }
  }

  private boolean bar(boolean b) {
    if (b) {
      java.util.Optional<Object> value = getValue();
      doSomething(value.get()); // Noncompliant {{Call "value.isPresent()" before accessing the value.}}
      return true;
    }
    return false;
  }

  public abstract void doSomething(Object o);
  public abstract java.util.Optional<Object> getValue();
}

abstract class FilteringOptionalImpactSymbolicValues {


  private boolean bar() {
    if (java.util.Optional.of("abc").isPresent()) { // Noncompliant {{Change this condition so that it does not always evaluate to "true"}}
      return true;
    } else {
      return false;
    }
  }

  private boolean foo(String x) {
    if (!java.util.Optional.of(x).filter(s -> s.startsWith("a")).isPresent()) { // Ok - not always true
      return true;
    }

    java.util.Optional op = java.util.Optional.empty();
    if (op.isPresent()) { // Noncompliant {{Change this condition so that it does not always evaluate to "false"}}
      return true;
    }

    return false;
  }

  public void foobar(@Nullable String x) {
    java.util.Optional<String> op = java.util.Optional.ofNullable(x).filter(s -> s.startsWith("a"));
    if (!op.isPresent()) {
      doSomething("");
    }
  }

  public abstract void doSomething(Object o);
}
