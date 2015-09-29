@SuppressWarnings("unused") // Noncompliant {{Suppressing the 'unused' warning is not allowed}}
class A {
  
  private final String BOXING = "boxing";
  
  @SuppressWarnings("all")
  private void f() {
  }
  
  @SuppressWarnings({"unchecked", "cast"}) // Noncompliant {{Suppressing the 'unchecked, cast' warnings is not allowed}}
  private void g() {
  }
  
  @SuppressWarnings(BOXING) // corner case: will not be detected, as using constants is not a proper use
  private void h() {
  }
  
  @SuppressWarnings({BOXING, "all"}) // "boxing" will not be detected (corner case), but "all" will be detected
  private void i() {
  }
  
  @SuppressWarnings("boxing") // Noncompliant {{Suppressing the 'boxing' warning is not allowed}}
  private void j() {
  }
  
  @Override
  private String toString() {
    return "";
  }
  
}
