package org.sonar.java.ecj;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.ReturnStatementTree;

import java.util.Collections;

public class TypesTest {

  @Test
  public void null_type() {
    ASTParser parser = ASTParser.newParser(AST.JLS11);
    parser.setResolveBindings(true);
    parser.setEnvironment(
      new String[]{},
      new String[]{},
      new String[]{},
      true
    );
    parser.setSource("class C { void m() { return null; } }".toCharArray());
    parser.setUnitName("Example.java");

    CompilationUnit compilationUnit = (CompilationUnit) parser.createAST(null);
    TypeDeclaration typeDeclaration = (TypeDeclaration) compilationUnit.types().get(0);
    MethodDeclaration methodDeclaration = typeDeclaration.getMethods()[0];
    ReturnStatement returnStatement = (ReturnStatement) methodDeclaration.getBody().statements().get(0);
    ITypeBinding typeBinding = returnStatement.getExpression().resolveTypeBinding();

    System.out.println(typeBinding);
    System.out.println(typeBinding.getName());
    System.out.println(typeBinding.getQualifiedName());
    System.out.println(typeBinding.isNullType());
  }

  @Test
  public void our_null_type() {
    CompilationUnitTree compilationUnit = (CompilationUnitTree) JavaParser.createParser().parse("class C { void m() { return null; } }");
    SemanticModel.createFor(compilationUnit, new SquidClassLoader(Collections.emptyList()));

    ClassTree classDeclaration = (ClassTree) compilationUnit.types().get(0);
    MethodTree methodDeclaration = (MethodTree) classDeclaration.members().get(0);
    ReturnStatementTree returnStatement = (ReturnStatementTree) methodDeclaration.block().body().get(0);
    Type type = returnStatement.expression().symbolType();
    System.out.println(type.fullyQualifiedName());
    System.out.println(type.name());
  }

}
