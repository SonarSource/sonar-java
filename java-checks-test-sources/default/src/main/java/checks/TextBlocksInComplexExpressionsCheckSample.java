package checks;

import java.util.List;
import java.util.function.Supplier;

class TextBlocksInComplexExpressionsCheckSample {

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

  void fun(List<String> listOfStrings) {
    listOfStrings.stream()

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

    listOfStrings.stream()
      // Noncompliant@+1
      .map(str -> !"""
        <project>
          <modelVersion>4.0.0</modelVersion>
          <parent>
            <groupId>com.mycompany.app</groupId>
            <artifactId>my-app</artifactId>
        """.equals(str));

    listOfStrings.stream()
      .map(str -> { // Compliant
        return !"""
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
          """.equals(str);
      });

    listOfStrings.stream()
      .map(str -> { // Noncompliant@+1
        return !"""





              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
          """.equals(str);
      });

    listOfStrings.stream()
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

    listOfStrings.stream()
      .map(str -> !myTextBlock.equals(str)); // Compliant

    listOfStrings.stream()
      .map(str -> "ABC\nABC\nABC\nABC\nABC\nABC"); // Compliant
  }

}
