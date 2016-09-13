class InferedLambdaType {

    private int foo = 42;

    private static String staticPrint(String s) { return s+"  "+s; }

    public void bar(java.util.Collection<String> data) {

      int myVar = 42 * 2;
      data.stream()
        .filter( s0 -> !s0.isEmpty())
        .map(line0 -> { return line0.split("\\s"); })
        .forEach(words -> System.out.println(words));

      data.stream()
        .filter(s1 -> !s1.isEmpty())
        .map(line1 -> {
          if(line1.length() >0) {
            return new Integer(1);
          } else {
            return new Long(2);
          }
        }).forEach(words -> System.out.println(words));

      data.stream()
        .filter( s2 -> !s2.isEmpty())
        .map(line2 -> { throw new IllegalStateException(); })
        .forEach(words -> System.out.println(words));

      data.stream()
        .filter( s3 -> !s3.isEmpty())
        .map(line3 -> {
          new Object() {
            Long foo() {
              return new Long(0);
            }
          };
          data.stream().map( s -> {return s.split("");});
          return new Integer(1);
        }).forEach(words -> System.out.println(words));

      data.stream()
        .filter( s -> !s.isEmpty())
        .map(line -> line.split("\\s"))
        .forEach(words -> {
          staticPrint(words[0]);
          if (foo > words.length || myVar < words.length) {
            // do something
          }
        });

      data.stream()
        .map(InferedLambdaType::staticPrint)
        .forEach(sx -> System.out.println(sx.length()));
    }
}

class ChainedMapOperations {

  public void qix(java.util.List<String> list) {
    list.stream()
      .filter(s -> s.length() > 42)
      .map(s -> stringToBoolean(s));

    list.stream()
      .filter(s -> s.length() > 42)
      .map(s -> stringToBoolean(s))
      .map(b -> booleanToInt(b));

    list.stream()
      .map(String::length)
      .filter(x -> intToInt(x) > 0);
  }

  private boolean stringToBoolean(String s) {
    return false;
  }

  private int booleanToInt(boolean b) {
    return 0;
  }

  private int intToInt(int x) {
    return x - 1;
  }

}
