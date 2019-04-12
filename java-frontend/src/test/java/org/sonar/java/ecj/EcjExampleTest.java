package org.sonar.java.ecj;

import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.parser.Scanner;
import org.eclipse.jdt.internal.compiler.parser.TerminalTokens;
import org.eclipse.jdt.internal.formatter.DefaultCodeFormatterOptions;
import org.eclipse.jdt.internal.formatter.Token;
import org.eclipse.jdt.internal.formatter.TokenManager;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static org.fest.assertions.Assertions.assertThat;

public class EcjExampleTest {

  @Test
  public void test() {
    ASTParser astParser = ASTParser.newParser(AST.JLS11);
    astParser.setResolveBindings(true);
    astParser.setEnvironment(
      new String[]{},
      new String[]{},
      new String[]{},
      true
    );
    astParser.setUnitName("Example.java");

    String source = "class Example { X fun() { return \"\" ; } }";
    char[] sourceChars = source.toCharArray();
    astParser.setSource(sourceChars);
    CompilationUnit compilationUnit = (CompilationUnit) astParser.createAST(null);

    System.out.println(compilationUnit);

    List<String> problems = Arrays.stream(compilationUnit.getProblems())
      .map(IProblem::getMessage)
      .collect(Collectors.toList());
    assertThat(problems).containsOnly("X cannot be resolved to a type");

    List<Token> tokens = new ArrayList<>();
    Scanner scanner = new Scanner(
      true,
      true,
      false,
      CompilerOptions.versionToJdkLevel("11"),
      null,
      null,
      false
    );
    scanner.setSource(sourceChars);
    while (true) {
      try {
        int tokenType = scanner.getNextToken();
        if (tokenType == TerminalTokens.TokenNameEOF) {
          break;
        }
        Token token = Token.fromCurrent(scanner, tokenType);
        tokens.add(token);
      } catch (InvalidInputException e) {
        throw new RuntimeException(e);
      }
    }
    TokenManager tokenManager = new TokenManager(tokens, source, new DefaultCodeFormatterOptions(new HashMap<>()));

    compilationUnit.getRoot().accept(new ASTVisitor() {
      @Override
      public void endVisit(CompilationUnit node) {
        AbstractTypeDeclaration td;

        switch (node.getNodeType()) {
          case ASTNode.ANNOTATION_TYPE_DECLARATION:
            break;
          case ASTNode.BOOLEAN_LITERAL:
            break;
        }
      }

      @Override
      public boolean visit(ReturnStatement node) {
        int end = node.getStartPosition() + node.getLength() - 1;
        assertThat(sourceChars[end]).isEqualTo(';');

        Token semicolon = tokens.get(
          tokenManager.firstIndexAfter(node.getExpression(), TerminalTokens.TokenNameSEMICOLON)
        );
        assertThat(semicolon.originalStart).isEqualTo(end);

        ITypeBinding typeBinding = node.getExpression().resolveTypeBinding();
        assertThat(typeBinding.getName()).isEqualTo("String");

        return super.visit(node);
      }
    });
  }

}
