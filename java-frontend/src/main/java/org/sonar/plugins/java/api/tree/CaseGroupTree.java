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
package org.sonar.plugins.java.api.tree;

import org.sonar.java.annotations.Beta;

import java.util.List;
import org.sonar.plugins.java.api.lighttree.LT;
import org.sonar.plugins.java.api.lighttree.LightBlock;
import org.sonar.plugins.java.api.lighttree.LightCaseGroup;
import org.sonar.plugins.java.api.lighttree.LightStat;

/**
 * Group of 'case's in a 'switch' statement.
 *
 * JLS 14.11
 *
 * <pre>
 *   {@link #labels()} {@link #body()}
 * </pre>
 *
 * @since Java 1.3
 */
@Beta
public interface CaseGroupTree extends Tree, LightCaseGroup {

  List<CaseLabelTree> labels();

  List<StatementTree> body();

  @Override
  default LightStat bodyAsStat() {
    if (body().size() == 1 && body().get(0) instanceof LightBlock){
      return body().get(0);
    } else {
      return new LT.Block(body());
    }
  }

}
