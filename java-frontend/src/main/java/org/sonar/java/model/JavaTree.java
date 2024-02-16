/*
 * SonarQube Java
 * Copyright (C) 2012-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.sonar.java.Preconditions;
import org.sonar.java.annotations.Beta;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.expression.AssessableExpressionTree;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.PrimitiveTypeTree;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TreeVisitor;
import org.sonar.plugins.java.api.tree.TypeArguments;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.WildcardTree;
import org.sonarsource.analyzer.commons.collections.ListUtils;

public abstract class JavaTree implements Tree {

  protected CompilationUnitTreeImpl root;

  @Nullable
  private Tree parent;

  private List<Tree> children;


  @Override
  @Nullable
  public SyntaxToken firstToken() {
    for (Tree child : getChildren()) {
      SyntaxToken first = child.firstToken();
      if (first != null) {
        return first;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public SyntaxToken lastToken() {
    List<Tree> trees = getChildren();
    for (int index = trees.size() - 1; index >= 0; index--) {
      SyntaxToken last = trees.get(index).lastToken();
      if (last != null) {
        return last;
      }
    }
    return null;
  }

  public int getLine() {
    SyntaxToken firstSyntaxToken = firstToken();
    if (firstSyntaxToken == null) {
      return -1;
    }
    return LineUtils.startLine(firstSyntaxToken);
  }

  @Override
  public final boolean is(Kind... kinds) {
    Kind treeKind = kind();
    for (Kind kindIter : kinds) {
      if (treeKind == kindIter) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Tree parent() {
    return parent;
  }

  public void setParent(Tree parent) {
    this.root = ((JavaTree) parent).root;
    this.parent = parent;
  }

  /**
   * Creates iterable for children of this node.
   * Note that iterable may contain {@code null} elements.
   *
   * @throws java.lang.UnsupportedOperationException if {@link #isLeaf()} returns {@code true}
   */
  protected abstract List<Tree> children();

  public List<Tree> getChildren() {
    if(children == null) {
      children = children().stream()
        .filter(Objects::nonNull)
        .toList();
    }
    return children;
  }

  public boolean isLeaf() {
    return false;
  }

  public static class CompilationUnitTreeImpl extends JavaTree implements CompilationUnitTree {
    @Nullable
    private final PackageDeclarationTree packageDeclaration;
    private final List<ImportClauseTree> imports;
    private final List<Tree> types;
    @Nullable
    private final ModuleDeclarationTree moduleDeclaration;
    private final SyntaxToken eofToken;
    public JSema sema;

    private final Map<JProblem.Type, Set<JWarning>> warnings = new EnumMap<>(JProblem.Type.class);

    public CompilationUnitTreeImpl(@Nullable PackageDeclarationTree packageDeclaration, List<ImportClauseTree> imports, List<Tree> types,
      @Nullable ModuleDeclarationTree moduleDeclaration, SyntaxToken eofToken) {
      this.root = this;
      this.packageDeclaration = packageDeclaration;
      this.imports = imports;
      this.types = types;
      this.moduleDeclaration = moduleDeclaration;
      this.eofToken = eofToken;
    }

    @Override
    public Kind kind() {
      return Kind.COMPILATION_UNIT;
    }

    @Override
    public List<ImportClauseTree> imports() {
      return imports;
    }

    @Override
    public List<Tree> types() {
      return types;
    }

    @Beta
    public List<JWarning> warnings(JProblem.Type type) {
      return Collections.unmodifiableList(new ArrayList<>(warnings.getOrDefault(type, Collections.emptySet())));
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitCompilationUnit(this);
    }

    @Override
    public List<Tree> children() {
      Iterable<Tree> packageIterator = packageDeclaration == null ? Collections.emptyList() : Collections.singletonList(packageDeclaration);
      Iterable<Tree> moduleIterator = moduleDeclaration == null ? Collections.emptyList() : Collections.singletonList(moduleDeclaration);
      return ListUtils.concat(
        packageIterator,
        imports,
        types,
        moduleIterator,
        Collections.singletonList(eofToken));
    }

    @Nullable
    @Override
    public PackageDeclarationTree packageDeclaration() {
      return packageDeclaration;
    }

    @Nullable
    @Override
    public ModuleDeclarationTree moduleDeclaration() {
      return moduleDeclaration;
    }

    @Override
    public SyntaxToken eofToken() {
      return eofToken;
    }

    public void addWarnings(Map<JProblem.Type, Set<JWarning>> warnings) {
      this.warnings.putAll(warnings);
    }

  }

  public static class PackageDeclarationTreeImpl extends JavaTree implements PackageDeclarationTree {

    private final List<AnnotationTree> annotations;
    private final SyntaxToken packageKeyword;
    private final ExpressionTree packageName;
    private final SyntaxToken semicolonToken;

    public PackageDeclarationTreeImpl(List<AnnotationTree> annotations, SyntaxToken packageKeyword, ExpressionTree packageName, SyntaxToken semicolonToken) {
      this.annotations = Objects.requireNonNull(annotations);
      this.packageKeyword = packageKeyword;
      this.packageName = packageName;
      this.semicolonToken = semicolonToken;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitPackage(this);
    }

    @Override
    public List<AnnotationTree> annotations() {
      return annotations;
    }

    @Override
    public SyntaxToken packageKeyword() {
      return packageKeyword;
    }

    @Override
    public ExpressionTree packageName() {
      return packageName;
    }

    @Override
    public SyntaxToken semicolonToken() {
      return semicolonToken;
    }

    @Override
    public Kind kind() {
      return Tree.Kind.PACKAGE;
    }

    @Override
    public List<Tree> children() {
      return ListUtils.concat(
        annotations,
        Arrays.asList(packageKeyword, packageName, semicolonToken)
        );
    }

    public static String packageNameAsString(@Nullable PackageDeclarationTree tree) {
      if (tree == null) {
        return "";
      }
      Deque<String> pieces = new LinkedList<>();
      ExpressionTree expr = tree.packageName();
      while (expr.is(Tree.Kind.MEMBER_SELECT)) {
        MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
        pieces.push(mse.identifier().name());
        pieces.push(mse.operatorToken().text());
        expr = mse.expression();
      }
      if (expr.is(Tree.Kind.IDENTIFIER)) {
        IdentifierTree idt = (IdentifierTree) expr;
        pieces.push(idt.name());
      }

      StringBuilder sb = new StringBuilder();
      for (String piece : pieces) {
        sb.append(piece);
      }
      return sb.toString();
    }
  }

  public static class ImportTreeImpl extends JavaTree implements ImportTree {
    private final boolean isStatic;
    private final Tree qualifiedIdentifier;
    private final SyntaxToken semicolonToken;
    private final SyntaxToken importToken;
    private final SyntaxToken staticToken;

    public IBinding binding;

    public ImportTreeImpl(InternalSyntaxToken importToken, @Nullable InternalSyntaxToken staticToken,
                          Tree qualifiedIdentifier, InternalSyntaxToken semiColonToken) {
      this.importToken = importToken;
      this.staticToken = staticToken;
      this.qualifiedIdentifier = qualifiedIdentifier;
      this.semicolonToken = semiColonToken;
      isStatic = staticToken != null;
    }

    @Nullable
    public Symbol symbol() {
      if (binding != null) {
        switch (binding.getKind()) {
          case IBinding.TYPE:
            return root.sema.typeSymbol((ITypeBinding) binding);
          case IBinding.METHOD:
            return root.sema.methodSymbol((IMethodBinding) binding);
          case IBinding.VARIABLE:
            return root.sema.variableSymbol((IVariableBinding) binding);
        }
      }
      return null;
    }

    @Override
    public Kind kind() {
      return Kind.IMPORT;
    }

    @Override
    public boolean isStatic() {
      return isStatic;
    }

    @Override
    public SyntaxToken importKeyword() {
      return importToken;
    }

    @Nullable
    @Override
    public SyntaxToken staticKeyword() {
      return staticToken;
    }

    @Override
    public Tree qualifiedIdentifier() {
      return qualifiedIdentifier;
    }

    @Override
    public SyntaxToken semicolonToken() {
      return semicolonToken;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitImport(this);
    }

    @Override
    public List<Tree> children() {
      return ListUtils.concat(
        Collections.singletonList(importToken),
        isStatic ? Collections.singletonList(staticToken) : Collections.<Tree>emptyList(),
        Arrays.asList(qualifiedIdentifier, semicolonToken));
    }
  }

  public static class WildcardTreeImpl extends AbstractTypedTree implements WildcardTree, AnnotatedTypeTree {

    private SyntaxToken queryToken;
    @Nullable
    private final SyntaxToken extendsOrSuperToken;
    private final Kind kind;
    @Nullable
    private final TypeTree bound;
    private List<AnnotationTree> annotations;

    public WildcardTreeImpl(InternalSyntaxToken queryToken) {
      this.kind = Kind.UNBOUNDED_WILDCARD;
      this.annotations = Collections.emptyList();
      this.queryToken = queryToken;
      this.extendsOrSuperToken = null;
      this.bound = null;
    }

    public WildcardTreeImpl(Kind kind, InternalSyntaxToken extendsOrSuperToken, TypeTree bound) {
      Preconditions.checkState(kind == Kind.EXTENDS_WILDCARD || kind == Kind.SUPER_WILDCARD);
      this.kind = kind;
      this.annotations = Collections.emptyList();
      this.extendsOrSuperToken = extendsOrSuperToken;
      this.bound = bound;
    }

    public WildcardTreeImpl complete(InternalSyntaxToken queryToken) {
      Preconditions.checkState(kind == Kind.EXTENDS_WILDCARD || kind == Kind.SUPER_WILDCARD);
      this.queryToken = queryToken;
      return this;
    }

    @Override
    public void complete(List<AnnotationTree> annotations) {
      this.annotations = annotations;
    }

    @Override
    public Kind kind() {
      return kind;
    }

    @Override
    public List<AnnotationTree> annotations() {
      return annotations;
    }

    @Override
    public SyntaxToken queryToken() {
      return queryToken;
    }

    @Nullable
    @Override
    public SyntaxToken extendsOrSuperToken() {
      return extendsOrSuperToken;
    }

    @Nullable
    @Override
    public TypeTree bound() {
      return bound;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitWildcard(this);
    }

    @Override
    public List<Tree> children() {
      List<Tree> builder = new ArrayList<>(annotations);
      builder.add(queryToken);
      if (bound != null) {
        builder.add(extendsOrSuperToken);
        builder.add(bound);
      }
      return Collections.unmodifiableList(builder);
    }
  }

  public static class UnionTypeTreeImpl extends AbstractTypedTree implements UnionTypeTree {
    private final ListTree<TypeTree> typeAlternatives;

    public UnionTypeTreeImpl(QualifiedIdentifierListTreeImpl typeAlternatives) {
      this.typeAlternatives = Objects.requireNonNull(typeAlternatives);
    }

    @Override
    public Kind kind() {
      return Kind.UNION_TYPE;
    }

    @Override
    public ListTree<TypeTree> typeAlternatives() {
      return typeAlternatives;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitUnionType(this);
    }

    @Override
    public List<Tree> children() {
      return Collections.singletonList(typeAlternatives);
    }

    @Override
    public List<AnnotationTree> annotations() {
      return Collections.emptyList();
    }
  }

  public static class NotImplementedTreeImpl extends AssessableExpressionTree {

    public NotImplementedTreeImpl() {
    }

    @Override
    public Kind kind() {
      return Kind.OTHER;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitOther(this);
    }

    @Override
    public boolean isLeaf() {
      return true;
    }

    @Override
    public List<Tree> children() {
      throw new UnsupportedOperationException();
    }
  }

  public static class PrimitiveTypeTreeImpl extends AssessableExpressionTree implements PrimitiveTypeTree, AnnotatedTypeTree {

    private final InternalSyntaxToken token;
    private List<AnnotationTree> annotations;

    public PrimitiveTypeTreeImpl(InternalSyntaxToken token) {
      this.token = token;
      this.annotations = Collections.emptyList();
    }

    @Override
    public void complete(List<AnnotationTree> annotations) {
      this.annotations = annotations;
    }

    @Override
    public Kind kind() {
      return Kind.PRIMITIVE_TYPE;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitPrimitiveType(this);
    }

    @Override
    public SyntaxToken keyword() {
      return token;
    }

    @Override
    public List<Tree> children() {
      return ListUtils.concat(annotations, Collections.singletonList(token));
    }

    @Override
    public List<AnnotationTree> annotations() {
      return annotations;
    }
  }

  public static class ParameterizedTypeTreeImpl extends AssessableExpressionTree implements ParameterizedTypeTree, AnnotatedTypeTree {

    private final TypeTree type;
    private final TypeArguments typeArguments;
    private List<AnnotationTree> annotations;

    public ParameterizedTypeTreeImpl(TypeTree type, TypeArgumentListTreeImpl typeArguments) {
      this.type = Objects.requireNonNull(type);
      this.typeArguments = Objects.requireNonNull(typeArguments);
      this.annotations = Collections.emptyList();
    }

    @Override
    public void complete(List<AnnotationTree> annotations) {
      this.annotations = annotations;
    }

    @Override
    public Kind kind() {
      return Kind.PARAMETERIZED_TYPE;
    }

    @Override
    public TypeTree type() {
      return type;
    }

    @Override
    public TypeArguments typeArguments() {
      return typeArguments;
    }

    @Override
    public List<AnnotationTree> annotations() {
      return annotations;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitParameterizedType(this);
    }

    @Override
    public List<Tree> children() {
      return ListUtils.concat(annotations, Arrays.asList(type, typeArguments));
    }
  }

  public static class ArrayTypeTreeImpl extends AssessableExpressionTree implements ArrayTypeTree, AnnotatedTypeTree {
    private TypeTree type;
    private List<AnnotationTree> annotations;
    private final InternalSyntaxToken openBracketToken;
    private final InternalSyntaxToken closeBracketToken;
    private final InternalSyntaxToken ellipsisToken;

    public ArrayTypeTreeImpl(@Nullable TypeTree type, List<AnnotationTreeImpl> annotations, InternalSyntaxToken openBracketToken, InternalSyntaxToken closeBracketToken) {
      this.type = type;
      this.annotations = Collections.unmodifiableList(annotations);
      this.openBracketToken = openBracketToken;
      this.closeBracketToken = closeBracketToken;
      this.ellipsisToken = null;
    }

    public ArrayTypeTreeImpl(@Nullable TypeTree type, List<AnnotationTreeImpl> annotations, InternalSyntaxToken ellispsisToken) {
      this.type = type;
      this.annotations = Collections.unmodifiableList(annotations);
      this.openBracketToken = null;
      this.closeBracketToken = null;
      this.ellipsisToken = ellispsisToken;
    }

    public void completeType(TypeTree type) {
      this.type = type;
    }

    public void setLastChildType(TypeTree type) {
      ArrayTypeTree childType = this;
      while (childType.type() != null && childType.is(Tree.Kind.ARRAY_TYPE)) {
        childType = (ArrayTypeTree) childType.type();
      }
      ((ArrayTypeTreeImpl) childType).completeType(type);
    }

    @Override
    public Kind kind() {
      return Kind.ARRAY_TYPE;
    }

    @Override
    public TypeTree type() {
      return type;
    }

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitArrayType(this);
    }

    @Override
    public List<Tree> children() {
      boolean hasBrackets = ellipsisToken == null;
      return ListUtils.concat(
        Collections.singletonList(type),
        annotations,
        hasBrackets ? Arrays.asList(openBracketToken, closeBracketToken) : Collections.singletonList(ellipsisToken));
    }

    @Override
    public List<AnnotationTree> annotations() {
      return annotations;
    }

    @Override
    public SyntaxToken openBracketToken() {
      return openBracketToken;
    }

    @Override
    public SyntaxToken closeBracketToken() {
      return closeBracketToken;
    }

    @Override
    public SyntaxToken ellipsisToken() {
      return ellipsisToken;
    }

    @Override
    public void complete(List<AnnotationTree> typeAnnotations) {
      this.annotations = typeAnnotations;
    }
  }

  /**
   * This interface is dedicated to mark TypeTrees which will requires completion of their annotations during parsing.
   *
   * Note that {@link org.sonar.plugins.java.api.tree.InferedTypeTree} and {@link UnionTypeTree} can not have annotations.
   */
  public interface AnnotatedTypeTree extends TypeTree {
    void complete(List<AnnotationTree> annotations);
  }
}
