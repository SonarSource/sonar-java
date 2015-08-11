class A implements MyInterface {
  private static final long var1 = -9215047833775013803L; // Compliant
  public long var2 = 0L;
  
  public static void equAls(Object obj) {} // Noncompliant {{Rename method "equAls" to prevent any misunderstanding/clash with method "equals" defined in superclass "java.lang.Object".}}
  protected void finaliZe() {} // Noncompliant {{Rename method "finaliZe" to prevent any misunderstanding/clash with method "finalize" defined in superclass "java.lang.Object".}}
  
  public void myMethod() {}
  
  public Object stuff;
  public void stuff() {} // Noncompliant {{Rename method "stuff" to prevent any misunderstanding/clash with field "stuff" defined on line 10.}} 
  public void stuff(int i) {} // Noncompliant {{Rename method "stuff" to prevent any misunderstanding/clash with field "stuff" defined on line 10.}} 
  
  public void foo(int i) {} // Compliant
  public void foo(boolean i) {} // Compliant
  
  public void myOtherMethoD() {} // Noncompliant {{Rename method "myOtherMethoD" to prevent any misunderstanding/clash with method "myOtherMethod" defined in interface "MyInterface".}}
  
  private static void gUl() {} // Noncompliant {{Rename method "gUl" to prevent any misunderstanding/clash with method "gul" defined on line 20.}}
  public void gul() {} // Compliant

  public int tmp0;
  public void tmp0() {} // Noncompliant {{Rename method "tmp0" to prevent any misunderstanding/clash with field "tmp0" defined on line 22.}}
  
  private int tmp1;
  private void tmp1() {} // Compliant
  
  private int tmp2;
  public void tmp2() {}; // Compliant
  
  public int tmp3;
  private void tmp3() {}; // Compliant
  
  private boolean qix;
  
  public Object myField; // Noncompliant
  public void myField();
  
  public A() {}
  class MyInnerClass {}
  ;
}

interface MyInterface {
  public boolean myOtherMethod();
  public void gul();
  public void myField();
}

class B extends A {
  private static final long var1 = -9215047833775013803L; // Compliant
  public void var2() {}; // Noncompliant
  public void vAr2() {}; // Noncompliant
  
  public void myMethoD() {} // Noncompliant {{Rename method "myMethoD" to prevent any misunderstanding/clash with method "myMethod" defined in superclass "A".}}
  
  public Object qix;  // Compliant
  
  public void fOo(int i) {} // Noncompliant
}