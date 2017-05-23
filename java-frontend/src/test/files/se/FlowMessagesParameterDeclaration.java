import javax.annotation.Nullable;

abstract class A {

  private final Object finalFieldNonNull = new Object(); // flow@cof1 [[sc=44;ec=56]] {{Implies 'finalFieldNonNull' is not null.}} flow@cof2 {{Implies 'finalFieldNonNull' is not null.}}
  private final Object finalFieldNull = null; // flow@cot [[sc=41;ec=45]] {{Implies 'finalFieldNull' is null.}}
  private final Object otherFinalFieldNull = null; // flow@npe [[sc=46;ec=50]] {{Implies 'otherFinalFieldNull' is null.}}

  void bar() {
    // Noncompliant@+1 [[flows=cof1]] {{Change this condition so that it does not always evaluate to "false"}}
    if (finalFieldNonNull == null) { // flow@cof1 {{Expression is always false.}}
      doSomething();
    }
    // Noncompliant@+1 [[flows=cot]] {{Remove this expression which always evaluates to "true"}}
    if (finalFieldNull == null) { // flow@cot {{Expression is always true.}}
      doSomething();
    }
    // Noncompliant@+1 [[flows=cof2]] {{Change this condition so that it does not always evaluate to "false"}}
    if (finalFieldNonNull == null) { // flow@cof2 {{Expression is always false.}}
      doSomething();
    }
    // Noncompliant@+1 [[flows=npe]] {{A "NullPointerException" could be thrown; "otherFinalFieldNull" is nullable here.}}
    this.otherFinalFieldNull.toString(); // flow@npe {{'otherFinalFieldNull' is dereferenced.}}
  }

  /**
   * no explicit annotation
   */
  @Override
  public boolean equals(Object obj) { // flow@param [[sc=32;ec=35]] {{Implies 'obj' can be null.}}
    // Noncompliant@+1 [[flows=param]] {{A "NullPointerException" could be thrown; "obj" is nullable here.}}
    obj.toString(); // flow@param {{'obj' is dereferenced.}}
    return true;
  }

  /**
   * explicit annotation
   */
  public void foo(@Nullable Object obj) { // flow@paramWithNullableAnnotation {{Implies 'obj' can be null.}}
    // Noncompliant@+1 [[flows=paramWithNullableAnnotation]] {{A "NullPointerException" could be thrown; "obj" is nullable here.}}
    obj.toString(); // flow@paramWithNullableAnnotation {{'obj' is dereferenced.}}
  }

  /**
   * no nullness annotation, constraint learn on path
   */
  public void bar(@MyAnnotation Object obj) {
    if (obj == null) { // flow@paramWithAnnotation {{Implies 'obj' is null.}}
      // Noncompliant@+1 [[flows=paramWithAnnotation]] {{A "NullPointerException" could be thrown; "obj" is nullable here.}}
      obj.toString(); // flow@paramWithAnnotation {{'obj' is dereferenced.}}
    }
  }

  /**
   * explicit annotation, constraint valided on path
   */
  public void qix(@MyAnnotation @Nullable Object obj) { // flow@paramWithAnnotation2 {{Implies 'obj' can be null.}}
    if (obj == null) {
      // Noncompliant@+1 [[flows=paramWithAnnotation2]] {{A "NullPointerException" could be thrown; "obj" is nullable here.}}
      obj.toString(); // flow@paramWithAnnotation2 {{'obj' is dereferenced.}}
    }
  }

  abstract void doSomething();
  private static @interface MyAnnotation {}
}
