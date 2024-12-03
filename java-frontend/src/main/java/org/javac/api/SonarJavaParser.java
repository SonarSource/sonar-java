package org.javac.api;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.ModuleTree;
import com.sun.source.tree.PackageTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Element;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;

public class SonarJavaParser extends TreePathScanner<Void, Void> {

  JavacTask task;

  private final List<JavaTree.CompilationUnitTreeImpl> parsedCUs = new ArrayList<>();

  //Contains the semantic information about the AST
  private final Trees trees;

  public SonarJavaParser(JavacTask task) {
    this.task = task;
    this.trees = Trees.instance(task);
  }

  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, Void unused) {
    JavaTree.CompilationUnitTreeImpl cu = new JavaTree.CompilationUnitTreeImpl(
      parsePackageDeclaration(node.getPackage()),
      parseImports(node.getImports()),
      parseTypes(node.getTypeDecls()),
      parseModuleDeclaration(node.getModule()),
      getEOFToken()
    );
    parsedCUs.add(cu);
    return super.visitCompilationUnit(node, unused);
  }

  private SyntaxToken getEOFToken() {
    return null;
  }

  private ModuleDeclarationTree parseModuleDeclaration(ModuleTree module) {
    return null;
  }

  private List<Tree> parseTypes(List<? extends com.sun.source.tree.Tree> typeDecls) {
    var result = new ArrayList<Tree>();
    for(com.sun.source.tree.Tree tree : typeDecls) {
      switch(tree.getKind()){
        case CLASS -> result.add(parseClassTree((ClassTree) tree));
        case ENUM -> result.add(parseEnum((ClassTree) tree));
      }
    }
    return result;
  }

  private ClassTreeImpl parseClassTree(ClassTree classTree) {
    return new ClassTreeImpl(
      Tree.Kind.CLASS,
      null,
      Collections.emptyList(),
      null
    );
  }

  private ClassTreeImpl parseEnum(ClassTree classTree) {
    return new ClassTreeImpl(
      Tree.Kind.ENUM,
      null,
      Collections.emptyList(),
      null
    );
  }

  private List<ImportClauseTree> parseImports(List<? extends ImportTree> imports) {
    return Collections.emptyList();
  }

  private JavaTree.PackageDeclarationTreeImpl parsePackageDeclaration(PackageTree aPackage) {
    return null;
  }

  @Override
  public Void visitClass(ClassTree classTree, Void aVoid) {
    Element classElement = trees.getElement(getCurrentPath());
    System.out.println("Class: " + classElement.getSimpleName());
    return super.visitClass(classTree, aVoid);
  }

  public List<JavaTree.CompilationUnitTreeImpl> getParsedCUs() {
    return parsedCUs;
  }

}
