/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.checks.regex;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.sonar.check.Rule;
import org.sonar.java.regex.RegexParseResult;
import org.sonar.java.regex.ast.CharacterClassTree;
import org.sonar.java.regex.ast.EscapedCharacterClassTree;
import org.sonar.java.regex.ast.Quantifier;
import org.sonar.java.regex.ast.RegexBaseVisitor;
import org.sonar.java.regex.ast.RegexTree;
import org.sonar.java.regex.ast.RepetitionTree;
import org.sonar.java.regex.ast.SequenceTree;
import org.sonar.java.regex.ast.SimpleQuantifier;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = "S5857")
public class ReluctantQuantifierCheck extends AbstractRegexCheck {

  @Override
  public void checkRegex(RegexParseResult regexForLiterals, MethodInvocationTree mit) {
    new ReluctantQuantifierFinder().visit(regexForLiterals);
  }

  private class ReluctantQuantifierFinder extends RegexBaseVisitor {

    @Override
    public void visitSequence(SequenceTree tree) {
      super.visitSequence(tree);
      List<RegexTree> items = tree.getItems();
      if (items.size() >= 2 && items.get(items.size() - 2).is(RegexTree.Kind.REPETITION)) {
        RepetitionTree repetition = (RepetitionTree) items.get(items.size() - 2);
        if (isReluctantlyQuantifiedDot(repetition)) {
          findNegatedCharacterClassFor(items.get(items.size() - 1)).ifPresent(negatedClass -> {
            String newQuantifier = makePossessive(repetition.getQuantifier());
            String message = String.format("Replace this use of a reluctant quantifier with \"%s%s\".", negatedClass, newQuantifier);
            reportIssue(repetition, String.format(message, negatedClass), null, Collections.emptyList());
          });
        }
      }
    }

    private boolean isReluctantlyQuantifiedDot(RepetitionTree repetition) {
      return repetition.getQuantifier().getModifier() == Quantifier.Modifier.RELUCTANT
        && !repetition.getQuantifier().isFixed()
        && repetition.getElement().is(RegexTree.Kind.DOT);
    }

    private String makePossessive(Quantifier quantifier) {
      if (quantifier instanceof SimpleQuantifier) {
        return ((SimpleQuantifier) quantifier).getKind() + "+";
      } else {
        String max = Optional.ofNullable(quantifier.getMaximumRepetitions()).map(Object::toString).orElse("");
        return String.format("{%d,%s}+", quantifier.getMinimumRepetitions(), max);
      }
    }

    private Optional<String> findNegatedCharacterClassFor(RegexTree tree) {
      String result;
      switch (tree.kind()) {
        case PLAIN_CHARACTER:
          result = "[^" + tree.getText() + "]";
          break;
        case ESCAPED_CHARACTER_CLASS:
          EscapedCharacterClassTree escapedClass = (EscapedCharacterClassTree) tree;
          result = "\\\\" + negateEscapedCharacterClassType(escapedClass.getType());
          if (escapedClass.isProperty()) {
            result += "{" + escapedClass.property() + "}";
          }
          break;
        case CHARACTER_CLASS:
          CharacterClassTree characterClass = (CharacterClassTree) tree;
          String body = characterClass.getContents().getText();
          if (characterClass.isNegated()) {
            result = "[" + body + "]";
          } else {
            result = "[^" + body + "]";
          }
          break;
        default:
          return Optional.empty();
      }
      return Optional.of(result);
    }

    private char negateEscapedCharacterClassType(char type) {
      return Character.isLowerCase(type) ? Character.toUpperCase(type) : Character.toLowerCase(type);
    }
  }

}
