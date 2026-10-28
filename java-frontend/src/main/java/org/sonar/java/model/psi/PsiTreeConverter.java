/*
 * SonarQube Java
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonar.java.model.psi;

import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationMethod;
import com.intellij.psi.PsiAnonymousClass;
import com.intellij.psi.PsiArrayAccessExpression;
import com.intellij.psi.PsiArrayInitializerExpression;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiAssertStatement;
import com.intellij.psi.PsiAssignmentExpression;
import com.intellij.psi.PsiBlockStatement;
import com.intellij.psi.PsiBreakStatement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiConditionalExpression;
import com.intellij.psi.PsiContinueStatement;
import com.intellij.psi.PsiDeclarationStatement;
import com.intellij.psi.PsiDeconstructionPattern;
import com.intellij.psi.PsiDefaultCaseLabelElement;
import com.intellij.psi.PsiDoWhileStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEmptyStatement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiExpressionListStatement;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiForStatement;
import com.intellij.psi.PsiForeachStatement;
import com.intellij.psi.PsiIfStatement;
import com.intellij.psi.PsiImplicitClass;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportModuleStatement;
import com.intellij.psi.PsiImportStatementBase;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiInstanceOfExpression;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiJavaModule;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiKeyword;
import com.intellij.psi.PsiLabeledStatement;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiMethodReferenceExpression;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiPackageAccessibilityStatement;
import com.intellij.psi.PsiPackageStatement;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiParenthesizedExpression;
import com.intellij.psi.PsiPattern;
import com.intellij.psi.PsiPolyadicExpression;
import com.intellij.psi.PsiPostfixExpression;
import com.intellij.psi.PsiPrefixExpression;
import com.intellij.psi.PsiProvidesStatement;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiRequiresStatement;
import com.intellij.psi.PsiResourceExpression;
import com.intellij.psi.PsiResourceListElement;
import com.intellij.psi.PsiResourceVariable;
import com.intellij.psi.PsiReturnStatement;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.PsiSuperExpression;
import com.intellij.psi.PsiSwitchExpression;
import com.intellij.psi.PsiSwitchLabelStatementBase;
import com.intellij.psi.PsiSwitchLabeledRuleStatement;
import com.intellij.psi.PsiSwitchStatement;
import com.intellij.psi.PsiSynchronizedStatement;
import com.intellij.psi.PsiThisExpression;
import com.intellij.psi.PsiThrowStatement;
import com.intellij.psi.PsiTryStatement;
import com.intellij.psi.PsiTypeCastExpression;
import com.intellij.psi.PsiTypeElement;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.PsiTypeTestPattern;
import com.intellij.psi.PsiUnnamedPattern;
import com.intellij.psi.PsiUsesStatement;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.PsiWhileStatement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.PsiYieldStatement;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.ast.parser.FormalParametersListTreeImpl;
import org.sonar.java.ast.parser.InitializerListTreeImpl;
import org.sonar.java.ast.parser.ModuleNameListTreeImpl;
import org.sonar.java.ast.parser.ModuleNameTreeImpl;
import org.sonar.java.ast.parser.QualifiedIdentifierListTreeImpl;
import org.sonar.java.ast.parser.ResourceListTreeImpl;
import org.sonar.java.ast.parser.StatementListTreeImpl;
import org.sonar.java.ast.parser.TypeParameterListTreeImpl;
import org.sonar.java.model.ArrayDimensionTreeImpl;
import org.sonar.java.model.InternalSyntaxToken;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.JavaTreeFinalizer;
import org.sonar.java.model.KeywordSuper;
import org.sonar.java.model.KeywordThis;
import org.sonar.java.model.SemanticValues;
import org.sonar.java.model.TypeParameterTreeImpl;
import org.sonar.java.model.declaration.AnnotationTreeImpl;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.java.model.declaration.EnumConstantTreeImpl;
import org.sonar.java.model.declaration.ExportsDirectiveTreeImpl;
import org.sonar.java.model.declaration.MethodTreeImpl;
import org.sonar.java.model.declaration.ModifierKeywordTreeImpl;
import org.sonar.java.model.declaration.ModifiersTreeImpl;
import org.sonar.java.model.declaration.ModuleDeclarationTreeImpl;
import org.sonar.java.model.declaration.OpensDirectiveTreeImpl;
import org.sonar.java.model.declaration.ProvidesDirectiveTreeImpl;
import org.sonar.java.model.declaration.RequiresDirectiveTreeImpl;
import org.sonar.java.model.declaration.UsesDirectiveTreeImpl;
import org.sonar.java.model.declaration.VariableTreeImpl;
import org.sonar.java.model.expression.ArrayAccessExpressionTreeImpl;
import org.sonar.java.model.expression.AssignmentExpressionTreeImpl;
import org.sonar.java.model.expression.BinaryExpressionTreeImpl;
import org.sonar.java.model.expression.ConditionalExpressionTreeImpl;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.java.model.expression.InstanceOfTreeImpl;
import org.sonar.java.model.expression.InternalPostfixUnaryExpression;
import org.sonar.java.model.expression.InternalPrefixUnaryExpression;
import org.sonar.java.model.expression.LambdaExpressionTreeImpl;
import org.sonar.java.model.expression.LiteralTreeImpl;
import org.sonar.java.model.expression.MemberSelectExpressionTreeImpl;
import org.sonar.java.model.expression.MethodInvocationTreeImpl;
import org.sonar.java.model.expression.MethodReferenceTreeImpl;
import org.sonar.java.model.expression.NewArrayTreeImpl;
import org.sonar.java.model.expression.NewClassTreeImpl;
import org.sonar.java.model.expression.ParenthesizedTreeImpl;
import org.sonar.java.model.expression.TypeArgumentListTreeImpl;
import org.sonar.java.model.expression.TypeCastExpressionTreeImpl;
import org.sonar.java.model.expression.VarTypeTreeImpl;
import org.sonar.java.model.pattern.DefaultPatternTreeImpl;
import org.sonar.java.model.pattern.GuardedPatternTreeImpl;
import org.sonar.java.model.pattern.NullPatternTreeImpl;
import org.sonar.java.model.pattern.RecordPatternTreeImpl;
import org.sonar.java.model.pattern.TypePatternTreeImpl;
import org.sonar.java.model.statement.AssertStatementTreeImpl;
import org.sonar.java.model.statement.BlockTreeImpl;
import org.sonar.java.model.statement.BreakStatementTreeImpl;
import org.sonar.java.model.statement.CaseGroupTreeImpl;
import org.sonar.java.model.statement.CaseLabelTreeImpl;
import org.sonar.java.model.statement.CatchTreeImpl;
import org.sonar.java.model.statement.ContinueStatementTreeImpl;
import org.sonar.java.model.statement.DoWhileStatementTreeImpl;
import org.sonar.java.model.statement.EmptyStatementTreeImpl;
import org.sonar.java.model.statement.ExpressionStatementTreeImpl;
import org.sonar.java.model.statement.ForEachStatementImpl;
import org.sonar.java.model.statement.ForStatementTreeImpl;
import org.sonar.java.model.statement.IfStatementTreeImpl;
import org.sonar.java.model.statement.LabeledStatementTreeImpl;
import org.sonar.java.model.statement.ReturnStatementTreeImpl;
import org.sonar.java.model.statement.StaticInitializerTreeImpl;
import org.sonar.java.model.statement.SwitchExpressionTreeImpl;
import org.sonar.java.model.statement.SwitchStatementTreeImpl;
import org.sonar.java.model.statement.SynchronizedStatementTreeImpl;
import org.sonar.java.model.statement.ThrowStatementTreeImpl;
import org.sonar.java.model.statement.TryStatementTreeImpl;
import org.sonar.java.model.statement.WhileStatementTreeImpl;
import org.sonar.java.model.statement.YieldStatementTreeImpl;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ArrayDimensionTree;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.EnumConstantTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportClauseTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModuleDeclarationTree;
import org.sonar.plugins.java.api.tree.ModuleDirectiveTree;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;
import org.sonar.plugins.java.api.tree.PatternTree;
import org.sonar.plugins.java.api.tree.StatementTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;

/** Converts a PSI source snapshot to the existing Sonar syntax model without resolving semantics. */
public final class PsiTreeConverter {

  private final PsiTokenIndex tokens;
  private final Map<PsiTypeElement, TypeTree> types = new IdentityHashMap<>();
  private final Map<PsiModifierList, ModifiersTreeImpl> modifierLists = new IdentityHashMap<>();

  private PsiTreeConverter(PsiJavaFile file, PsiSourceSnapshot source) {
    tokens = new PsiTokenIndex(file, source);
  }

  /** The caller owns the PSI read action and may dispose its environment after conversion. Sources containing Unicode escapes must use a snapshot. */
  public static JavaTree.CompilationUnitTreeImpl convert(PsiJavaFile file, String source) {
    var snapshot = PsiSourceSnapshot.from(source);
    if (!snapshot.psiText().equals(source)) {
      throw new IllegalArgumentException("Source contains Unicode escapes; convert a PsiSourceSnapshot instead");
    }
    return convert(file, snapshot);
  }

  /** Converts the exact parsing text of a translated snapshot while retaining its original token spelling and positions. */
  public static JavaTree.CompilationUnitTreeImpl convert(PsiJavaFile file, PsiSourceSnapshot source) {
    var converter = new PsiTreeConverter(file, source);
    requireNoParseErrors(file);
    var result = converter.compilationUnit(file);
    JavaTreeFinalizer.setParents(result);
    return result;
  }

  /** PSI reports a module named {@code transitive} as a missing requires name; {@link #module} converts that form like ECJ. */
  private static void requireNoParseErrors(PsiJavaFile file) {
    for (var error : PsiTreeUtil.findChildrenOfType(file, PsiErrorElement.class)) {
      if (!(error.getParent() instanceof PsiRequiresStatement)) {
        throw new IllegalArgumentException("PSI contains parse errors: " + error.getErrorDescription() + " at " + error.getTextRange());
      }
    }
  }

  private JavaTree.CompilationUnitTreeImpl compilationUnit(PsiJavaFile file) {
    var declaration = file.getPackageStatement();
    PackageDeclarationTree packageTree = declaration == null ? null : new JavaTree.PackageDeclarationTreeImpl(
      annotations(declaration.getAnnotationList()), first(declaration, "package"), (ExpressionTree) reference(declaration.getPackageReference()), last(declaration, ";"));
    List<ImportClauseTree> imports = new ArrayList<>();
    List<Tree> types = new ArrayList<>();
    var importList = file.getImportList();
    boolean hasImports = importList != null && importList.getAllImportStatements().length != 0;
    List<? super EmptyStatementTreeImpl> emptyStatements = hasImports || file.getClasses().length != 0 ? null : imports;
    for (PsiElement child : file.getChildren()) {
      if (child instanceof PsiImportList list) {
        for (PsiElement entry : list.getChildren()) {
          if (entry instanceof PsiImportStatementBase imp) {
            imports.add(importTree(imp));
            emptyStatements = imports;
          } else if (is(entry, ";")) {
            addEmptyStatement(entry, emptyStatements);
          } else {
            requireTrivia(entry);
          }
        }
      } else if (child instanceof PsiClass c) {
        types.add(classTree(c));
        emptyStatements = types;
      } else if (is(child, ";")) {
        addEmptyStatement(child, emptyStatements);
      } else if (child instanceof PsiPackageStatement || child instanceof PsiJavaModule) {
        emptyStatements = null;
      } else {
        requireTrivia(child);
      }
    }
    return new JavaTree.CompilationUnitTreeImpl(packageTree, imports, types, module(file.getModuleDeclaration()), tokens.eof());
  }

  private JavaTree.ImportTreeImpl importTree(PsiImportStatementBase imp) {
    PsiElement name = imp instanceof PsiImportModuleStatement module ? module.getModuleReference() : imp.getImportReference();
    ExpressionTree imported = imp instanceof PsiImportModuleStatement ? dottedName(name) : (ExpressionTree) reference((PsiJavaCodeReferenceElement) name);
    if (imp.isOnDemand() && !(imp instanceof PsiImportModuleStatement)) {
      imported = new MemberSelectExpressionTreeImpl(imported, last(imp, "."), new IdentifierTreeImpl(last(imp, "*")));
    }
    return new JavaTree.ImportTreeImpl(first(imp, "import"),
      imp instanceof PsiImportStaticStatement ? first(imp, "static") : null,
      imp instanceof PsiImportModuleStatement ? first(imp, "module") : null, imported, last(imp, ";"));
  }

  /** Like ECJ, keeps top-level semicolons only after an import, a type, or at the start of a file without either. */
  private void addEmptyStatement(PsiElement semicolon, List<? super EmptyStatementTreeImpl> target) {
    if (target != null) {
      target.add(new EmptyStatementTreeImpl(token(semicolon)));
    }
  }

  private ClassTreeImpl classTree(PsiClass c) {
    boolean anonymous = c instanceof PsiAnonymousClass;
    boolean implicit = c instanceof PsiImplicitClass;
    Kind kind = implicit ? Kind.IMPLICIT_CLASS : c.isAnnotationType() ? Kind.ANNOTATION_TYPE : c.isEnum() ? Kind.ENUM : c.isRecord() ? Kind.RECORD
      : c.isInterface() ? Kind.INTERFACE : Kind.CLASS;
    List<Tree> members = new ArrayList<>();
    boolean body = implicit;
    boolean enumSeparatorSeen = false;
    for (PsiElement child : c.getChildren()) {
      if (child == c.getLBrace()) {
        body = true;
      } else if (child == c.getRBrace()) {
        body = false;
      } else if (body) {
        if (child instanceof PsiEnumConstant constant) {
          members.add(enumConstant(constant));
        } else if (child instanceof PsiField field) {
          members.add(variable(field, true));
        } else if (child instanceof PsiMethod method) {
          members.add(method(method));
        } else if (child instanceof PsiClass inner) {
          members.add(classTree(inner));
        } else if (child instanceof PsiClassInitializer initializer) {
          var block = block(initializer.getBody());
          members.add(initializer.hasModifierProperty(PsiModifier.STATIC)
            ? new StaticInitializerTreeImpl(first(initializer, "static"), (InternalSyntaxToken) block.openBraceToken(), block.body(), (InternalSyntaxToken) block.closeBraceToken())
            : new BlockTreeImpl(Kind.INITIALIZER, (InternalSyntaxToken) block.openBraceToken(), block.body(), (InternalSyntaxToken) block.closeBraceToken()));
        } else if (is(child, ";")) {
          if (!c.isEnum() || enumSeparatorSeen || members.isEmpty() || !(members.getLast() instanceof EnumConstantTree)) {
            members.add(new EmptyStatementTreeImpl(token(child)));
          }
          enumSeparatorSeen = true;
        } else if (!is(child, ",")) {
          requireTrivia(child);
        }
      }
    }
    var result = new ClassTreeImpl(kind, nullableToken(c.getLBrace()), members, nullableToken(c.getRBrace()));
    if (anonymous || implicit) {
      return result;
    }
    String keyword = c.isAnnotationType() || c.isInterface() ? "interface" : c.isEnum() ? "enum" : c.isRecord() ? "record" : "class";
    result.complete(modifiers(c.getModifierList()), first(c, keyword), identifier(c.getNameIdentifier()));
    if (c.isAnnotationType()) {
      result.completeAtToken(first(c, "@"));
    }
    if (!c.isEnum() && !c.isAnnotationType()) {
      result.completeTypeParameters(typeParameters(c.getTypeParameterList()));
    }
    var extended = c.getExtendsList();
    if (extended != null && extended.getReferenceElements().length != 0) {
      if (c.isInterface()) {
        result.completeInterfaces(first(extended, "extends"), referenceList(extended, ","));
      } else {
        result.completeSuperclass(first(extended, "extends"), reference(extended.getReferenceElements()[0]));
      }
    }
    var implemented = c.getImplementsList();
    if (implemented != null && implemented.getReferenceElements().length != 0) {
      result.completeInterfaces(first(implemented, "implements"), referenceList(implemented, ","));
    }
    var permitted = c.getPermitsList();
    if (permitted != null && permitted.getReferenceElements().length != 0) {
      result.completePermittedTypes(first(permitted, "permits"), referenceList(permitted, ","));
    }
    if (c.isRecord()) {
      var header = Objects.requireNonNull(c.getRecordHeader());
      var components = header.getRecordComponents();
      List<VariableTree> converted = new ArrayList<>();
      for (int i = 0; i < components.length; i++) {
        var component = variable(components[i], false);
        if (i + 1 < components.length) {
          component.setEndToken(tokens.after(components[i], ","));
        }
        converted.add(component);
      }
      result.completeRecordComponents(first(header, "("), converted, last(header, ")"));
    }
    return result;
  }

  private MethodTreeImpl method(PsiMethod method) {
    var parameterList = children(method, PsiParameterList.class).stream().findFirst().orElse(null);
    boolean compact = parameterList == null;
    var parameters = new FormalParametersListTreeImpl(compact ? null : first(parameterList, "("), compact ? null : last(parameterList, ")"));
    PsiParameter[] declared = compact ? new PsiParameter[0] : parameterList.getParameters();
    for (int i = 0; i < declared.length; i++) {
      var parameter = variable(declared[i], false);
      if (i + 1 < declared.length) {
        parameter.setEndToken(tokens.after(declared[i], ","));
      }
      parameters.add(parameter);
    }
    TypeTree returnType = method.getReturnTypeElement() == null ? null : extraDimensions(type(method.getReturnTypeElement()), method.getNameIdentifier(), method);
    MethodTreeImpl result;
    if (method instanceof PsiAnnotationMethod annotation) {
      var value = annotation.getDefaultValue();
      result = new MethodTreeImpl(parameters, value == null ? null : tokens.before(value, "default"), value == null ? null : annotationValue(value))
        .complete(returnType, identifier(method.getNameIdentifier()), last(method, ";"));
    } else {
      var thrown = method.getThrowsList();
      var body = method.getBody();
      result = new MethodTreeImpl(returnType, identifier(method.getNameIdentifier()), parameters,
        thrown.getReferenceElements().length == 0 ? null : first(thrown, "throws"), referenceList(thrown, ","),
        body == null ? null : block(body), body == null ? last(method, ";") : null);
    }
    return result.completeWithModifiers(modifiers(method.getModifierList())).completeWithTypeParameters(typeParameters(method.getTypeParameterList()));
  }

  private VariableTreeImpl variable(PsiVariable variable, boolean terminated) {
    var name = identifier(variable.getNameIdentifier());
    var typeElement = variable.getTypeElement();
    var result = new VariableTreeImpl(name);
    if (typeElement != null && typeElement.getTextLength() != 0) {
      result.completeModifiersAndType(modifiers(variable.getModifierList()), extraDimensions(type(typeElement), variable.getNameIdentifier(), variable));
    }
    var initializer = variable.getInitializer();
    if (initializer != null) {
      result.completeTypeAndInitializer(result.type(), tokens.before(initializer, "="), expression(initializer));
    }
    if (terminated) {
      var next = tokens.next(variable);
      result.setEndToken(tokens.matches(next, ",") || tokens.matches(next, ";") ? next : last(variable, ";"));
    }
    return result;
  }

  private TypeTree extraDimensions(TypeTree base, PsiElement name, PsiElement owner) {
    List<AnnotationTreeImpl> annotations = new ArrayList<>();
    boolean afterName = false;
    for (PsiElement child : owner.getChildren()) {
      if (child == name) {
        afterName = true;
      } else if (afterName && child instanceof PsiAnnotation annotation) {
        annotations.add(annotation(annotation));
      } else if (afterName && is(child, "[")) {
        base = new JavaTree.ArrayTypeTreeImpl(base, List.copyOf(annotations), token(child), tokens.after(child, "]"));
        annotations.clear();
      } else if (afterName && (is(child, "=") || is(child, ",") || is(child, ";"))) {
        break;
      }
    }
    return base;
  }

  private EnumConstantTreeImpl enumConstant(PsiEnumConstant constant) {
    var name = identifier(constant.getNameIdentifier());
    var initializer = new NewClassTreeImpl(name, arguments(constant.getArgumentList()),
      constant.getInitializingClass() == null ? null : classTree(constant.getInitializingClass()));
    var next = tokens.next(constant);
    return new EnumConstantTreeImpl(modifiers(constant.getModifierList()), name, initializer,
      tokens.matches(next, ",") || tokens.matches(next, ";") ? next : null);
  }

  private ModifiersTreeImpl modifiers(PsiModifierList list) {
    if (list != null && modifierLists.containsKey(list)) {
      return modifierLists.get(list);
    }
    List<ModifierTree> modifiers = new ArrayList<>();
    if (list != null) {
      for (PsiElement child : list.getChildren()) {
        if (child instanceof PsiAnnotation annotation) {
          modifiers.add(annotation(annotation));
        } else if (child instanceof PsiKeyword) {
          Modifier modifier = Modifier.valueOf(child.getText().toUpperCase(Locale.ROOT).replace('-', '_'));
          modifiers.add(new ModifierKeywordTreeImpl(modifier, token(child)));
        } else {
          requireTrivia(child);
        }
      }
    }
    var result = new ModifiersTreeImpl(modifiers);
    if (list != null) {
      modifierLists.put(list, result);
    }
    return result;
  }

  private List<AnnotationTree> annotations(PsiElement list) {
    List<AnnotationTree> result = new ArrayList<>();
    if (list != null) {
      for (PsiElement child : list.getChildren()) {
        if (child instanceof PsiAnnotation annotation) {
          result.add(annotation(annotation));
        }
      }
    }
    return result;
  }

  private AnnotationTreeImpl annotation(PsiAnnotation annotation) {
    var list = annotation.getParameterList();
    var values = ArgumentListTreeImpl.emptyList();
    var attributes = list.getAttributes();
    for (int i = 0; i < attributes.length; i++) {
      var attribute = attributes[i];
      var value = annotationValue(attribute.getValue());
      if (attribute.getNameIdentifier() != null) {
        value = new AssignmentExpressionTreeImpl(Kind.ASSIGNMENT, identifier(attribute.getNameIdentifier()), first(attribute, "="), value);
      }
      values.add(value);
      if (i + 1 < attributes.length) {
        values.separators().add(tokens.after(attribute, ","));
      }
    }
    if (list.getTextLength() != 0) {
      values.complete(first(list, "("), last(list, ")"));
    }
    return new AnnotationTreeImpl(first(annotation, "@"), reference(annotation.getNameReferenceElement()), values);
  }

  private ExpressionTree annotationValue(PsiAnnotationMemberValue value) {
    if (value instanceof PsiExpression expression) {
      return expression(expression);
    }
    if (value instanceof PsiAnnotation annotation) {
      return annotation(annotation);
    }
    if (value instanceof PsiArrayInitializerMemberValue array) {
      var values = InitializerListTreeImpl.emptyList();
      for (var item : array.getInitializers()) {
        values.add(annotationValue(item));
        if (tokens.matches(tokens.next(item), ",")) {
          values.separators().add(tokens.next(item));
        }
      }
      return new NewArrayTreeImpl(List.of(), values).completeWithCurlyBraces(first(array, "{"), last(array, "}"));
    }
    throw unsupported(value);
  }

  private TypeParameterListTreeImpl typeParameters(PsiTypeParameterList list) {
    if (list == null || list.getTypeParameters().length == 0) {
      return new TypeParameterListTreeImpl();
    }
    var result = new TypeParameterListTreeImpl(first(list, "<"), tokens.closingAngle(list));
    var parameters = list.getTypeParameters();
    for (int i = 0; i < parameters.length; i++) {
      var parameter = parameters[i];
      var name = identifier(parameter.getNameIdentifier());
      name.complete(annotations(parameter.getModifierList()));
      var bounds = parameter.getExtendsList();
      result.add(new TypeParameterTreeImpl(name, bounds.getReferenceElements().length == 0 ? null : first(bounds, "extends"), referenceList(bounds, "&")));
      if (i + 1 < parameters.length) {
        result.separators().add(tokens.after(parameter, ","));
      }
    }
    return result;
  }

  private QualifiedIdentifierListTreeImpl referenceList(PsiReferenceList list, String separator) {
    var result = QualifiedIdentifierListTreeImpl.emptyList();
    if (list != null) {
      var elements = list.getReferenceElements();
      for (int i = 0; i < elements.length; i++) {
        result.add(reference(elements[i]));
        if (i + 1 < elements.length) {
          result.separators().add(tokens.after(elements[i], separator));
        }
      }
    }
    return result;
  }

  private TypeTree type(PsiTypeElement element) {
    TypeTree result = types.get(element);
    if (result == null) {
      result = convertType(element);
      types.put(element, result);
    }
    return result;
  }

  private TypeTree convertType(PsiTypeElement element) {
    List<PsiTypeElement> parts = children(element, PsiTypeElement.class);
    if (direct(element, "|") != null) {
      var alternatives = QualifiedIdentifierListTreeImpl.emptyList();
      for (int i = 0; i < parts.size(); i++) {
        alternatives.add(type(parts.get(i)));
        if (i + 1 < parts.size()) {
          alternatives.separators().add(tokens.after(parts.get(i), "|"));
        }
      }
      return new JavaTree.UnionTypeTreeImpl(alternatives);
    }
    if (direct(element, "?") != null) {
      JavaTree.WildcardTreeImpl result;
      if (parts.isEmpty()) {
        result = new JavaTree.WildcardTreeImpl(first(element, "?"));
      } else {
        boolean upper = direct(element, "extends") != null;
        result = new JavaTree.WildcardTreeImpl(upper ? Kind.EXTENDS_WILDCARD : Kind.SUPER_WILDCARD,
          first(element, upper ? "extends" : "super"), type(parts.getFirst())).complete(first(element, "?"));
      }
      result.complete(annotations(element));
      return result;
    }
    TypeTree result = null;
    List<AnnotationTreeImpl> annotations = new ArrayList<>();
    for (PsiElement child : element.getChildren()) {
      if (child instanceof PsiAnnotation annotation) {
        annotations.add(annotation(annotation));
      } else if (child instanceof PsiTypeElement nested) {
        result = type(nested);
      } else if (child instanceof PsiJavaCodeReferenceElement ref) {
        result = reference(ref);
        if (!annotations.isEmpty()) {
          ((JavaTree.AnnotatedTypeTree) result).complete(new ArrayList<>(annotations));
          annotations.clear();
        }
      } else if (is(child, "[")) {
        result = new JavaTree.ArrayTypeTreeImpl(result, List.copyOf(annotations), token(child), tokens.after(child, "]"));
        annotations.clear();
      } else if (is(child, "...")) {
        result = new JavaTree.ArrayTypeTreeImpl(result, List.copyOf(annotations), token(child));
        annotations.clear();
      } else if (child instanceof PsiKeyword keyword) {
        result = is(keyword, "var") ? new VarTypeTreeImpl(token(keyword)) : new JavaTree.PrimitiveTypeTreeImpl(token(keyword));
        ((JavaTree.AnnotatedTypeTree) result).complete(new ArrayList<>(annotations));
        annotations.clear();
      } else if (!is(child, "]")) {
        requireTrivia(child);
      }
    }
    if (result == null) {
      throw unsupported(element);
    }
    return result;
  }

  private TypeTree reference(PsiJavaCodeReferenceElement ref) {
    if (ref == null) {
      throw new IllegalArgumentException("Missing PSI reference");
    }
    var name = identifier(ref.getReferenceNameElement());
    TypeTree result;
    if (ref.getQualifier() instanceof PsiJavaCodeReferenceElement qualifier) {
      result = new MemberSelectExpressionTreeImpl((ExpressionTree) reference(qualifier), tokens.before(ref.getReferenceNameElement(), "."), name);
    } else {
      result = name;
    }
    name.complete(annotations(ref));
    var parameters = typeArguments(ref.getParameterList(), true);
    return parameters == null ? result : new JavaTree.ParameterizedTypeTreeImpl(result, parameters);
  }

  private ExpressionTree dottedName(PsiElement element) {
    ExpressionTree result = null;
    InternalSyntaxToken dot = null;
    for (var token : tokens.within(element)) {
      if (tokens.matches(token, ".")) {
        dot = token;
      } else {
        var name = new IdentifierTreeImpl(token);
        result = result == null ? name : new MemberSelectExpressionTreeImpl(result, dot, name);
      }
    }
    return Objects.requireNonNull(result);
  }

  private TypeArgumentListTreeImpl typeArguments(PsiReferenceParameterList list, boolean parameterizedType) {
    if (list == null || list.getTextLength() == 0) {
      return null;
    }
    var parameters = Arrays.stream(list.getTypeParameterElements()).filter(p -> p.getTextLength() != 0).toArray(PsiTypeElement[]::new);
    var close = parameterizedType && parameters.length != 0
      ? tokens.syntheticAtOriginalOffset(tokens.closingAnglePosition(typeEnd(parameters[parameters.length - 1])), ">") : tokens.closingAngle(list);
    var result = new TypeArgumentListTreeImpl(first(list, "<"), close);
    for (int i = 0; i < parameters.length; i++) {
      result.add(type(parameters[i]));
      if (i + 1 < parameters.length) {
        result.separators().add(tokens.after(parameters[i], ","));
      }
    }
    return result;
  }

  /** Preserves ECJ's parameterized-type end position, including its treatment of intervening trivia. */
  private int typeEnd(PsiTypeElement type) {
    if (type.getLastChild() instanceof PsiJavaCodeReferenceElement ref) {
      var parameters = ref.getParameterList().getTypeParameterElements();
      if (parameters.length != 0) {
        return tokens.closingAnglePosition(typeEnd(parameters[parameters.length - 1])) + 1;
      }
    }
    return tokens.originalOffset(type.getTextRange().getEndOffset());
  }

  private ArgumentListTreeImpl arguments(PsiExpressionList list) {
    var result = ArgumentListTreeImpl.emptyList();
    if (list != null && list.getTextLength() != 0) {
      result.complete(first(list, "("), last(list, ")"));
      var expressions = list.getExpressions();
      for (int i = 0; i < expressions.length; i++) {
        result.add(expression(expressions[i]));
        if (i + 1 < expressions.length) {
          result.separators().add(tokens.after(expressions[i], ","));
        }
      }
    }
    return result;
  }

  private ExpressionTree expression(PsiExpression e) {
    if (e == null) {
      return null;
    }
    if (e instanceof PsiMethodReferenceExpression ref) {
      var qualifier = ref.getQualifierType();
      Tree target = qualifier == null ? expression(ref.getQualifierExpression()) : type(qualifier);
      var result = new MethodReferenceTreeImpl(target, first(ref, "::"));
      result.complete(typeArguments(ref.getParameterList(), false), identifier(ref.getReferenceNameElement()));
      return result;
    }
    if (e instanceof PsiReferenceExpression ref) {
      var qualifier = ref.getQualifierExpression();
      var name = identifier(ref.getReferenceNameElement());
      return qualifier == null ? name : new MemberSelectExpressionTreeImpl(expression(qualifier), tokens.before(ref.getReferenceNameElement(), "."), name);
    }
    if (e instanceof PsiLiteralExpression literal) {
      Kind kind;
      var tokenType = literal.getFirstChild().getNode().getElementType();
      if (tokenType == JavaTokenType.NULL_KEYWORD) {
        kind = Kind.NULL_LITERAL;
      } else if (tokenType == JavaTokenType.TRUE_KEYWORD || tokenType == JavaTokenType.FALSE_KEYWORD) {
        kind = Kind.BOOLEAN_LITERAL;
      } else if (tokenType == JavaTokenType.CHARACTER_LITERAL) {
        kind = Kind.CHAR_LITERAL;
      } else if (tokenType == JavaTokenType.TEXT_BLOCK_LITERAL) {
        kind = Kind.TEXT_BLOCK;
      } else if (tokenType == JavaTokenType.STRING_LITERAL) {
        kind = Kind.STRING_LITERAL;
      } else if (tokenType == JavaTokenType.LONG_LITERAL) {
        kind = Kind.LONG_LITERAL;
      } else if (tokenType == JavaTokenType.FLOAT_LITERAL) {
        kind = Kind.FLOAT_LITERAL;
      } else if (tokenType == JavaTokenType.DOUBLE_LITERAL) {
        kind = Kind.DOUBLE_LITERAL;
      } else {
        kind = Kind.INT_LITERAL;
      }
      return new LiteralTreeImpl(kind, token(literal));
    }
    if (e instanceof PsiParenthesizedExpression p) {
      return new ParenthesizedTreeImpl(first(p, "("), expression(p.getExpression()), last(p, ")"));
    }
    if (e instanceof PsiMethodCallExpression call) {
      return new MethodInvocationTreeImpl(expression(call.getMethodExpression()), typeArguments(call.getMethodExpression().getParameterList(), false), arguments(call.getArgumentList()));
    }
    if (e instanceof PsiNewExpression creation) {
      return newExpression(creation);
    }
    if (e instanceof PsiArrayInitializerExpression array) {
      var initializers = InitializerListTreeImpl.emptyList();
      for (var value : array.getInitializers()) {
        initializers.add(expression(value));
        if (tokens.matches(tokens.next(value), ",")) {
          initializers.separators().add(tokens.next(value));
        }
      }
      return new NewArrayTreeImpl(List.of(), initializers).completeWithCurlyBraces(first(array, "{"), last(array, "}"));
    }
    if (e instanceof PsiAssignmentExpression assignment) {
      return new AssignmentExpressionTreeImpl(operator(assignment.getOperationSign().getText()), expression(assignment.getLExpression()),
        token(assignment.getOperationSign()), expression(assignment.getRExpression()));
    }
    if (e instanceof PsiPolyadicExpression polyadic) {
      var operands = polyadic.getOperands();
      ExpressionTree result = expression(operands[0]);
      for (int i = 1; i < operands.length; i++) {
        var sign = polyadic.getTokenBeforeOperand(operands[i]);
        result = new BinaryExpressionTreeImpl(operator(sign.getText()), result, token(sign), expression(operands[i]));
      }
      return result;
    }
    if (e instanceof PsiPrefixExpression prefix) {
      Kind kind = switch (prefix.getOperationSign().getText()) {
        case "+" -> Kind.UNARY_PLUS;
        case "-" -> Kind.UNARY_MINUS;
        case "!" -> Kind.LOGICAL_COMPLEMENT;
        case "~" -> Kind.BITWISE_COMPLEMENT;
        case "++" -> Kind.PREFIX_INCREMENT;
        case "--" -> Kind.PREFIX_DECREMENT;
        default -> throw unsupported(prefix);
      };
      return new InternalPrefixUnaryExpression(kind, token(prefix.getOperationSign()), expression(prefix.getOperand()));
    }
    if (e instanceof PsiPostfixExpression postfix) {
      return new InternalPostfixUnaryExpression(postfix.getOperationSign().textMatches("++") ? Kind.POSTFIX_INCREMENT : Kind.POSTFIX_DECREMENT,
        expression(postfix.getOperand()), token(postfix.getOperationSign()));
    }
    if (e instanceof PsiConditionalExpression conditional) {
      return new ConditionalExpressionTreeImpl(expression(conditional.getCondition()), first(conditional, "?"), expression(conditional.getThenExpression()),
        tokens.before(conditional.getElseExpression(), ":"), expression(conditional.getElseExpression()));
    }
    if (e instanceof PsiTypeCastExpression cast) {
      var type = cast.getCastType();
      var parts = children(type, PsiTypeElement.class);
      if (direct(type, "&") != null) {
        var bounds = QualifiedIdentifierListTreeImpl.emptyList();
        for (int i = 1; i < parts.size(); i++) {
          bounds.add(type(parts.get(i)));
          if (i + 1 < parts.size()) {
            bounds.separators().add(tokens.after(parts.get(i), "&"));
          }
        }
        return new TypeCastExpressionTreeImpl(first(cast, "("), type(parts.getFirst()), tokens.after(parts.getFirst(), "&"), bounds,
          tokens.before(cast.getOperand(), ")"), expression(cast.getOperand()));
      }
      return new TypeCastExpressionTreeImpl(first(cast, "("), type(type), tokens.before(cast.getOperand(), ")"), expression(cast.getOperand()));
    }
    if (e instanceof PsiArrayAccessExpression access) {
      return new ArrayAccessExpressionTreeImpl(expression(access.getArrayExpression()), new ArrayDimensionTreeImpl(tokens.after(access.getArrayExpression(), "["),
        expression(access.getIndexExpression()), last(access, "]")));
    }
    if (e instanceof PsiClassObjectAccessExpression classObject) {
      return new MemberSelectExpressionTreeImpl((ExpressionTree) type(classObject.getOperand()), last(classObject, "."), new IdentifierTreeImpl(last(classObject, "class")));
    }
    if (e instanceof PsiThisExpression thisExpression) {
      var keyword = new KeywordThis(last(e, "this"));
      return thisExpression.getQualifier() == null ? keyword : new MemberSelectExpressionTreeImpl((ExpressionTree) reference(thisExpression.getQualifier()), last(e, "."), keyword);
    }
    if (e instanceof PsiSuperExpression superExpression) {
      var keyword = new KeywordSuper(last(e, "super"));
      return superExpression.getQualifier() == null ? keyword : new MemberSelectExpressionTreeImpl((ExpressionTree) reference(superExpression.getQualifier()), last(e, "."), keyword);
    }
    if (e instanceof PsiInstanceOfExpression instanceOf) {
      return instanceOf.getPattern() == null
        ? new InstanceOfTreeImpl(expression(instanceOf.getOperand()), first(e, "instanceof"), type(instanceOf.getCheckType()))
        : new InstanceOfTreeImpl(expression(instanceOf.getOperand()), first(e, "instanceof"), pattern(instanceOf.getPattern()));
    }
    if (e instanceof PsiLambdaExpression lambda) {
      List<VariableTree> parameters = new ArrayList<>();
      var source = lambda.getParameterList().getParameters();
      for (int i = 0; i < source.length; i++) {
        var parameter = variable(source[i], false);
        if (i + 1 < source.length) {
          parameter.setEndToken(tokens.after(source[i], ","));
        }
        parameters.add(parameter);
      }
      var list = lambda.getParameterList();
      var body = lambda.getBody();
      boolean parenthesized = is(list.getFirstChild(), "(");
      return new LambdaExpressionTreeImpl(parenthesized ? first(list, "(") : null, parameters,
        parenthesized ? last(list, ")") : null, first(lambda, "->"), body instanceof PsiCodeBlock block ? block(block) : expression((PsiExpression) body));
    }
    if (e instanceof PsiSwitchExpression expression) {
      return new SwitchExpressionTreeImpl(first(e, "switch"), first(e, "("), expression(expression.getExpression()), tokens.before(expression.getBody(), ")"),
        first(expression.getBody(), "{"), switchGroups(expression.getBody()), last(expression.getBody(), "}"));
    }
    throw unsupported(e);
  }

  private static Kind operator(String text) {
    return switch (text) {
      case "=" -> Kind.ASSIGNMENT;
      case "+=" -> Kind.PLUS_ASSIGNMENT;
      case "-=" -> Kind.MINUS_ASSIGNMENT;
      case "*=" -> Kind.MULTIPLY_ASSIGNMENT;
      case "/=" -> Kind.DIVIDE_ASSIGNMENT;
      case "%=" -> Kind.REMAINDER_ASSIGNMENT;
      case "&=" -> Kind.AND_ASSIGNMENT;
      case "|=" -> Kind.OR_ASSIGNMENT;
      case "^=" -> Kind.XOR_ASSIGNMENT;
      case "<<=" -> Kind.LEFT_SHIFT_ASSIGNMENT;
      case ">>=" -> Kind.RIGHT_SHIFT_ASSIGNMENT;
      case ">>>=" -> Kind.UNSIGNED_RIGHT_SHIFT_ASSIGNMENT;
      case "+" -> Kind.PLUS;
      case "-" -> Kind.MINUS;
      case "*" -> Kind.MULTIPLY;
      case "/" -> Kind.DIVIDE;
      case "%" -> Kind.REMAINDER;
      case "&&" -> Kind.CONDITIONAL_AND;
      case "||" -> Kind.CONDITIONAL_OR;
      case "&" -> Kind.AND;
      case "|" -> Kind.OR;
      case "^" -> Kind.XOR;
      case "==" -> Kind.EQUAL_TO;
      case "!=" -> Kind.NOT_EQUAL_TO;
      case "<" -> Kind.LESS_THAN;
      case "<=" -> Kind.LESS_THAN_OR_EQUAL_TO;
      case ">" -> Kind.GREATER_THAN;
      case ">=" -> Kind.GREATER_THAN_OR_EQUAL_TO;
      case "<<" -> Kind.LEFT_SHIFT;
      case ">>" -> Kind.RIGHT_SHIFT;
      case ">>>" -> Kind.UNSIGNED_RIGHT_SHIFT;
      default -> throw new IllegalArgumentException("Unsupported PSI operator: " + text);
    };
  }

  private ExpressionTree newExpression(PsiNewExpression e) {
    var ref = e.getClassOrAnonymousClassReference();
    if (e.getArgumentList() != null) {
      var result = new NewClassTreeImpl(reference(ref), arguments(e.getArgumentList()), e.getAnonymousClass() == null ? null : classTree(e.getAnonymousClass()))
        .completeWithNewKeyword(first(e, "new")).completeWithTypeArguments(typeArguments(e.getTypeArgumentList(), false));
      if (e.getQualifier() != null) {
        result.completeWithEnclosingExpression(expression(e.getQualifier()));
        result.completeWithDotToken(tokens.after(e.getQualifier(), "."));
      }
      return result;
    }
    TypeTree base = ref == null ? null : reference(ref);
    List<ArrayDimensionTree> dimensions = new ArrayList<>();
    List<AnnotationTree> annotations = new ArrayList<>();
    for (PsiElement child : e.getChildren()) {
      if (child instanceof PsiKeyword keyword && !is(keyword, "new")) {
        var primitive = new JavaTree.PrimitiveTypeTreeImpl(token(child));
        primitive.complete(List.copyOf(annotations));
        annotations.clear();
        base = primitive;
      } else if (child instanceof PsiAnnotation annotation) {
        annotations.add(annotation(annotation));
      } else if (is(child, "[")) {
        PsiElement next = nextSignificant(child);
        var dimension = new ArrayDimensionTreeImpl(token(child), next instanceof PsiExpression expression ? expression(expression) : null,
          tokens.after(next instanceof PsiExpression ? next : child, "]"))
          .completeAnnotations(List.copyOf(annotations));
        annotations.clear();
        if (next instanceof PsiExpression || e.getArrayInitializer() != null) {
          dimensions.add(dimension);
        }
      }
    }
    if (base == null) {
      throw unsupported(e);
    }
    var initializer = e.getArrayInitializer();
    return initializer == null
      ? new NewArrayTreeImpl(dimensions, InitializerListTreeImpl.emptyList()).complete(base).completeWithNewKeyword(first(e, "new"))
      : ((NewArrayTreeImpl) expression(initializer)).complete(base).completeDimensions(dimensions).completeWithNewKeyword(first(e, "new"));
  }

  private BlockTreeImpl block(PsiCodeBlock block) {
    List<StatementTree> statements = new ArrayList<>();
    for (var statement : block.getStatements()) {
      addStatement(statement, statements, true);
    }
    return new BlockTreeImpl(first(block, "{"), statements, last(block, "}"));
  }

  private void addStatement(PsiStatement statement, List<StatementTree> result, boolean terminated) {
    if (statement instanceof PsiDeclarationStatement declaration) {
      for (var element : declaration.getDeclaredElements()) {
        if (element instanceof PsiVariable variable) {
          result.add(variable(variable, terminated));
        } else if (element instanceof PsiClass c) {
          result.add(classTree(c));
        } else {
          throw unsupported(element);
        }
      }
    } else if (statement instanceof PsiExpressionListStatement list) {
      for (var expression : list.getExpressionList().getExpressions()) {
        result.add(new ExpressionStatementTreeImpl(expression(expression), null));
      }
    } else if (!terminated && statement instanceof PsiExpressionStatement expression) {
      result.add(new ExpressionStatementTreeImpl(expression(expression.getExpression()), null));
    } else {
      result.add(statement(statement));
    }
  }

  private StatementTree statement(PsiStatement s) {
    if (s instanceof PsiBlockStatement block) {
      return block(block.getCodeBlock());
    }
    if (s instanceof PsiEmptyStatement) {
      return new EmptyStatementTreeImpl(first(s, ";"));
    }
    if (s instanceof PsiExpressionStatement expression) {
      return new ExpressionStatementTreeImpl(expression(expression.getExpression()), last(s, ";"));
    }
    if (s instanceof PsiReturnStatement returned) {
      return new ReturnStatementTreeImpl(first(s, "return"), expression(returned.getReturnValue()), last(s, ";"));
    }
    if (s instanceof PsiThrowStatement thrown) {
      return new ThrowStatementTreeImpl(first(s, "throw"), expression(thrown.getException()), last(s, ";"));
    }
    if (s instanceof PsiYieldStatement yielded) {
      return new YieldStatementTreeImpl(first(s, "yield"), expression(yielded.getExpression()), last(s, ";"));
    }
    if (s instanceof PsiIfStatement branch) {
      var then = branch.getThenBranch();
      var otherwise = branch.getElseBranch();
      return new IfStatementTreeImpl(first(s, "if"), first(s, "("), expression(branch.getCondition()), tokens.after(branch.getCondition(), ")"), statement(then),
        otherwise == null ? null : tokens.after(then, "else"), otherwise == null ? null : statement(otherwise));
    }
    if (s instanceof PsiWhileStatement loop) {
      return new WhileStatementTreeImpl(first(s, "while"), first(s, "("), expression(loop.getCondition()), tokens.after(loop.getCondition(), ")"), statement(loop.getBody()));
    }
    if (s instanceof PsiDoWhileStatement loop) {
      return new DoWhileStatementTreeImpl(first(s, "do"), statement(loop.getBody()), tokens.after(loop.getBody(), "while"), tokens.before(loop.getCondition(), "("),
        expression(loop.getCondition()), tokens.after(loop.getCondition(), ")"), last(s, ";"));
    }
    if (s instanceof PsiForStatement loop) {
      var init = statementList(loop.getInitialization());
      var update = statementList(loop.getUpdate());
      var semicolons = Arrays.stream(loop.getChildren()).filter(c -> is(c, ";")).toList();
      var firstSemicolon = semicolons.size() == 2 ? token(semicolons.getFirst()) : last(loop.getInitialization(), ";");
      var secondSemicolon = token(semicolons.getLast());
      return new ForStatementTreeImpl(first(s, "for"), first(s, "("), init, firstSemicolon, expression(loop.getCondition()), secondSemicolon,
        update, tokens.before(loop.getBody(), ")"), statement(loop.getBody()));
    }
    if (s instanceof PsiForeachStatement loop) {
      return new ForEachStatementImpl(first(s, "for"), first(s, "("), variable(loop.getIterationParameter(), false), first(s, ":"),
        expression(loop.getIteratedValue()), tokens.before(loop.getBody(), ")"), statement(loop.getBody()));
    }
    if (s instanceof PsiBreakStatement broken) {
      return new BreakStatementTreeImpl(first(s, "break"), broken.getLabelIdentifier() == null ? null : identifier(broken.getLabelIdentifier()), last(s, ";"));
    }
    if (s instanceof PsiContinueStatement continued) {
      return new ContinueStatementTreeImpl(first(s, "continue"), continued.getLabelIdentifier() == null ? null : identifier(continued.getLabelIdentifier()), last(s, ";"));
    }
    if (s instanceof PsiLabeledStatement labeled) {
      return new LabeledStatementTreeImpl(identifier(labeled.getLabelIdentifier()), first(s, ":"), statement(labeled.getStatement()));
    }
    if (s instanceof PsiAssertStatement assertion) {
      var result = new AssertStatementTreeImpl(first(s, "assert"), expression(assertion.getAssertCondition()), last(s, ";"));
      return assertion.getAssertDescription() == null ? result : result.complete(tokens.before(assertion.getAssertDescription(), ":"), expression(assertion.getAssertDescription()));
    }
    if (s instanceof PsiSynchronizedStatement sync) {
      return new SynchronizedStatementTreeImpl(first(s, "synchronized"), first(s, "("), expression(sync.getLockExpression()), tokens.before(sync.getBody(), ")"), block(sync.getBody()));
    }
    if (s instanceof PsiTryStatement attempt) {
      return tryStatement(attempt);
    }
    if (s instanceof PsiSwitchStatement selection) {
      return new SwitchStatementTreeImpl(first(s, "switch"), first(s, "("), expression(selection.getExpression()), tokens.before(selection.getBody(), ")"),
        first(selection.getBody(), "{"), switchGroups(selection.getBody()), last(selection.getBody(), "}"));
    }
    throw unsupported(s);
  }

  private StatementListTreeImpl statementList(PsiStatement statement) {
    var result = StatementListTreeImpl.emptyList();
    if (statement != null && !(statement instanceof PsiEmptyStatement)) {
      addStatement(statement, result, false);
      if (statement instanceof PsiExpressionListStatement expressions) {
        var values = expressions.getExpressionList().getExpressions();
        for (int i = 0; i + 1 < values.length; i++) {
          result.separators().add(tokens.after(values[i], ","));
        }
      } else if (statement instanceof PsiDeclarationStatement declaration) {
        var elements = declaration.getDeclaredElements();
        for (int i = 0; i + 1 < elements.length; i++) {
          ((VariableTreeImpl) result.get(i)).setEndToken(tokens.after(elements[i], ","));
        }
      }
    }
    return result;
  }

  private TryStatementTreeImpl tryStatement(PsiTryStatement s) {
    var resources = ResourceListTreeImpl.emptyList();
    var list = s.getResourceList();
    if (list != null) {
      for (PsiResourceListElement resource : list) {
        if (resource instanceof PsiResourceVariable variable) {
          resources.add(variable(variable, false));
        } else if (resource instanceof PsiResourceExpression expression) {
          resources.add(expression(expression.getExpression()));
        } else {
          throw unsupported(resource);
        }
        if (tokens.matches(tokens.next(resource), ";")) {
          resources.separators().add(tokens.next(resource));
        }
      }
    }
    List<CatchTree> catches = new ArrayList<>();
    for (var section : s.getCatchSections()) {
      catches.add(new CatchTreeImpl(first(section, "catch"), first(section, "("), variable(section.getParameter(), false),
        tokens.before(section.getCatchBlock(), ")"), block(section.getCatchBlock())));
    }
    return new TryStatementTreeImpl(first(s, "try"), list == null ? null : first(list, "("), resources, list == null ? null : last(list, ")"), block(s.getTryBlock()), catches,
      s.getFinallyBlock() == null ? null : tokens.before(s.getFinallyBlock(), "finally"), s.getFinallyBlock() == null ? null : block(s.getFinallyBlock()));
  }

  private List<CaseGroupTreeImpl> switchGroups(PsiCodeBlock block) {
    List<CaseGroupTreeImpl> groups = new ArrayList<>();
    List<CaseLabelTreeImpl> labels = new ArrayList<>();
    var body = StatementListTreeImpl.emptyList();
    for (var statement : block.getStatements()) {
      if (statement instanceof PsiSwitchLabelStatementBase label) {
        if (!body.isEmpty()) {
          groups.add(new CaseGroupTreeImpl(labels, body));
          labels = new ArrayList<>();
          body = StatementListTreeImpl.emptyList();
        }
        List<ExpressionTree> values = new ArrayList<>();
        if (label.getCaseLabelElementList() != null) {
          for (var element : label.getCaseLabelElementList().getElements()) {
            ExpressionTree value;
            if (element instanceof PsiPattern pattern) {
              value = pattern(pattern);
              if (label.getGuardExpression() != null) {
                value = new GuardedPatternTreeImpl((PatternTree) value, tokens.before(label.getGuardExpression(), "when"), expression(label.getGuardExpression()), SemanticValues.UNKNOWN_TYPE);
              }
            } else if (element instanceof PsiDefaultCaseLabelElement) {
              value = new DefaultPatternTreeImpl(token(element), SemanticValues.UNKNOWN_TYPE);
            } else {
              value = expression((PsiExpression) element);
              if (value.is(Kind.NULL_LITERAL)) {
                value = new NullPatternTreeImpl((LiteralTreeImpl) value);
              }
            }
            values.add(value);
          }
        }
        labels.add(new CaseLabelTreeImpl(first(label, label.isDefaultCase() ? "default" : "case"), values,
          label instanceof PsiSwitchLabeledRuleStatement ? first(label, "->") : last(label, ":")));
        if (label instanceof PsiSwitchLabeledRuleStatement rule) {
          var ruleBody = rule.getBody();
          if (ruleBody instanceof PsiExpressionStatement expression) {
            body.add(new YieldStatementTreeImpl(null, expression(expression.getExpression()), last(ruleBody, ";")));
          } else {
            addStatement(ruleBody, body, true);
          }
        }
      } else {
        addStatement(statement, body, true);
      }
    }
    if (!labels.isEmpty()) {
      groups.add(new CaseGroupTreeImpl(labels, body));
    }
    return groups;
  }

  private PatternTree pattern(PsiPattern pattern) {
    if (pattern instanceof PsiTypeTestPattern test) {
      return new TypePatternTreeImpl(variable(test.getPatternVariable(), false), SemanticValues.UNKNOWN_TYPE);
    }
    if (pattern instanceof PsiDeconstructionPattern record) {
      List<PatternTree> components = Arrays.stream(record.getDeconstructionList().getDeconstructionComponents()).map(this::pattern).toList();
      return new RecordPatternTreeImpl(type(record.getTypeElement()), first(record.getDeconstructionList(), "("), components, last(record.getDeconstructionList(), ")"));
    }
    if (pattern instanceof PsiUnnamedPattern unnamed) {
      var variable = new VariableTreeImpl(new IdentifierTreeImpl(last(unnamed, "_"), true));
      if (unnamed.getTypeElement().getTextLength() != 0) {
        variable.completeType(type(unnamed.getTypeElement()));
      }
      return new TypePatternTreeImpl(variable, SemanticValues.UNKNOWN_TYPE);
    }
    throw unsupported(pattern);
  }

  private ModuleDeclarationTree module(PsiJavaModule module) {
    if (module == null) {
      return null;
    }
    List<ModuleDirectiveTree> directives = new ArrayList<>();
    for (PsiElement child : module.getChildren()) {
      if (child instanceof PsiRequiresStatement requires) {
        var reference = requires.getReferenceElement();
        var modifiers = modifiers(requires.getModifierList());
        ModuleNameTreeImpl name;
        if (reference == null) {
          var sourceTokens = tokens.within(requires);
          var nameToken = sourceTokens.get(sourceTokens.size() - 2);
          if (!tokens.matches(nameToken, "transitive")) {
            throw unsupported(requires);
          }
          name = ModuleNameTreeImpl.emptyList();
          name.add(new IdentifierTreeImpl(nameToken));
          modifiers = new ModifiersTreeImpl(modifiers.stream().filter(m -> m.firstToken() != nameToken).toList());
        } else {
          name = moduleName(reference);
        }
        directives.add(new RequiresDirectiveTreeImpl(first(requires, "requires"), modifiers, name, last(requires, ";")));
      } else if (child instanceof PsiPackageAccessibilityStatement access) {
        var names = ModuleNameListTreeImpl.emptyList();
        for (var ref : access.getModuleReferences()) {
          if (!names.isEmpty()) {
            names.separators().add(tokens.before(ref, ","));
          }
          names.add(moduleName(ref));
        }
        var to = names.isEmpty() ? null : first(access, "to");
        directives.add(access.getRole() == PsiPackageAccessibilityStatement.Role.EXPORTS
          ? new ExportsDirectiveTreeImpl(first(access, "exports"), (ExpressionTree) reference(access.getPackageReference()), to, names, last(access, ";"))
          : new OpensDirectiveTreeImpl(first(access, "opens"), (ExpressionTree) reference(access.getPackageReference()), to, names, last(access, ";")));
      } else if (child instanceof PsiUsesStatement uses) {
        directives.add(new UsesDirectiveTreeImpl(first(uses, "uses"), reference(uses.getClassReference()), last(uses, ";")));
      } else if (child instanceof PsiProvidesStatement provides) {
        directives.add(new ProvidesDirectiveTreeImpl(first(provides, "provides"), reference(provides.getInterfaceReference()), first(provides, "with"),
          referenceList(provides.getImplementationList(), ","), last(provides, ";")));
      } else if (!(child instanceof PsiJavaToken) && child != module.getModifierList() && child != module.getNameIdentifier()) {
        requireTrivia(child);
      }
    }
    return new ModuleDeclarationTreeImpl(annotations(module.getModifierList()), module.hasModifierProperty(PsiModifier.OPEN) ? first(module, "open") : null,
      first(module, "module"), moduleName(module.getNameIdentifier()), first(module, "{"), directives, last(module, "}"));
  }

  private ModuleNameTreeImpl moduleName(PsiElement element) {
    var result = ModuleNameTreeImpl.emptyList();
    var name = dottedName(element);
    addModuleName(name, result);
    return result;
  }

  private void addModuleName(ExpressionTree name, ModuleNameTreeImpl result) {
    if (name instanceof MemberSelectExpressionTree member) {
      addModuleName(member.expression(), result);
      result.add(member.identifier());
    } else {
      result.add((IdentifierTree) name);
    }
  }

  private IdentifierTreeImpl identifier(PsiElement element) {
    return new IdentifierTreeImpl(token(element), element.textMatches("_"));
  }

  private InternalSyntaxToken token(PsiElement element) {
    return tokens.token(Objects.requireNonNull(element));
  }

  private InternalSyntaxToken nullableToken(PsiElement element) {
    return element == null ? null : token(element);
  }

  private InternalSyntaxToken first(PsiElement element, String text) {
    var direct = direct(element, text);
    return direct == null ? tokens.first(element, text) : token(direct);
  }

  private InternalSyntaxToken last(PsiElement element, String text) {
    return tokens.last(element, text);
  }

  private static boolean is(PsiElement element, String text) {
    return element instanceof PsiJavaToken && element.textMatches(text);
  }

  private static PsiElement direct(PsiElement element, String text) {
    return Arrays.stream(element.getChildren()).filter(c -> is(c, text)).findFirst().orElse(null);
  }

  private static <T> List<T> children(PsiElement element, Class<T> type) {
    return Arrays.stream(element.getChildren()).filter(type::isInstance).map(type::cast).toList();
  }

  private static PsiElement nextSignificant(PsiElement element) {
    PsiElement next = element.getNextSibling();
    while (next instanceof PsiWhiteSpace || next instanceof PsiComment) {
      next = next.getNextSibling();
    }
    return next;
  }

  private static void requireTrivia(PsiElement element) {
    if (!(element instanceof PsiWhiteSpace) && !(element instanceof PsiComment) && element.getTextLength() != 0) {
      throw unsupported(element);
    }
  }

  private static IllegalArgumentException unsupported(PsiElement element) {
    return new IllegalArgumentException("Unsupported PSI syntax " + (element == null ? "null" : element.getNode().getElementType() + " at " + element.getTextRange()));
  }
}
