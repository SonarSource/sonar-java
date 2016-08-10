class InferedLambdaType {

    private int foo = 42; // FP on Unused private field

    private static void staticPrint(String s) { /* do something */ } // FP on Unused private method

    public void bar(java.util.Collection<String> data) {

      int myVar = 42 * 2; // FP on Unused local variable

      data.stream()
        .filter( s -> !s.isEmpty())
        .map(line -> line.split("\\s"))
        .forEach(words -> {
          staticPrint(words[0]);
          if (foo > words.length || myVar < words.length) {
            // do something
          }
        });
    }
}
