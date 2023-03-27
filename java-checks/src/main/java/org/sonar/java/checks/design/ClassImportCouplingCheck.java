/*
 * SonarQube Java
 * Copyright (C) 2012-2023 SonarSource SA
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
package org.sonar.java.checks.design;

import org.sonar.check.Rule;
import org.sonar.check.RuleProperty;
import org.sonar.java.checks.helpers.MethodTreeUtils;
import org.sonar.java.model.ExpressionUtils;
import org.sonar.java.model.JavaTree;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.model.expression.IdentifierTreeImpl;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.ArrayTypeTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.CatchTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.InstanceOfTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.NewArrayTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.ParameterizedTypeTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.TypeCastTree;
import org.sonar.plugins.java.api.tree.TypeParameterTree;
import org.sonar.plugins.java.api.tree.TypeTree;
import org.sonar.plugins.java.api.tree.UnionTypeTree;
import org.sonar.plugins.java.api.tree.VariableTree;
import org.sonar.plugins.java.api.tree.WildcardTree;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Rule(key = "S6539")
public class ClassImportCouplingCheck extends BaseTreeVisitor implements JavaFileScanner {

    // TODO change to 20, 3 is for testing purposes
    private static final int COUPLING_THRESHOLD = 3;

    @RuleProperty(
            key = "couplingThreshold",
            description = "Maximum number of classes a single class is allowed to depend upon",
            defaultValue = "" + COUPLING_THRESHOLD)
    public int couplingThreshold = COUPLING_THRESHOLD;
    private String packageName;
    private final Deque<Set<String>> nesting = new LinkedList<>();
    private Set<String> types;

    private JavaFileScannerContext context;

    @Override
    public void scanFile(JavaFileScannerContext context) {
        this.context = context;
        scan(context.getTree());
    }

    @Override
    public void visitClass(ClassTree tree) {
        // if class is utility class -> don't report
        if (isUtilityClass(tree) || isPrivateInnerClass(tree)) {
            return;
        }

        if (tree.is(Tree.Kind.CLASS) && tree.simpleName() != null) {
            nesting.push(types);
            types = new HashSet<>();
        }
        CompilationUnitTree compilationUnitTree = (CompilationUnitTree) ExpressionUtils.getParentOfType(tree, Tree.Kind.COMPILATION_UNIT);
        packageName = JavaTree.PackageDeclarationTreeImpl.packageNameAsString(compilationUnitTree.packageDeclaration());

        Set<String> imports = compilationUnitTree.imports().stream()
                .map(ImportTree.class::cast)
                .map(ImportTree::qualifiedIdentifier)
                .map(this::helper)
                .collect(Collectors.toSet());

        Set<String> filteredImports = imports.stream()
                .filter(i -> !i.startsWith("java."))
                .collect(Collectors.toSet());

        checkTypes(tree.superClass());
        checkTypes((List<? extends Tree>) tree.superInterfaces());
        super.visitClass(tree);

        filteredImports.addAll(types);
        int size = filteredImports.size();
        if (size > couplingThreshold) {
            context.reportIssue(this, tree, "Split this class into smaller and more specialized ones to reduce its dependencies on other classes from " +
                    size + " to the maximum authorized " + couplingThreshold + " or less.");
        }
        types = nesting.pop();
    }

    @Override
    public void visitVariable(VariableTree tree) {
        checkTypes(tree.type());
        super.visitVariable(tree);
    }

    @Override
    public void visitCatch(CatchTree tree) {
        //skip visit catch parameter for backward compatibility
        scan(tree.block());
    }

    @Override
    public void visitTypeCast(TypeCastTree tree) {
        checkTypes(tree.type());
        super.visitTypeCast(tree);
    }

    @Override
    public void visitMethod(MethodTree tree) {
        checkTypes(tree.returnType());
        super.visitMethod(tree);
    }

    @Override
    public void visitTypeParameter(TypeParameterTree typeParameter) {
        checkTypes((List<? extends Tree>) typeParameter.bounds());
        checkTypes(typeParameter.identifier());
        super.visitTypeParameter(typeParameter);
    }

    @Override
    public void visitUnionType(UnionTypeTree tree) {
        // can not be visited because of visitCatch excluding exceptions
        checkTypes((List<? extends Tree>) tree.typeAlternatives());
        super.visitUnionType(tree);
    }

    @Override
    public void visitParameterizedType(ParameterizedTypeTree tree) {
        checkTypes(tree.type());
        checkTypes((List<TypeTree>) tree.typeArguments());
        super.visitParameterizedType(tree);
    }

    @Override
    public void visitNewClass(NewClassTree tree) {
        if (tree.typeArguments() != null) {
            checkTypes((List<TypeTree>) tree.typeArguments());
        }
        if (tree.identifier().is(Tree.Kind.PARAMETERIZED_TYPE)) {
            scan(tree.enclosingExpression());
            checkTypes((List<TypeTree>) ((ParameterizedTypeTree) tree.identifier()).typeArguments());
            scan(tree.typeArguments());
            scan(tree.arguments());
            scan(tree.classBody());
        } else {
            super.visitNewClass(tree);
        }
    }

    @Override
    public void visitWildcard(WildcardTree tree) {
        checkTypes(tree.bound());
        super.visitWildcard(tree);
    }

    @Override
    public void visitArrayType(ArrayTypeTree tree) {
        checkTypes(tree.type());
        super.visitArrayType(tree);
    }

    @Override
    public void visitInstanceOf(InstanceOfTree tree) {
        checkTypes(tree.type());
        super.visitInstanceOf(tree);
    }

    @Override
    public void visitNewArray(NewArrayTree tree) {
        checkTypes(tree.type());
        super.visitNewArray(tree);
    }

    private void checkTypes(List<? extends Tree> types) {
        for (Tree type : types) {
            checkTypes(type);
        }
    }

    private void checkTypes(@Nullable Tree type) {
        if (type == null || types == null) {
            return;
        }
        if (type.is(Tree.Kind.IDENTIFIER)) {
            IdentifierTreeImpl identifierTree = (IdentifierTreeImpl) type;
            String name = identifierTree.typeBinding.getPackage().getName();
            if (name.contains(packageName)) {
                types.add(((IdentifierTree) type).name());
            }
        } else if (type.is(Tree.Kind.MEMBER_SELECT)) {
            Deque<String> fullyQualifiedNameComponents = new ArrayDeque<>();
            ExpressionTree expr = (ExpressionTree) type;
            while (expr.is(Tree.Kind.MEMBER_SELECT)) {
                MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
                IdentifierTreeImpl identifierTree = (IdentifierTreeImpl) mse.identifier();
                String name = identifierTree.typeBinding.getPackage().getName();
                if (name.contains(packageName)) {
                    fullyQualifiedNameComponents.push(mse.identifier().name());
                }
                expr = mse.expression();
            }
            if (expr.is(Tree.Kind.IDENTIFIER)) {
                IdentifierTreeImpl identifierTree = (IdentifierTreeImpl) type;
                String name = identifierTree.typeBinding.getPackage().getName();
                if (name.contains(packageName)) {
                    fullyQualifiedNameComponents.push(identifierTree.name());
                }
            }
            types.add(String.join(".", fullyQualifiedNameComponents));
        }
    }

    private String helper(Tree tree) {
        List<String> packageName = new ArrayList<>();
        MemberSelectExpressionTree memberSelect = (MemberSelectExpressionTree) tree;
        while (true) {
            memberSelect.expression();
            packageName.add(memberSelect.identifier().name());
            try {
                memberSelect = (MemberSelectExpressionTree) memberSelect.expression();
            } catch (ClassCastException e) {
                packageName.add(((IdentifierTree) memberSelect.expression()).name());
                break;
            }
        }
        Collections.reverse(packageName);
        return String.join(".", packageName);
    }


    // TODO: remove to external class to be used with S1118
    private boolean isPrivateInnerClass(ClassTree classTree) {
        return !classTree.symbol().owner().isPackageSymbol() &&
                ModifiersUtils.hasModifier(classTree.modifiers(), Modifier.PRIVATE);
    }

    private boolean isUtilityClass(ClassTree classTree) {
        return hasOnlyStaticMembers(classTree) && !anonymousClass(classTree) && !extendsAnotherClassOrImplementsSerializable(classTree)
                && !containsMainMethod(classTree);
    }

    private boolean containsMainMethod(ClassTree classTree) {
        return classTree.members().stream()
                .filter(member -> member.is(Tree.Kind.METHOD))
                .anyMatch(method -> MethodTreeUtils.isMainMethod((MethodTree) method));
    }

    private boolean hasOnlyStaticMembers(ClassTree classTree) {
        List<Tree> members = classTree.members();
        if (noStaticMember(members)) {
            return false;
        }
        return members.stream().allMatch(member -> isConstructor(member) || isStatic(member) || member.is(Tree.Kind.EMPTY_STATEMENT));
    }

    private boolean anonymousClass(ClassTree classTree) {
        return classTree.simpleName() == null;
    }

    private boolean extendsAnotherClassOrImplementsSerializable(ClassTree classTree) {
        return classTree.superClass() != null || classTree.symbol().type().isSubtypeOf("java.io.Serializable");
    }

    private boolean noStaticMember(List<Tree> members) {
        return members.stream().noneMatch(this::isStatic);
    }

    private boolean isStatic(Tree member) {
        if (member.is(Tree.Kind.STATIC_INITIALIZER)) {
            return true;
        }
        if (member.is(Tree.Kind.VARIABLE)) {
            VariableTree variableTree = (VariableTree) member;
            return hasStaticModifier(variableTree.modifiers());
        } else if (member.is(Tree.Kind.METHOD)) {
            MethodTree methodTree = (MethodTree) member;
            return hasStaticModifier(methodTree.modifiers());
        } else if (isClassTree(member)) {
            ClassTree classTree = (ClassTree) member;
            return hasStaticModifier(classTree.modifiers());
        }
        return false;
    }

    private boolean isClassTree(Tree member) {
        return member.is(Tree.Kind.CLASS) || member.is(Tree.Kind.ANNOTATION_TYPE) || member.is(Tree.Kind.INTERFACE) || member.is(Tree.Kind.ENUM);
    }

    private boolean hasStaticModifier(ModifiersTree modifiers) {
        return ModifiersUtils.hasModifier(modifiers, Modifier.STATIC);
    }

    private boolean isConstructor(Tree tree) {
        return tree.is(Tree.Kind.CONSTRUCTOR);
    }

}
