class A {
  public void method() {
    IntStream.range(1, 5).map(x -> x * x - 1).forEach(x -> System.out.println(x));
    IntStream.range(1, 5).map(x -> {return x * x - 1;}) // Noncompliant {{Remove useless curly braces around statement and then remove useless return keyword}}
        .forEach(x -> { // Noncompliant {{Remove useless curly braces around statement}}
          System.out.println(x + 11);
        });
    //Non-Expression statement :
    IntStream.range(1, 5).map(x -> {
      if (x % 2 == 0) return 0;
      else return 1;
    });
    IntStream.range(1, 5).forEach(x -> {
      try {
      x = x/0;
      } catch (Exception e) {
        System.out.println(x);
      }
    });
    IntStream.range(1, 5).forEach(x -> {
      while(true) {
      }
    });

    //Nested blocks
    IntStream.range(1, 5).map(x -> { // Noncompliant {{Remove useless curly braces around statement}}
      {
        {
          return x + 1;
        }
      }
    });
  }

}
