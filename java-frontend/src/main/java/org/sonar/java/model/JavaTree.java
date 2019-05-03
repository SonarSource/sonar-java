/*
 * SonarQube Java
 * Copyright (C) 2012-2019 SonarSource SA
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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sonar.java.ast.parser.TypeUnionListTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
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
import org.sonar.sslr.grammar.GrammarRuleKey;

public abstract class JavaTree implements Tree {

  @Nullable
  private Tree parent;

  protected GrammarRuleKey grammarRuleKey;

  private List<Tree> children;

  public JavaTree(GrammarRuleKey grammarRuleKey) {
    this.grammarRuleKey = grammarRuleKey;
  }

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
    return firstSyntaxToken.line();
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
    this.parent = parent;
  }

  /**
   * Creates iterable for children of this node.
   * Note that iterable may contain {@code null} elements.
   *
   * @throws java.lang.UnsupportedOperationException if {@link #isLeaf()} returns {@code true}
   */
  protected abstract Iterable<Tree> children();

  public List<Tree> getChildren() {
    if(children == null) {
      children = new ArrayList<>();
      children().forEach(child -> {
        // null children are ignored
        if (child != null) {
          children.add(child);
        }
      });
    }
    return children;
  }

  public boolean isLeaf() {
    return false;
  }

  public GrammarRuleKey getGrammarRuleKey() {
    return grammarRuleKey;
  }

  public static class CompilationUnitTreeImpl extends JavaTree implements CompilationUnitTree {
    @Nullable
    private final PackageDeclarationTree packageDeclaration;
    private final List<ImportClauseTree> imports;
    private final List<Tree> types;
    @Nullable
    private final ModuleDeclarationTree moduleDeclaration;
    private final SyntaxToken eofToken;

    public CompilationUnitTreeImpl(@Nullable PackageDeclarationTree packageDeclaration, List<ImportClauseTree> imports, List<Tree> types,
      @Nullable ModuleDeclarationTree moduleDeclaration, SyntaxToken eofToken) {
      super(Kind.COMPILATION_UNIT);
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

    @Override
    public void accept(TreeVisitor visitor) {
      visitor.visitCompilationUnit(this);
    }

    @Override
    public Iterable<Tree> children() {
      Iterable<Tree> packageIterator = packageDeclaration == null ? Collections.emptyList() : Collections.singletonList(packageDeclaration);
      Iterable<Tree> moduleIterator = moduleDeclaration == null ? Collections.emptyList() : Collections.singletonList(moduleDeclaration);
      return Iterables.concat(
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

  }

  public static class PackageDeclarationTreeImpl extends JavaTree implements PackageDeclarationTree {

    private final List<AnnotationTree> annotations;
    private final SyntaxToken packageKeyword;
    private final ExpressionTree packageName;
    private final SyntaxToken semicolonToken;

    public PackageDeclarationTreeImpl(List<AnnotationTree> annotations, SyntaxToken packageKeyword, ExpressionTree packageName, SyntaxToken semicolonToken) {
      super(Tree.Kind.PACKAGE);

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
    public Iterable<Tree> children() {
      return Iterables.concat(
        annotations,
        Lists.newArrayList(packageKeyword, packageName, semicolonToken)
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

    public ImportTreeImpl(InternalSyntaxToken importToken, @Nullable InternalSyntaxToken staticToken,
                          Tree qualifiedIdentifier, InternalSyntaxToken semiColonToken) {
      super(Kind.IMPORT);
      this.importToken = importToken;
      this.staticToken = staticToken;
      this.qualifiedIdentifier = qualifiedIdentifier;
      this.semicolonToken = semiColonToken;
      isStatic = staticToken != null;
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
    public Iterable<Tree> children() {

      return Iterables.concat(
        Collections.singletonList(importToken),
        isStatic ? Collections.singletonList(staticToken) : Collections.<Tree>emptyList(),
        Lists.newArrayList(qualifiedIdentifier, semicolonToken));
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
      super(Kind.UNBOUNDED_WILDCARD);
      this.kind = Kind.UNBOUNDED_WILDCARD;
      this.annotations = Collections.emptyList();
      this.queryToken = queryToken;
      this.extendsOrSuperToken = null;
      this.bound = null;
    }

    public WildcardTreeImpl(Kind kind, InternalSyntaxToken extendsOrSuperToken, TypeTree bound) {
      super(kind);
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
    public Iterable<Tree> children() {
      ImmutableList.Builder<Tree> builder = ImmutableList.builder();
      builder.addAll(annotations);
      builder.add(queryToken);
      if (bound != null) {
        builder.add(extendsOrSuperToken);
        builder.add(bound);
      }
      return builder.build();
    }
  }

  public static class UnionTypeTreeImpl extends AbstractTypedTree implements UnionTypeTree {
    private final ListTree<TypeTree> typeAlternatives;

    public UnionTypeTreeImpl(TypeUnionListTreeImpl typeAlternatives) {
      super(Kind.UNION_TYPE);
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
    public Iterable<Tree> children() {
      return ImmutableList.<Tree>builder().add(typeAlternatives).build();
    }

    @Override
    public List<AnnotationTree> annotations() {
      return Collections.emptyList();
    }
  }

  public static class NotImplementedTreeImpl extends AbstractTypedTree implements ExpressionTree {

    public NotImplementedTreeImpl() {
      super(Kind.OTHER);
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
    public Iterable<Tree> children() {
      throw new UnsupportedOperationException();
    }
  }

  public static class PrimitiveTypeTreeImpl extends AbstractTypedTree implements PrimitiveTypeTree, AnnotatedTypeTree {

    private final InternalSyntaxToken token;
    private List<AnnotationTree> annotations;

    public PrimitiveTypeTreeImpl(InternalSyntaxToken token) {
      super(Kind.PRIMITIVE_TYPE);
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
    public Iterable<Tree> children() {
      return Iterables.concat(annotations, Collections.singletonList(token));
    }

    @Override
    public List<AnnotationTree> annotations() {
      return annotations;
    }
  }

  public static class ParameterizedTypeTreeImpl extends AbstractTypedTree implements ParameterizedTypeTree, ExpressionTree, AnnotatedTypeTree {

    private final TypeTree type;
    private final TypeArguments typeArguments;
    private List<AnnotationTree> annotations;

    public ParameterizedTypeTreeImpl(TypeTree type, TypeArgumentListTreeImpl typeArguments) {
      super(Kind.PARAMETERIZED_TYPE);
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
    public Iterable<Tree> children() {
      return Iterables.concat(annotations, Lists.newArrayList(type, typeArguments));
    }
  }

  public static class ArrayTypeTreeImpl extends AbstractTypedTree implements ArrayTypeTree, AnnotatedTypeTree {
    private TypeTree type;
    private List<AnnotationTree> annotations;
    private final InternalSyntaxToken openBracketToken;
    private final InternalSyntaxToken closeBracketToken;
    private final InternalSyntaxToken ellipsisToken;

    public ArrayTypeTreeImpl(@Nullable TypeTree type, List<AnnotationTreeImpl> annotations, InternalSyntaxToken openBracketToken, InternalSyntaxToken closeBracketToken) {
      super(Kind.ARRAY_TYPE);
      this.type = type;
      this.annotations = getAnnotations(annotations);
      this.openBracketToken = openBracketToken;
      this.closeBracketToken = closeBracketToken;
      this.ellipsisToken = null;
    }

    public ArrayTypeTreeImpl(@Nullable TypeTree type, List<AnnotationTreeImpl> annotations, InternalSyntaxToken ellispsisToken) {
      super(Kind.ARRAY_TYPE);
      this.type = type;
      this.annotations = getAnnotations(annotations);
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
    public Iterable<Tree> children() {
      boolean hasBrackets = ellipsisToken == null;
      return Iterables.concat(
        Collections.singletonList(type),
        annotations,
        hasBrackets ? Lists.newArrayList(openBracketToken, closeBracketToken) : Collections.singletonList(ellipsisToken));
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

    private static ImmutableList<AnnotationTree> getAnnotations(List<AnnotationTreeImpl> annotations) {
      ImmutableList.Builder<AnnotationTree> annotationBuilder = ImmutableList.builder();
      for (AnnotationTreeImpl annotation : annotations) {
        annotationBuilder.add(annotation);
      }
      return annotationBuilder.build();
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
