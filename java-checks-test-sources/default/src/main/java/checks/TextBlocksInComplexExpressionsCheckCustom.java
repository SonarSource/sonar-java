package checks;

import java.util.List;
import java.util.function.Supplier;

class TextBlocksInComplexExpressionsCheckCustom {

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
    
    
    ABC
    """;
  
  void fun(List<String> listOfStrings) {
    listOfStrings.stream()
      // Noncompliant@+1
      .map(str -> !"""
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
        ABC
        ABC
        CBA
        """.equals(str));
  } 

  void fun2(List<String> listOfStrings) {
    listOfStrings.stream()
      .map(str -> { // Noncompliant@+1
        return !"""
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
          ABC
          ABC
          CBA
          """.equals(str);
      });
  }

  void fun3(List<String> listOfStrings) {
    listOfStrings.stream()
      // Compliant
      .map(str -> !"""
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
    ABC
    CBA    """.equals(str));



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
  }
  
}
