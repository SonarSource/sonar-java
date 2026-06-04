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
package org.sonar.java.model.springcontext.gatherers;

import org.sonar.java.model.springcontext.SpringContextModel;
import org.sonar.java.model.springcontext.SpringContextModelGatherer;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.ArrayList;
import java.util.List;

public class SampleSpringContextModelGatherer extends SpringContextModelGatherer {

  private final List<String> collectedData = new ArrayList<>();

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return List.of(Tree.Kind.COMPILATION_UNIT);
  }

  @Override
  public void visitNode(Tree tree) {
    collectedData.add(context.getInputFile().filename());
  }

  @Override
  public void gatherSpringContextData(SpringContextModel springContextModel) {
    for (String data : collectedData) {
      springContextModel.getProjectPackageScan().addPackage(data, data);
    }
  }

}
