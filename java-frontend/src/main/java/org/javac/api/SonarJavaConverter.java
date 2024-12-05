package org.javac.api;

import com.sun.source.tree.BlockTree;
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
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeTree;

public class SonarJavaConverter extends TreePathScanner<Void, Void> {

  private final List<JavaTree.CompilationUnitTreeImpl> parsedCUs = new ArrayList<>();

  private TokenManager tokenManager;
  private CompilationUnitTree currentCU;
  private final Trees trees;

  private final FirstBlockScanner blockScanner;

  public SonarJavaConverter(JavacTask task) {
    this.trees = Trees.instance(task);
    blockScanner = new FirstBlockScanner(trees);
  }

  public void convert(CompilationUnitTree node, CharSequence sourceCode) {
    currentCU = node;
    tokenManager = new TokenManager(node, trees, sourceCode);
    JavaTree.CompilationUnitTreeImpl cu = new JavaTree.CompilationUnitTreeImpl(
      convertPackageDeclaration(node.getPackage()),
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
        case CLASS -> result.add(convertClassTree((ClassTree) tree));
      }
    }
    return result;
  }

  private ClassTreeImpl convertClassTree(ClassTree classTree) {
//    blockScanner.scan(new TreePath(new TreePath(currentCU), classTree), null);
    return new ClassTreeImpl(
      Tree.Kind.CLASS,
      tokenManager.getOpenBraceToken(classTree),
      convertMembers(classTree.getMembers()),
      tokenManager.getCloseBraceToken(classTree))
      .complete(
        new ModifiersTreeImpl(convertModifiers(classTree)),
        convertClassDeclarationKeyword(classTree),
        convertClassSimpleName(classTree)
      );
  }

  private IdentifierTreeImpl convertClassSimpleName(ClassTree classTree) {
    int startPos = (int) tokenManager.getNodeStartLine(classTree);
    return new IdentifierTreeImpl(
      new InternalSyntaxToken(startPos, 0, classTree.getSimpleName().toString(), Collections.emptyList(), false)
    );
  }

  private List<ModifierTree> convertModifiers(ClassTree classTree) {
    for (var flag : classTree.getModifiers().getFlags()) {
      System.out.println("Class Flag: " + flag);
    }
    return Collections.emptyList();
  }

  private List<ImportClauseTree> parseImports(List<? extends ImportTree> imports) {
    return Collections.emptyList();
  }

  private JavaTree.PackageDeclarationTreeImpl convertPackageDeclaration(PackageTree aPackage) {
    return null;
  }

  private InternalSyntaxToken convertClassDeclarationKeyword(ClassTree classTree) {
    int startLine = (int) tokenManager.getNodeStartLine(classTree);
    return new InternalSyntaxToken(startLine, 0, "class", Collections.emptyList(), false);
  }

  private List<Tree> convertMembers(List<? extends com.sun.source.tree.Tree> members) {
    List<Tree> result = new ArrayList<>();
    for (com.sun.source.tree.Tree member : members) {
      switch (member.getKind()) {
        case METHOD -> result.add(convertMethodTree((MethodTree) member));
      }
    }
    return result;
  }

  private MethodTreeImpl convertMethodTree(MethodTree method) {
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

  private static class FirstBlockScanner extends TreePathScanner<Void, Void> {

    private BlockTree foundBlock;
    private final Trees trees;

    private FirstBlockScanner(Trees trees) {
      this.trees = trees;
    }

    public BlockTree getBlockTree() {
      return foundBlock;
    }

    @Override
    public Void visitBlock(BlockTree node, Void unused) {
      if (foundBlock == null) {
        foundBlock = node;
      }
      return super.visitBlock(node, unused);
    }
  }

}
