class A {
  static A newInstance() {
    return new A();
  }

  void securing() {
  }

  void other() {
  }
}

class B {
  A a_0 = A.newInstance();

  void securing_1() {
    A a = A.newInstance();
    a.securing();
  }

  void nothing_to_secure_2() {
    int i = 1;
    i++;
  }

  void unsecured_3() {
    A a = A.newInstance();
  }

  void unsecured_4() {
    A a = A.newInstance();
    a.other();
  }

  void not_assigned_triggering_call_5() {
    A.newInstance();
  }

  void not_correct_securing_6() {
    A a = A.newInstance();
    other();
  }

  void other() {
  }

  ;
}
