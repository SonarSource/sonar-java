class InferedLambdaType {

    private int foo = 42; // FP on Unused private field

    private static String staticPrint(String s) { return s+"  "+s; } // FP on Unused private method

    public void bar(java.util.Collection<String> data) {

      int myVar = 42 * 2; // FP on Unused local variable
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
        .forEach(s3 -> System.out.println(s3.length()));
    }
}
