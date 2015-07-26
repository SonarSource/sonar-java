package javax.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@interface CheckForNull {}

@interface Nonnull {}

@interface Nullable {}

class A {

  {
    return;
  }

  public A() {
    return null;        // Compliant
  }

  public void f1() {     // Compliant
    return;
  }

  public int[] f2() {
    return null;        // Non-Compliant
    return a;           // Compliant
  }

  public Object f3() {
    return null;        // Compliant
  }

  public Object f4()[] {
    return null;        // Non-Compliant
  }

  public int[] f5() {
    new B() {
      public Object g1() {
        return null;    // Compliant
      }

      public int[] g2() {
        return null;    // Non-Compliant
      }
    };

    return new int[0];  // Compliant
    return null;        // Non-Compliant
  }

  public List f6() {
    return null;        // Non-Compliant
  }

  public ArrayList f7() {
    return null;        // Non-Compliant
  }

  public Set<Integer> f8() {
    return Collections.EMPTY_SET;
    return null;        // Non-Compliant
  }

  public <T> List<Integer>[] f9() {
    return null;        // Non-Compliant
  }

  public java.util.Collection f10() {
    return null;        // Non-Compliant
  }

  public int f11() {
    return null;        // Compliant
  }
}

interface B{
  default int[] a(){
    return null; // Noncompliant
  }

  default int[] b(){
    return new int[4];
  }

  default List<String> c(){
    return null; // Noncompliant
  }

  default List<String> d(){
    return new ArrayList<String>();
  }

  default <T> int[] e(){
    return null; // Noncompliant
  }

  default <T> int[] f(){
    return new int[4];
  }
}

class C {
  @Other
  public int[] gul() {
    return null;  // Noncompliant
  }

  @Nullable
  public Object foo() {
    return null; // Compliant
  }
  
  @javax.annotation.CheckForNull
  public Object bar() {
    return null; // Compliant
  }
  
  @javax.annotation.Nullable
  public int[] fool() {
    return null; // Compliant
  }
  
  @CheckForNull
  public int[] bark() {
    return null; // Compliant
  }

  int[] qix(){
    plop(a -> {
      return null;
    });
  }

  static final Object CONSTANT = plop(a->{return null;});



}