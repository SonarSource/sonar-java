/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.java.checks;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.AstNodeType;
import com.sonar.sslr.squid.SquidAstVisitor;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ClassBytecodeProviderAwareVisitor;
import org.sonar.java.SourceAndBytecodeVisitor;
import org.sonar.java.ast.api.JavaTokenType;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.java.bytecode.asm.AsmClass;
import org.sonar.java.bytecode.asm.AsmClassProvider;
import org.sonar.java.bytecode.asm.AsmClassProvider.DETAIL_LEVEL;
import org.sonar.java.bytecode.asm.AsmField;
import org.sonar.java.bytecode.visitor.BytecodeVisitor;
import org.sonar.squid.api.CheckMessage;
import org.sonar.squid.api.SourceFile;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Rule(
  key = "HiddenFieldCheck",
  priority = Priority.MAJOR)
@BelongsToProfile(title = "Sonar way", priority = Priority.MAJOR)
public class HiddenFieldCheck extends BytecodeVisitor implements SourceAndBytecodeVisitor, ClassBytecodeProviderAwareVisitor {

  private final Map<String, Multimap<String, Integer>> localVariableOccurencesByClassName = Maps.newHashMap();
  private AsmClassProvider classProvider;

  @Override
  public SquidAstVisitor getSourceVisitor() {
    return new SquidAstVisitor<LexerlessGrammar>() {

      @Override
      public void init() {
        subscribeTo(
            JavaGrammar.LOCAL_VARIABLE_DECLARATION_STATEMENT,
            JavaGrammar.VARIABLE_DECLARATOR_ID,
            JavaGrammar.FOR_INIT);
      }

      @Override
      public void visitFile(AstNode node) {
        localVariableOccurencesByClassName.clear();
      }

      @Override
      public void visitNode(AstNode astNode) {
        if (astNode.is(JavaGrammar.VARIABLE_DECLARATOR_ID)) {
          saveLocalVariableOccurrence(astNode.getFirstChild(JavaTokenType.IDENTIFIER));
        } else {
          AstNode variableDeclarators = astNode.getFirstChild(JavaGrammar.VARIABLE_DECLARATORS);
          if (variableDeclarators != null) {
            for (AstNode variableDeclarator : variableDeclarators.getChildren(JavaGrammar.VARIABLE_DECLARATOR)) {
              saveLocalVariableOccurrence(variableDeclarator.getFirstChild(JavaTokenType.IDENTIFIER));
            }
          }
        }
      }

      private void saveLocalVariableOccurrence(AstNode identifier) {
        if (!isInConstructorOrSetter(identifier)) {
          String className = getClassName(identifier);
          if (!localVariableOccurencesByClassName.containsKey(className)) {
            localVariableOccurencesByClassName.put(className, HashMultimap.<String, Integer> create());
          }
          localVariableOccurencesByClassName.get(className).put(identifier.getTokenOriginalValue(), identifier.getTokenLine());
        }
      }

      private boolean isInConstructorOrSetter(AstNode node) {
        AstNode ancestor = getFirstAncestor(node, JavaGrammar.CLASS_INIT_DECLARATION, JavaGrammar.CLASS_BODY_DECLARATION);
        return ancestor != null && (isConstructor(ancestor) || isSetter(ancestor));
      }

      private boolean isConstructor(AstNode node) {
        AstNode memberDecl = getActualMemberDecl(node);

        return node.is(JavaGrammar.CLASS_BODY_DECLARATION) &&
          memberDecl != null &&
          memberDecl.hasDirectChildren(JavaGrammar.CONSTRUCTOR_DECLARATOR_REST);
      }

      private boolean isSetter(AstNode node) {
        AstNode memberDecl = getActualMemberDecl(node);

        return node.is(JavaGrammar.CLASS_BODY_DECLARATION) &&
          memberDecl != null &&
          memberDecl.getFirstChild(JavaTokenType.IDENTIFIER).getTokenOriginalValue().startsWith("set");
      }

      private AstNode getActualMemberDecl(AstNode node) {
        AstNode memberDecl = node.getFirstChild(JavaGrammar.MEMBER_DECL);
        if (memberDecl == null) {
          return null;
        }

        AstNode genericMethodOrConstructor = memberDecl.getFirstChild(JavaGrammar.GENERIC_METHOD_OR_CONSTRUCTOR_REST);
        return genericMethodOrConstructor == null ? memberDecl : genericMethodOrConstructor;
      }

    };
  }

  @Override
  public void setClassProvider(AsmClassProvider classProvider) {
    this.classProvider = classProvider;
  }

  @Override
  public void visitClass(AsmClass asmClass) {
    SourceFile file = getSourceFile(asmClass);
    String className = getClassName(asmClass);
    Collection<AsmField> fields = getAllFields(asmClass);

    if (localVariableOccurencesByClassName.containsKey(className)) {
      for (String localVariable : localVariableOccurencesByClassName.get(className).keySet()) {
        for (AsmField field : fields) {
          if (localVariable.equals(field.getName())) {
            for (Integer line : localVariableOccurencesByClassName.get(className).get(localVariable)) {
              CheckMessage message = new CheckMessage(this, "Rename this variable/parameter which hides a field of '" + field.getParent().getDisplayName() + "'.");
              message.setLine(line);
              file.log(message);
            }

            break;
          }
        }
      }
    }
  }

  private Collection<AsmField> getAllFields(AsmClass asmClass) {
    ImmutableSet.Builder<AsmField> builder = ImmutableSet.builder();

    addClassOwnFields(builder, asmClass);
    addClassInheritedFields(builder, asmClass);
    addClassParentClassesFields(builder, asmClass);

    return builder.build();
  }

  private static void addClassOwnFields(ImmutableSet.Builder<AsmField> builder, AsmClass asmClass) {
    for (AsmField field : asmClass.getFields()) {
      builder.add(field);
    }
  }

  private static void addClassInheritedFields(ImmutableSet.Builder<AsmField> builder, AsmClass asmClass) {
    for (AsmClass superClass = asmClass.getSuperClass(); superClass != null; superClass = superClass.getSuperClass()) {
      for (AsmField field : superClass.getFields()) {
        if (!field.isPrivate()) {
          builder.add(field);
        }
      }
    }
  }

  private void addClassParentClassesFields(ImmutableSet.Builder<AsmField> builder, AsmClass asmClass) {
    for (AsmClass parentClass : getParentClasses(asmClass)) {
      for (AsmField field : parentClass.getFields()) {
        builder.add(field);
      }
    }
  }

  private List<AsmClass> getParentClasses(AsmClass asmClass) {
    ImmutableList.Builder<AsmClass> builder = ImmutableList.builder();

    for (AsmClass parentClass = getParentClass(asmClass); parentClass != null; parentClass = getParentClass(parentClass)) {
      builder.add(parentClass);
    }

    return builder.build();
  }

  private AsmClass getParentClass(AsmClass asmClass) {
    String internalName = asmClass.getInternalName();
    int lastDollar = internalName.lastIndexOf('$');

    return lastDollar == -1 ? null : classProvider.getClass(internalName.substring(0, lastDollar), DETAIL_LEVEL.STRUCTURE);
  }

  private static String getClassName(AstNode node) {
    return getFirstAncestor(node, JavaGrammar.CLASS_DECLARATION, JavaGrammar.ENUM_DECLARATION, JavaGrammar.INTERFACE_DECLARATION)
        .getFirstChild(JavaTokenType.IDENTIFIER).getTokenOriginalValue();
  }

  private static String getClassName(AsmClass asmClass) {
    return lastInnerClass(removePackages(asmClass.getInternalName()));
  }

  private static String removePackages(String internalName) {
    int lastSlash = internalName.lastIndexOf('/');

    return lastSlash == -1 ? internalName : internalName.substring(lastSlash + 1);
  }

  private static String lastInnerClass(String internalName) {
    int lastDollar = internalName.lastIndexOf('$');

    return lastDollar == -1 ? internalName : internalName.substring(lastDollar + 1);
  }

  private static AstNode getFirstAncestor(AstNode node, AstNodeType... types) {
    for (AstNode ancestor = node.getParent(); ancestor != null; ancestor = ancestor.getParent()) {
      for (AstNodeType type : types) {
        if (ancestor.getType() == type) {
          return ancestor;
        }
      }
    }

    return null;
  }

}
