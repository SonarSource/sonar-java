class A {
  void foo(){
    switch (1) {
      case 1:
        System.out.println("plop");
        break;
      case 2:
        System.out.println("bar"); //Compliant
        break;
      case 3:
      case 4:
        System.out.println("plop"); //Non-Compliant
        break;
      case 5:
        System.out.println("plop"); //Non-Compliant
        break;
    }
  }
}