/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * sonarqube@googlegroups.com
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import org.sonar.java.ast.parser.TypeUnionListTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.java.syntaxtoken.FirstSyntaxTokenFinder;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.ListTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
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

import javax.annotation.Nullable;

import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public abstract class JavaTree implements Tree {


  protected GrammarRuleKey grammarRuleKey;

  public JavaTree(GrammarRuleKey grammarRuleKey) {
    this.grammarRuleKey = grammarRuleKey;
  }
  public int getLine() {
    SyntaxToken firstSyntaxToken = FirstSyntaxTokenFinder.firstSyntaxToken(this);
    if (firstSyntaxToken == null) {
      return -1;
    }
    return firstSyntaxToken.line();
  }

  @Override
  public final boolean is(Kind... kind) {
    if (kind() != null) {
      for (Kind kindIter : kind) {
        if (kind() == kindIter) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Creates iterator for children of this node.
   * Note that iterator may contain {@code null} elements.
   *
   * @throws java.lang.UnsupportedOperationException if {@link #isLeaf()} returns {@code true}
   */
  public abstract Iterator<Tree> childrenIterator();

  public boolean isLeaf() {
    return false;
  }

  public GrammarRuleKey getGrammarRuleKey() {
    return grammarRuleKey;
  }

  public static class CompilationUnitTreeImpl extends JavaTree implements CompilationUnitTree {
    private final PackageDeclarationTree packageDeclaration;
    private final List<ImportClauseTree> imports;
    private final List<Tree> types;
    private final SyntaxToken eofToken;

    public CompilationUnitTreeImpl(@Nullable PackageDeclarationTree packageDeclaration, List<ImportClauseTree> imports,
      List<Tree> types, SyntaxToken eofToken) {
      super(Kind.COMPILATION_UNIT);
      this.packageDeclaration = packageDeclaration;
      this.imports = imports;
      this.types = types;
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
    public Iterator<Tree> childrenIterator() {
      Iterator<Tree> packageIterator = packageDeclaration == null ?
        Iterators.<Tree>emptyIterator() :
        Iterators.<Tree>singletonIterator(packageDeclaration);
      return Iterators.concat(
        packageIterator,
        imports.iterator(),
        types.iterator(),
        Iterators.singletonIterator(eofToken));
    }

    @Nullable
    @Override
    public PackageDeclarationTree packageDeclaration() {
      return packageDeclaration;
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

      this.annotations = Preconditions.checkNotNull(annotations);
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
    public Iterator<Tree> childrenIterator() {
      return Iterators.concat(
        annotations.iterator(),
        Iterators.forArray(packageKeyword, packageName, semicolonToken)
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
    public Iterator<Tree> childrenIterator() {

      return Iterators.concat(
        Iterators.singletonIterator(importToken),
        isStatic ? Iterators.singletonIterator(staticToken) : Iterators.<Tree>emptyIterator(),
        Iterators.forArray(qualifiedIdentifier, semicolonToken));
    }
  }

  public static class WildcardTreeImpl extends JavaTree implements WildcardTree {

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

    public WildcardTreeImpl complete(List<AnnotationTree> annotations) {
      this.annotations = annotations;
      return this;
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
    public Iterator<Tree> childrenIterator() {
      ImmutableList.Builder<Tree> iteratorBuilder = ImmutableList.builder();
      iteratorBuilder.addAll(annotations);
      iteratorBuilder.add(queryToken);
      if (bound != null) {
        iteratorBuilder.add(extendsOrSuperToken);
        iteratorBuilder.add(bound);
      }
      return iteratorBuilder.build().iterator();
    }
  }

  public static class UnionTypeTreeImpl extends AbstractTypedTree implements UnionTypeTree {
    private final ListTree<TypeTree> typeAlternatives;

    public UnionTypeTreeImpl(TypeUnionListTreeImpl typeAlternatives) {
      super(Kind.UNION_TYPE);
      this.typeAlternatives = Preconditions.checkNotNull(typeAlternatives);
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
    public Iterator<Tree> childrenIterator() {
      return ImmutableList.<Tree>builder().add(typeAlternatives).build().iterator();
    }

    @Override
    public List<AnnotationTree> annotations() {
      return ImmutableList.of();
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
    public Iterator<Tree> childrenIterator() {
      throw new UnsupportedOperationException();
    }
  }

  public static class PrimitiveTypeTreeImpl extends AbstractTypedTree implements PrimitiveTypeTree {

    private final InternalSyntaxToken token;
    private List<AnnotationTree> annotations;

    public PrimitiveTypeTreeImpl(InternalSyntaxToken token) {
      super(Kind.PRIMITIVE_TYPE);
      this.token = token;
      this.annotations = ImmutableList.of();
    }

    public PrimitiveTypeTreeImpl complete(List<AnnotationTree> annotations) {
      this.annotations = annotations;
      return this;
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
    public Iterator<Tree> childrenIterator() {
      return Iterators.concat(annotations.iterator(), Iterators.singletonIterator(token));
    }

    @Override
    public List<AnnotationTree> annotations() {
      return annotations;
    }
  }

  public static class ParameterizedTypeTreeImpl extends AbstractTypedTree implements ParameterizedTypeTree, ExpressionTree {

    private final TypeTree type;
    private final TypeArguments typeArguments;
    private List<AnnotationTree> annotations;

    public ParameterizedTypeTreeImpl(TypeTree type, TypeArgumentListTreeImpl typeArguments) {
      super(Kind.PARAMETERIZED_TYPE);
      this.type = Preconditions.checkNotNull(type);
      this.typeArguments = Preconditions.checkNotNull(typeArguments);
      this.annotations = ImmutableList.<AnnotationTree>of();
    }

    public ParameterizedTypeTreeImpl complete(List<AnnotationTree> annotations) {
      this.annotations = annotations;
      return this;
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
    public Iterator<Tree> childrenIterator() {
      return Iterators.concat(annotations.iterator(), Iterators.forArray(type, typeArguments));
    }
  }

  public static class ArrayTypeTreeImpl extends AbstractTypedTree implements ArrayTypeTree {
    private TypeTree type;
    private final List<AnnotationTree> annotations;
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
    public Iterator<Tree> childrenIterator() {
      boolean hasBrackets = ellipsisToken == null;
      return Iterators.concat(
        Iterators.singletonIterator(type),
        annotations.iterator(),
        hasBrackets ? Iterators.forArray(openBracketToken, closeBracketToken) : Iterators.singletonIterator(ellipsisToken));
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
  }
}
