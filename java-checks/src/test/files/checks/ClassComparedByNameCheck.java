class A {

    String foo(String s) {
      A.class.getSimpleName().equals("A"); //NonCompliant
      new A().getClass().getSimpleName().equals("A"); //NonCompliant
      new A().getClass().getName().equals("A"); //NonCompliant
      String name = new A().getClass().getName();
      name.equals("A"); //False negative ?
      A.class.getSimpleName().substring(0).equals("A"); //NonCompliant
      foo(A.class.getSimpleName()).equals("A");

    }


}