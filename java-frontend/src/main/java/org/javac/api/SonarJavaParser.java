package org.javac.api;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

public class SonarJavaParser extends TreePathScanner<Void, Void> {

  private final List<JavaTree.CompilationUnitTreeImpl> parsedCUs = new ArrayList<>();

  private CompilationUnitTree currentCU;
  private TokenManager tokenManager;
  private final Trees trees;

  public SonarJavaParser(JavacTask task) {
    this.trees = Trees.instance(task);
  }

  public void convert(CompilationUnitTree node, CharSequence sourceCode) {
    currentCU = node;
    tokenManager = new TokenManager(sourceCode, trees.getSourcePositions(), node);
    JavaTree.CompilationUnitTreeImpl cu = new JavaTree.CompilationUnitTreeImpl(
      parsePackageDeclaration(node.getPackage()),
      parseImports(node.getImports()),
      parseTypes(node.getTypeDecls()),
      parseModuleDeclaration(node.getModule()),
      tokenManager.getEOFToken());
    parsedCUs.add(cu);
  }

  private ModuleDeclarationTree parseModuleDeclaration(ModuleTree module) {
    return null;
  }

  private List<Tree> parseTypes(List<? extends com.sun.source.tree.Tree> typeDecls) {
    var result = new ArrayList<Tree>();
    for (com.sun.source.tree.Tree tree : typeDecls) {
      switch (tree.getKind()) {
        case CLASS -> result.add(parseClassTree((ClassTree) tree));
      }
    }
    return result;
  }

  private ClassTreeImpl parseClassTree(ClassTree classTree) {
    return new ClassTreeImpl(
      Tree.Kind.CLASS,
      getOpenBraceToken(classTree),
      parseMembers(classTree.getMembers()),
      getCloseBraceToken(classTree));
  }

  private SyntaxToken getOpenBraceToken(ClassTree classTree) {
    trees.getSourcePositions().getStartPosition(currentCU, classTree);
    return null;
  }

  private SyntaxToken getCloseBraceToken(ClassTree classTree) {
    return null;
  }

  private List<ImportClauseTree> parseImports(List<? extends ImportTree> imports) {
    return Collections.emptyList();
  }

  private JavaTree.PackageDeclarationTreeImpl parsePackageDeclaration(PackageTree aPackage) {
    return null;
  }

  private List<Tree> parseMembers(List<? extends com.sun.source.tree.Tree> members) {
    List<Tree> result = new ArrayList<>();
    for (com.sun.source.tree.Tree member : members) {
      switch (member.getKind()) {
        case METHOD -> result.add(parseMethodTree((MethodTree) member));
      }
    }
    return result;
  }

  private MethodTreeImpl parseMethodTree(MethodTree method) {
    return null;// new MethodTreeImpl( );
  }

  private TypeTree convertType(com.sun.source.tree.Tree type) {
    return switch (type.getKind()) {
      case PRIMITIVE_TYPE -> convertPrimitiveType((PrimitiveTypeTree) type);
      default -> throw new IllegalArgumentException("Unsupported type: " + type.getKind());
    };
  }

  private JavaTree.PrimitiveTypeTreeImpl convertPrimitiveType(PrimitiveTypeTree primitiveType) {
    return null; // new JavaTree.PrimitiveTypeTreeImpl(primitiveType.getPrimitiveTypeKind().toString());
  }

  // @Override
  // public Void visitClass(ClassTree classTree, Void aVoid) {
  // Element classElement = trees.getElement(getCurrentPath());
  // System.out.println("Class: " + classElement.getSimpleName());
  // return super.visitClass(classTree, aVoid);
  // }

  public List<JavaTree.CompilationUnitTreeImpl> getParsedCUs() {
    return parsedCUs;
  }

}
