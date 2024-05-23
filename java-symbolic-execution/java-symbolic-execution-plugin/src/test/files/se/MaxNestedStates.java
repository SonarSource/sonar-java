class A {
  void plop() {
    boolean a = true;
    a &= (Math.random() == 1.0d);
    a &= (Math.random() == 2.0d);
    a &= (Math.random() == 3.0d);
    a &= (Math.random() == 4.0d);
    a &= (Math.random() == 5.0d);
    a &= (Math.random() == 6.0d);
    a &= (Math.random() == 7.0d);
    a &= (Math.random() == 8.0d);
    a &= (Math.random() == 9.0d);
    a &= (Math.random() == 10.0d);
    a &= (Math.random() == 11.0d);
    a &= (Math.random() == 12.0d);
    a &= (Math.random() == 13.0d);
    a &= (Math.random() == 14.0d);
    a &= (Math.random() == 15.0d);
    a &= (Math.random() == 16.0d);
    a &= (Math.random() == 17.0d);
    a &= (Math.random() == 18.0d);
    a &= (Math.random() == 19.0d);
    a &= (Math.random() == 20.0d);
    a &= (Math.random() == 21.0d);
    a &= (Math.random() == 22.0d);
    a &= (Math.random() == 23.0d);
    a &= (Math.random() == 24.0d);
    a &= (Math.random() == 25.0d);
    a &= (Math.random() == 26.0d);
    a &= (Math.random() == 27.0d);
    a &= (Math.random() == 28.0d);

    if (a) { //BOOM : 2^n -1 states are generated (where n is the number of lines of &= assignements in the above code) -> fail fast by not even enqueuing nodes
    }
  }
}
