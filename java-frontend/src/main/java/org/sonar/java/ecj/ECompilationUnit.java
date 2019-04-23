package org.sonar.java.ecj;

import com.google.common.collect.Iterators;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.MethodsAreNonnullByDefault;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@MethodsAreNonnullByDefault
class ECompilationUnit extends ETree implements CompilationUnitTree {
  EPackageDeclaration packageDeclaration;
  List<ImportClauseTree> imports = new ArrayList<>();
  List<Tree> types = new ArrayList<>();
  SyntaxToken eofToken;

  @Nullable
  @Override
  public PackageDeclarationTree packageDeclaration() {
    return packageDeclaration;
  }

  @Override
  public List<ImportClauseTree> imports() {
    return imports;
  }

  @Override
  public List<Tree> types() {
    return types;
  }

  @Nullable
  @Override
  public ModuleDeclarationTree moduleDeclaration() {
    // FIXME
    return null;
  }

  @Override
  public SyntaxToken eofToken() {
    return eofToken;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitCompilationUnit(this);
  }

  @Override
  public Kind kind() {
    return Kind.COMPILATION_UNIT;
  }

  @Override
  Iterator<? extends Tree> childrenIterator() {
    return Iterators.concat(
      Iterators.forArray(packageDeclaration),
      imports.iterator(),
      types.iterator(),
      Iterators.forArray(eofToken)
    );
  }
}
