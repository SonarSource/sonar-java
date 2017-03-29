class A {

  void mmf_771(int a, int b, int c) {
    if (a == b && b == c && a > c) {} // Noncompliant

    if (a > b && b > c && a < c) {} // Noncompliant
  }


  void test00(int a, int b, int c) {
    if (a <  c && b >  c && a == b) { } // Noncompliant

    if (a >  b && b >  c && a <  c) { } // Noncompliant

    if (a == b && b == c && a == c) { } // Noncompliant

    if (a >= b && b >= c && a >= c) { } // Noncompliant

  }


  void test01(int a, int x, int b) {
    if (x == a && x == b && a == b) { } // Noncompliant
    if (x == a && x == b && a != b) { } // Noncompliant
  }

  void test02(int a, int x, int b) {
    if (x == a && x != b && a != b) { } // Noncompliant
    if (x == a && x != b && a == b) { } // Noncompliant
  }

  void test03(int a, int x, int b) {
    if (x == a && x <  b && a <  b) { } // Noncompliant
    if (x == a && x <  b && a == b) { } // Noncompliant
  }

  void test04(int a, int x, int b) {
    if (x == a && x >= b && a >= b) { } // Noncompliant
    if (x == a && x >= b && a <  b) { } // Noncompliant
  }

  void test05(int a, int x, int b) {
    if (x != a && x == b && a != b) { } // Noncompliant
  }

  void test06(int a, int x, int b) {
    if (x != a && x != b && a <= b) { }
    if (x != a && x != b && a >= b) { }
    if (x != a && x != b && a != b) { }
  }

  void test07(int a, int x, int b) {
    if (x != a && x <  b && a <= b) { }
    if (x != a && x <  b && a >= b) { }
    if (x != a && x <  b && a != b) { }
  }

  void test08(int a, int x, int b) {
    if (x != a && x >= b && a <= b) { }
    if (x != a && x >= b && a >= b) { }
    if (x != a && x >= b && a != b) { }
  }

  void test09(int a, int x, int b) {
    if (x <  a && x == b && b <  a) { } // Noncompliant
  }

  void test10(int a, int x, int b) {
    if (x <  a && x != b && a <= b) { }
    if (x <  a && x != b && a >= b) { }
    if (x <  a && x != b && a != b) { }
  }

  void test11(int a, int x, int b) {
    if (x <  a && x <  b && a <= b) { }
    if (x <  a && x <  b && a >= b) { }
    if (x <  a && x <  b && a != b) { }
  }

  void test12(int a, int x, int b) {
    if (x <  a && x >= b && b <  a) { } // Noncompliant
  }

  void test13(int a, int x, int b) {
    if (x >= a && x == b && b >= a) { } // Noncompliant
  }

  void test14(int a, int x, int b) {
    if (x >= a && x != b && a <= b) { }
    if (x >= a && x != b && a >= b) { }
    if (x >= a && x != b && a != b) { }
  }

  void test15(int a, int x, int b) {
    if (x >= a && x <  b && a <  b) { } // Noncompliant
  }

  void test16(int a, int x, int b) {
    if (x >= a && x >= b && a <= b) { }
    if (x >= a && x >= b && a >= b) { }
    if (x >= a && x >= b && a != b) { }
  }


  void test17(int a, int x, int b) {
    if (a == x && x == b && a == b) { } // Noncompliant
  }

  void test18(int a, int x, int b) {
    if (a == x && x != b && a != b) { } // Noncompliant
  }

  void test19(int a, int x, int b) {
    if (a == x && x <  b && a <  b) { } // Noncompliant
  }

  void test20(int a, int x, int b) {
    if (a == x && x >= b && a >= b) { } // Noncompliant
  }

  void test21(int a, int x, int b) {
    if (a != x && x == b && a != b) { } // Noncompliant
  }

  void test22(int a, int x, int b) {
    if (a != x && x != b && a <= b) { }
    if (a != x && x != b && a >= b) { }
    if (a != x && x != b && a != b) { }
  }

  void test23(int a, int x, int b) {
    if (a != x && x <  b && a <= b) { }
    if (a != x && x <  b && a >= b) { }
    if (a != x && x <  b && a != b) { }
  }

  void test24(int a, int x, int b) {
    if (a != x && x >= b && a <= b) { }
    if (a != x && x >= b && a >= b) { }
    if (a != x && x >= b && a != b) { }
  }

  void test25(int a, int x, int b) {
    if (a <  x && x == b && a <  b) { } // Noncompliant
  }

  void test26(int a, int x, int b) {
    if (a <  x && x != b && a <= b) { }
    if (a <  x && x != b && a >= b) { }
    if (a <  x && x != b && a != b) { }
  }

  void test27(int a, int x, int b) {
    if (a <  x && x <  b && a <  b) { } // Noncompliant
  }

  void test28(int a, int x, int b) {
    if (a <  x && x >= b && a <= b) { }
    if (a <  x && x >= b && a >= b) { }
    if (a <  x && x >= b && a != b) { }
  }

  void test29(int a, int x, int b) {
    if (a >= x && x == b && a >= b) { } // Noncompliant
  }

  void test30(int a, int x, int b) {
    if (a >= x && x != b && a <= b) { }
    if (a >= x && x != b && a >= b) { }
    if (a >= x && x != b && a != b) { }
  }

  void test31(int a, int x, int b) {
    if (a >= x && x <  b && a <= b) { }
    if (a >= x && x <  b && a >= b) { }
    if (a >= x && x <  b && a != b) { }
  }

  void test32(int a, int x, int b) {
    if (a >= x && x >= b && a >= b) { } // Noncompliant
  }

  void test33(int a, int x, int b) {
    if (x == a && b == x && a == b) { } // Noncompliant
  }

  void test34(int a, int x, int b) {
    if (x == a && b != x && a != b) { } // Noncompliant
  }

  void test35(int a, int x, int b) {
    if (x == a && b <  x && b <  a) { } // Noncompliant
  }

  void test36(int a, int x, int b) {
    if (x == a && b >= x && b >= a) { } // Noncompliant
  }

  void test37(int a, int x, int b) {
    if (x != a && b == x && a != b) { } // Noncompliant
  }

  void test38(int a, int x, int b) {
    if (x != a && b != x && a <= b) { }
    if (x != a && b != x && a >= b) { }
    if (x != a && b != x && a != b) { }
  }

  void test39(int a, int x, int b) {
    if (x != a && b <  x && a <= b) { }
    if (x != a && b <  x && a >= b) { }
    if (x != a && b <  x && a != b) { }
  }

  void test40(int a, int x, int b) {
    if (x != a && b >= x && a <= b) { }
    if (x != a && b >= x && a >= b) { }
    if (x != a && b >= x && a != b) { }
  }

  void test41(int a, int x, int b) {
    if (x <  a && b == x && b <  a) { } // Noncompliant
  }

  void test42(int a, int x, int b) {
    if (x <  a && b != x && a <= b) { }
    if (x <  a && b != x && a >= b) { }
    if (x <  a && b != x && a != b) { }
  }

  void test43(int a, int x, int b) {
    if (x <  a && b <  x && b <  a) { } // Noncompliant
  }

  void test44(int a, int x, int b) {
    if (x <  a && b >= x && a <= b) { }
    if (x <  a && b >= x && a >= b) { }
    if (x <  a && b >= x && a != b) { }
  }

  void test45(int a, int x, int b) {
    if (x >= a && b == x && b >= a) { } // Noncompliant
  }

  void test46(int a, int x, int b) {
    if (x >= a && b != x && a <= b) { }
    if (x >= a && b != x && a >= b) { }
    if (x >= a && b != x && a != b) { }
  }

  void test47(int a, int x, int b) {
    if (x >= a && b <  x && a <= b) { }
    if (x >= a && b <  x && a >= b) { }
    if (x >= a && b <  x && a != b) { }
  }

  void test48(int a, int x, int b) {
    if (x >= a && b >= x && b >= a) { } // Noncompliant
  }

  void test49(int a, int x, int b) {
    if (a == x && b == x && a == b) { } // Noncompliant
  }

  void test50(int a, int x, int b) {
    if (a == x && b != x && a != b) { } // Noncompliant
  }

  void test51(int a, int x, int b) {
    if (a == x && b <  x && b <  a) { } // Noncompliant
  }

  void test52(int a, int x, int b) {
    if (a == x && b >= x && b >= a) { } // Noncompliant
  }

  void test53(int a, int x, int b) {
    if (a != x && b == x && a != b) { } // Noncompliant
  }

  void test54(int a, int x, int b) {
    if (a != x && b != x && a <= b) { }
    if (a != x && b != x && a >= b) { }
    if (a != x && b != x && a != b) { }
  }

  void test55(int a, int x, int b) {
    if (a != x && b <  x && a <= b) { }
    if (a != x && b <  x && a >= b) { }
    if (a != x && b <  x && a != b) { }
  }

  void test56(int a, int x, int b) {
    if (a != x && b >= x && a <= b) { }
    if (a != x && b >= x && a >= b) { }
    if (a != x && b >= x && a != b) { }
  }

  void test57(int a, int x, int b) {
    if (a <  x && b == x && a <  b) { } // Noncompliant
  }

  void test58(int a, int x, int b) {
    if (a <  x && b != x && a <= b) { }
    if (a <  x && b != x && a >= b) { }
    if (a <  x && b != x && a != b) { }
  }

  void test59(int a, int x, int b) {
    if (a <  x && b <  x && a <= b) { }
    if (a <  x && b <  x && a >= b) { }
    if (a <  x && b <  x && a != b) { }
  }

  void test60(int a, int x, int b) {
    if (a <  x && b >= x && a <  b) { } // Noncompliant
  }

  void test61(int a, int x, int b) {
    if (a >= x && b == x && a >= b) { } // Noncompliant
  }
  void test62(int a, int x, int b) {
    if (a >= x && b != x && a <= b) { }
    if (a >= x && b != x && a >= b) { }
    if (a >= x && b != x && a != b) { }
  }

  void test63(int a, int x, int b) {
    if (a >= x && b <  x && b <  a) { } // Noncompliant
  }

  void test64(int a, int x, int b) {
    if (a >= x && b >= x && a <= b) { }
    if (a >= x && b >= x && a >= b) { }
    if (a >= x && b >= x && a != b) { }
  }

  void test65(int length, int n_words) {
    int i = 0;
    int j = 0;
    if (length < n_words) {
      return;
    }
    while (i <= length - n_words) {
      if (j < n_words) {
      }
    }
  }

  void test_transitive_relations_are_not_generated_when_already_present(boolean a) {
    boolean b = a == false;
    if (b == false) {
      if (b) { // Noncompliant
      }
    }
  }

}
