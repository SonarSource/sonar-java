/*
 * SonarQube Java
 * Copyright (C) 2012-2021 SonarSource SA
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
package org.sonar.java.ast.visitors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.java.SonarComponents;
import org.sonar.java.ast.api.JavaKeyword;
import org.sonar.java.ast.api.JavaRestrictedKeyword;
import org.sonar.java.model.ModifiersUtils;
import org.sonar.java.model.declaration.ClassTreeImpl;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.location.Range;
import org.sonar.plugins.java.api.tree.SyntaxToken;
import org.sonar.plugins.java.api.tree.SyntaxTrivia;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.YieldStatementTree;

public class SyntaxHighlighterVisitor extends SubscriptionVisitor {

  private final SonarComponents sonarComponents;
  private final Map<Tree.Kind, TypeOfText> typesByKind;
  private final Set<String> keywords;
  private final Set<String> restrictedKeywords;

  private NewHighlighting highlighting;
  private boolean withinModule = false;

  public SyntaxHighlighterVisitor(SonarComponents sonarComponents) {
    this.sonarComponents = sonarComponents;

    keywords = Collections.unmodifiableSet(Arrays.stream(JavaKeyword.keywordValues()).collect(Collectors.toSet()));
    restrictedKeywords = Collections.unmodifiableSet(Arrays.stream(JavaRestrictedKeyword.restrictedKeywordValues()).collect(Collectors.toSet()));

    Map<Tree.Kind, TypeOfText> typesByKindMap = new EnumMap<>(Tree.Kind.class);
    typesByKindMap.put(Tree.Kind.STRING_LITERAL, TypeOfText.STRING);
    typesByKindMap.put(Tree.Kind.TEXT_BLOCK, TypeOfText.STRING);
    typesByKindMap.put(Tree.Kind.CHAR_LITERAL, TypeOfText.STRING);
    typesByKindMap.put(Tree.Kind.FLOAT_LITERAL, TypeOfText.CONSTANT);
    typesByKindMap.put(Tree.Kind.DOUBLE_LITERAL, TypeOfText.CONSTANT);
    typesByKindMap.put(Tree.Kind.LONG_LITERAL, TypeOfText.CONSTANT);
    typesByKindMap.put(Tree.Kind.INT_LITERAL, TypeOfText.CONSTANT);
    typesByKindMap.put(Tree.Kind.ANNOTATION, TypeOfText.ANNOTATION);
    typesByKindMap.put(Tree.Kind.VAR_TYPE, TypeOfText.KEYWORD);
    this.typesByKind = Collections.unmodifiableMap(typesByKindMap);
  }

  @Override
  public List<Tree.Kind> nodesToVisit() {
    List<Tree.Kind> list = new ArrayList<>(typesByKind.keySet());
    list.add(Tree.Kind.TOKEN);
    list.add(Tree.Kind.TRIVIA);
    // modules have their own set of restricted keywords
    list.add(Tree.Kind.MODULE);
    // 'yield' is a restricted keyword
    list.add(Tree.Kind.YIELD_STATEMENT);
    // 'record' is a restricted keyword
    list.add(Tree.Kind.RECORD);
    // sealed classes comes with restricted keyword 'permits', applying on classes and interfaces
    list.add(Tree.Kind.CLASS);
    list.add(Tree.Kind.INTERFACE);
    // sealed classes comes with restricted modifiers 'sealed' and 'non-sealed', applying on classes and interfaces
    list.add(Tree.Kind.MODIFIERS);
    return Collections.unmodifiableList(list);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    highlighting = sonarComponents.highlightableFor(context.getInputFile());

    super.scanFile(context);

    highlighting.save();
  }

  @Override
  public void visitNode(Tree tree) {
    switch (tree.kind()) {
      case MODULE:
        withinModule = true;
        return;
      case ANNOTATION:
        AnnotationTree annotationTree = (AnnotationTree) tree;
        highlight(annotationTree.atToken(), annotationTree.annotationType(), typesByKind.get(Tree.Kind.ANNOTATION));
        return;
      case YIELD_STATEMENT:
        // 'yield' is a 'restricted identifier' (JSL16, $3.9) only acting as keyword in a yield statement
        Optional.ofNullable(((YieldStatementTree) tree).yieldKeyword()).ifPresent(yieldKeyword -> highlight(yieldKeyword, TypeOfText.KEYWORD));
        return;
      case RECORD:
        // 'record' is a 'restricted identifier' (JSL16, $3.9) only acting as keyword in a record declaration
        highlight(((ClassTree) tree).declarationKeyword(), TypeOfText.KEYWORD);
        return;
      case CLASS:
      case INTERFACE:
        // 'permits' is a 'restricted identifier' (JSL16, $3.9) only acting as keyword in a class/interface declaration
        Optional.ofNullable(((ClassTree) tree).permitsKeyword()).ifPresent(permitsKeyword -> highlight(permitsKeyword, TypeOfText.KEYWORD));
        return;
      case MODIFIERS:
        // 'sealed' and 'non-sealed' are 'restricted identifier' (JSL16, $3.9) only acting as keyword in a class declaration
        ModifiersTree modifiers = (ModifiersTree) tree;
        ModifiersUtils.findModifier(modifiers, Modifier.SEALED).ifPresent(modifier -> highlight(modifier, TypeOfText.KEYWORD));
        ModifiersUtils.findModifier(modifiers, Modifier.NON_SEALED).ifPresent(modifier -> highlight(modifier, TypeOfText.KEYWORD));
        return;
      default:
        highlight(tree, typesByKind.get(tree.kind()));
        return;
    }
  }

  @Override
  public void leaveNode(Tree tree) {
    if (tree.is(Tree.Kind.MODULE)) {
      withinModule = false;
    }
  }

  private void highlight(Tree tree, TypeOfText typeOfText) {
    highlight(tree, tree, typeOfText);
  }

  private void highlight(Tree from, Tree to, TypeOfText typeOfText) {
    Range first = from.firstToken().range();
    Range last = to.lastToken().range();
    highlighting.highlight(
      first.start().line(), first.start().columnOffset(),
      last.end().line(), last.end().columnOffset(), typeOfText);
  }

  @Override
  public void visitToken(SyntaxToken syntaxToken) {
    String text = syntaxToken.text();
    if (keywords.contains(text)) {
      if (isInterfaceOfAnnotationType(syntaxToken)) {
        ClassTreeImpl annotationType = (ClassTreeImpl) syntaxToken.parent();
        highlight(annotationType.atToken(), annotationType.declarationKeyword(), TypeOfText.KEYWORD);
      } else {
        highlight(syntaxToken, TypeOfText.KEYWORD);
      }
    } else if (isRestrictedKeyword(syntaxToken)) {
      highlight(syntaxToken, TypeOfText.KEYWORD);
    }
  }

  private static boolean isInterfaceOfAnnotationType(SyntaxToken syntaxToken) {
    return JavaKeyword.INTERFACE.getValue().equals(syntaxToken.text()) && syntaxToken.parent().is(Tree.Kind.ANNOTATION_TYPE);
  }

  private boolean isRestrictedKeyword(SyntaxToken syntaxToken) {
    return withinModule
      && restrictedKeywords.contains(syntaxToken.text())
      && !syntaxToken.parent().is(Tree.Kind.IDENTIFIER);
  }

  @Override
  public void visitTrivia(SyntaxTrivia syntaxTrivia) {
    Range range = syntaxTrivia.range();
    boolean isJavadoc = syntaxTrivia.comment().startsWith("/**");
    TypeOfText typeOfText = isJavadoc ? TypeOfText.STRUCTURED_COMMENT : TypeOfText.COMMENT;
    highlighting.highlight(
      range.start().line(), range.start().columnOffset(),
      range.end().line(), range.end().columnOffset(), typeOfText);
  }
}
