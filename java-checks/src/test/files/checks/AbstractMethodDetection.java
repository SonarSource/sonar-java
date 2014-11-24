class A{
  void method(int a){}
  void method(String a){}

}

class B{
  void foo(){
    A a = new A();
    a.method(1);
    a.method("");
  }
}