@SuppressWarnings("unused")
class A {
  
  private final String BOXING = "boxing";
  
  @SuppressWarnings("all")
  private  void f() {
  }
  
  @SuppressWarnings({"unchecked", "cast"})
  private void g() {
  }
  
  @SuppressWarnings(BOXING)
  private void h() {
  }
  
  @SuppressWarnings({BOXING, "all"})
  private void i() {
  }
  
  @SuppressWarnings("boxing")
  private void j() {
  }
  
  @Override
  private String toString() {
    return "";
  }
  
}