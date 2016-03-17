package javax.annotation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@interface CheckForNull {}

@interface Nonnull {}

@interface Nullable {}

class A {

  public A() {
    return null;        
  }

  public void f1() {     
    return;
  }

  public int[] f2() {
    return null;        // Noncompliant [[sc=12;ec=16]] {{Return an empty array instead of null.}}
  }

  public Object f3() {
    return null;        
  }

  public Object f4()[] {
    return null;        // Noncompliant
  }

  public int[] f5() {
    new B() {
      public Object g1() {
        return null;    
      }

      public int[] g2() {
        return null;    // Noncompliant
      }
    };

    return new int[0];  
    return null;        // Noncompliant
  }

  public List f6() {
    return null;        // Noncompliant
  }

  public ArrayList f7() {
    return null;        // Noncompliant
  }

  public Set<Integer> f8() {
    return Collections.EMPTY_SET;
    return null;        // Noncompliant {{Return an empty collection instead of null.}}
  }

  public <T> List<Integer>[] f9() {
    return null;        // Noncompliant {{Return an empty array instead of null.}}
  }

  public java.util.Collection f10() {
    return null;        // Noncompliant
  }

  public int f11() {
    return null;        
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
    return null; 
  }
  
  @javax.annotation.CheckForNull
  public Object bar() {
    return null; 
  }
  
  @javax.annotation.Nullable
  public int[] fool() {
    return null; 
  }
  
  @CheckForNull
  public int[] bark() {
    return null; 
  }

  int[] qix(){
    plop(a -> {
      return null;
    });
  }

  static final Object CONSTANT = plop(a->{return null;});



}
