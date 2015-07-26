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
package org.sonar.java.checks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.Modifier;
import org.sonar.plugins.java.api.tree.ModifierKeywordTree;
import org.sonar.plugins.java.api.tree.ModifierTree;
import org.sonar.plugins.java.api.tree.ModifiersTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.ActivatedByDefault;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;
import java.util.Set;

@Rule(
  key = "ModifiersOrderCheck",
  name = "Modifiers should be declared in the correct order",
  tags = {"convention"},
  priority = Priority.MINOR)
@ActivatedByDefault
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.READABILITY)
@SqaleConstantRemediation("2min")
public class ModifiersOrderCheck extends SubscriptionBaseVisitor {


  private Set<Tree> alreadyReported = Sets.newHashSet();

  @Override
  public void scanFile(JavaFileScannerContext context) {
    alreadyReported.clear();
    super.scanFile(context);
  }

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Kind.MODIFIERS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (!alreadyReported.contains(tree)) {
      alreadyReported.add(tree);
      ModifierTree badlyOrderedModifier = getFirstBadlyOrdered((ModifiersTree) tree);
      if (badlyOrderedModifier != null) {
        addIssue(badlyOrderedModifier, "Reorder the modifiers to comply with the Java Language Specification.");
      }
    }
  }

  private static ModifierTree getFirstBadlyOrdered(ModifiersTree modifiersTree) {
    int modifierIndex = -1;
    Modifier[] modifiers = Modifier.values();
    for (ModifierTree modifier : modifiersTree) {
      if (modifier.is(Kind.ANNOTATION)) {
        if (modifierIndex >= 0) {
          return modifier;
        }
      } else {
        if (modifierIndex < 0) {
          modifierIndex = 0;
        }
        ModifierKeywordTree mkt = (ModifierKeywordTree) modifier;
        for (; modifierIndex < modifiers.length && !mkt.modifier().equals(modifiers[modifierIndex]); modifierIndex++) {
          // We're just interested in the final value of modifierIndex
        }
        if (modifierIndex == modifiers.length) {
          return modifier;
        }
      }
    }
    return null;
  }
}
