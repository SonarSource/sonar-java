class A {
  interface MySAM {
    boolean equals(Object o);
    String fun();
  }
  interface MySAM2 extends MySAM {
    boolean equals(Object o);
    int bar();
  }
  MySAM var = () -> "";
  MySAM2 var2 = () -> 1;
}
