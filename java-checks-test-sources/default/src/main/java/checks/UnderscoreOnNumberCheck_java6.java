package checks;

class UnderscoreOnNumberCheck_java6 {
  int i2 = 100000; // Compliant
  int i3 = 10000000; // Compliant

  long l1 = 0x7fffffffffffffffL; // Compliant

  long octal1 = 012354435242; // Compliant
}
