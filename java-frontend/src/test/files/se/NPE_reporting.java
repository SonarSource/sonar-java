package javax.annotation;

@interface CheckForNull {}

@interface Nonnull {}

@interface Nullable {}

class A {
  void null_assigned(Object b, Object c) {
    if(b == null);
    if(c == null);
    Object a = null; // flow@fl1 {{a is assigned null}}
    a.toString(); // Noncompliant [[flows=fl1]] flow@fl1 {{a is dereferenced}}
  }

  private Object getA() {
    return null;
  }

  void null_assigned(Object b, Object c) {
    if(b == null);
    if(c == null);
    getA().toString(); // Noncompliant [[flows=mnull]] flow@mnull {{Result of getA() is dereferenced}} flow@mnull {{Uses return value [see L#17].}}
  }

  void reassignement() {
    Object a = null; // flow@reass {{a is assigned null}}
    Object b = new Object();
    b = a; // flow@reass {{b is assigned null}}
    b.toString(); // Noncompliant [[flows=reass]] flow@reass {{b is dereferenced}}
  }

  void relationshipLearning(Object a) {
    if (a == null) { // flow@rela {{...}}
      a.toString(); // Noncompliant [[flows=rela]] flow@rela {{a is dereferenced}}
    }
  }

  void combined(Object a) {
    Object b = new Object();
    if (a == null) { // flow@comb {{...}}
      b = a; // flow@comb {{b is assigned null}}
      b.toString(); // Noncompliant [[flows=comb]] flow@comb {{b is dereferenced}}
    }
  }

  void complexRelation(int a, int b, Object c) {
    if (a < b) { // This should be reported as well to highlight context
      c = null; // flow@cplx {{c is assigned null}}
    }
    System.out.println("");
    if (b > a) { // This should be reported as well to highlight context
      c.toString(); // Noncompliant [[flows=cplx]]  flow@cplx {{c is dereferenced}}
    }
  }

  void recursiveRelation(Object a, Object b) {
    if ((a == null) == true) { // flow@rec {{...}}
      b = a; // flow@rec {{b is assigned null}}
      b.toString(); // Noncompliant [[flows=rec]] flow@rec {{b is dereferenced}}
    }
  }

  void Xproc(boolean a) {
    if (a) {
      Object b = // flow@xproc
        foo(a); // flow@xproc
      b.toString(); // Noncompliant [[flows=xproc]] flow@xproc {{b is dereferenced}}
    }
  }

  private Object foo(boolean a) {
    if (a) {
      return null;
    }
    return new Object();
  }

  class A {}

  public void testMemberSelect(A a1, @CheckForNull A a2, @Nullable A a3) {
    a1.hashCode(); // No issue
    a2.hashCode(); // Noncompliant [[flows=a2]] {{NullPointerException might be thrown as 'a2' is nullable here}} flow@a2 {{a2 is dereferenced}}
    a3.hashCode(); // Noncompliant [[flows=a3]] {{NullPointerException might be thrown as 'a3' is nullable here}} flow@a3 {{a3 is dereferenced}}
  }
}
