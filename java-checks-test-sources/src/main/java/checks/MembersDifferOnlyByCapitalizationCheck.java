package checks;

abstract class MembersDifferOnlyByCapitalizationCheck implements MembersDifferOnlyByCapitalizationCheckInterface {
  private static final long var1 = -9215047833775013803L; // Compliant
  public long var2 = 0L;
  
  public static void equAls(Object obj) {} // Noncompliant [[sc=22;ec=28]] {{Rename method "equAls" to prevent any misunderstanding/clash with method "equals" defined in superclass "java.lang.Object".}}
  protected void finaliZe() {} // Noncompliant {{Rename method "finaliZe" to prevent any misunderstanding/clash with method "finalize" defined in superclass "java.lang.Object".}}
  
  public void myMethod() {}
  
  public Object stuff;
  public void stuff() {} // Noncompliant {{Rename method "stuff" to prevent any misunderstanding/clash with field "stuff" defined on line 12.}}
  public void stuff(int i) {} // Noncompliant {{Rename method "stuff" to prevent any misunderstanding/clash with field "stuff" defined on line 12.}}
  
  public void foo(int i) {} // Compliant
  public void foo(boolean i) {} // Compliant
  
  public void myOtherMethoD() {} // Noncompliant {{Rename method "myOtherMethoD" to prevent any misunderstanding/clash with method "myOtherMethod" defined in interface "checks.MembersDifferOnlyByCapitalizationCheckInterface".}}
  
  private static void gUl() {} // Noncompliant {{Rename method "gUl" to prevent any misunderstanding/clash with method "gul" defined on line 22.}}
  public void gul() {} // Compliant

  private boolean qix;
  
  public Object myField; // Noncompliant
  public void myField() {} // Compliant, as it overrides the parent interface method
  
  public MembersDifferOnlyByCapitalizationCheck() {}
  class MyInnerClass {}
  ;

  public void SUPER() {} // Compliant
  public void tHiS() {} // Compliant
}

interface MembersDifferOnlyByCapitalizationCheckInterface {
  public boolean myOtherMethod();
  public void gul();
  public void myField();
}

abstract class MembersDifferOnlyByCapitalizationCheckB extends MembersDifferOnlyByCapitalizationCheck {
  private static final long var1 = -9215047833775013803L; // Compliant
  public void var2() {}; // Noncompliant
  public void vAr2() {}; // Noncompliant
  
  public void myMethoD() {} // Noncompliant {{Rename method "myMethoD" to prevent any misunderstanding/clash with method "myMethod" defined in superclass "checks.MembersDifferOnlyByCapitalizationCheck".}}
  
  public Object qix;  // Compliant
  
  public void fOo(int i) {} // Noncompliant
}

class MembersDifferOnlyByCapitalizationCheckVisibility {
  public int tmp0;
  private void tmp0() {}
  public void tmp0(int i) {} // Noncompliant {{Rename method "tmp0" to prevent any misunderstanding/clash with field "tmp0" defined on line 56.}}
  protected void tmp0(long l) {}
  void tmp0(short s) {}
  
  private int tmp1;
  private void tmp1() {} // Compliant - private members having same name are ignored
  public void tmp1(int i) {}
  protected void tmp1(long l) {}
  void tmp1(short s) {}
  
  int tmp2;
  private void tmp2() {}
  void tmp2(short s) {}  // Noncompliant {{Rename method "tmp2" to prevent any misunderstanding/clash with field "tmp2" defined on line 68.}}
  
  protected int tmp3;
  private void tmp3() {}
  public void tmp3(int i) {}
  protected void tmp3(long l) {} // Noncompliant {{Rename method "tmp3" to prevent any misunderstanding/clash with field "tmp3" defined on line 72.}}
  void tmp3(short s) {} 
}

enum MembersDifferOnlyByCapitalizationCheckEnum {
  FOO;
  
  public void foo() {} // Compliant
  public static MembersDifferOnlyByCapitalizationCheckEnum valueof(int i) { return FOO; } // Noncompliant {{Rename method "valueof" to prevent any misunderstanding/clash with method "valueOf".}}
}

class MembersDifferOnlyByCapitalizationCheckBuilder {
  String name;
  String wrong;

  MembersDifferOnlyByCapitalizationCheckBuilder name(String name) { // Compliant - exception for builder pattern
    this.name = name;
    return this;
  }

  MembersDifferOnlyByCapitalizationCheckBuilder WRONG(String wrong) { // Noncompliant
    this.wrong = wrong;
    return this;
  }
}
