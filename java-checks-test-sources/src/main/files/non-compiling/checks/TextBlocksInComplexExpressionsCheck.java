package test;

class TextBlocksInComplexExpressionsCheck {

  // Compliant
  Supplier<String> supplier = () -> """
    <project>
      <modelVersion>4.0.0</modelVersion>
      <parent>
        <groupId>com.mycompany.app</groupId>
        <artifactId>my-app</artifactId>
        <version>1</version>
      </parent>

      <groupId>com.mycompany.app</groupId>
      <artifactId>my-module</artifactId>
      <version>1</version>
    </project>
    """;

  void fun() {
    listOfString.stream()

      .map(str -> { // Noncompliant@+1 [[sc=18;ec=14;el=+14]]{{Move this text block out of the lambda body and refactor it to a local variable or a static final field.}}
        var b = !"""
          <project>
            <modelVersion>4.0.0</modelVersion>
            <parent>
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
            </parent>

            <groupId>com.mycompany.app</groupId>
            <artifactId>my-module</artifactId>
            <version>1</version>
          </project>
          """.equals(str);

        System.out.println("ABC");
        System.out.println("ABC");
        System.out.println("ABC");

        return 0;
      });

    listOfString.stream()
      // Noncompliant@+1
      .map(str -> !"""
        <project>
          <modelVersion>4.0.0</modelVersion>
          <parent>
            <groupId>com.mycompany.app</groupId>
            <artifactId>my-app</artifactId>
        """.equals(str));

    listOfString.stream()
      .map(str -> { // Compliant
        return !"""
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
          """.equals(str);
      });

    listOfString.stream()
      .map(str -> { // Noncompliant@+1
        return !"""





              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
          """.equals(str);
      });

    listOfString.stream()
      // Compliant
      .map(str -> !"""
        <project>
          <parent>
            <groupId>com.mycompany.app</groupId>
        """.equals(str));

    String myTextBlock = """
      <project>
        <modelVersion>4.0.0</modelVersion>
        <parent>
          <groupId>com.mycompany.app</groupId>
          <artifactId>my-app</artifactId>
          <version>1</version>
        </parent>

        <groupId>com.mycompany.app</groupId>
        <artifactId>my-module</artifactId>
        <version>1</version>
      </project>
      """;

    listOfString.stream()
      .map(str -> !myTextBlock.equals(str)); // Compliant

    listOfString.stream()
      .map(str -> "ABC\nABC\nABC\nABC\nABC\nABC"); // Compliant
  }

}
