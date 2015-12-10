package javax.annotation;

@interface CheckForNull {}

@interface Nonnull {}

@interface Nullable {}

class A {
  public void testCheckNotNull2(@CheckForNull Object parameter) {
    long remainingNanos = 0;
    final long endNanos = remainingNanos > 0 ? System.nanoTime() + remainingNanos : 0;
    label :
    do{
      if(remainingNanos <0 ){
        break label;
      }
    } while ( remainingNanos >0);
  }
  public void testCheckNotNull(@CheckForNull Object parameter) {
    int i;
    Object o;

    Object[] array1 = checkForNullField;
    i = array1.length; // False negative

    i = checkForNullField.length; // False negative, instance and static fields are not checked

    Object[] array2 = checkForNullMethod();
    i = array2.length; // Noncompliant {{NullPointerException might be thrown as 'array2' is nullable here}}
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
    for (String n : dir.list(foo() ? "**" : "")) {
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

}