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
      case 4: // Noncompliant {{Either merge this case with the identical one on line "4" or change one of the implementations.}}
        System.out.println("plop");
        break;
      case 5: // Noncompliant {{Either merge this case with the identical one on line "4" or change one of the implementations.}}
        System.out.println("plop");
        break;
    }
  }
}