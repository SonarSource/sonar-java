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
package org.sonar.java.checks.queryAPI

import org.sonar.check.Rule
import org.sonar.check.RuleProperty
import org.sonar.plugins.java.api.JavaFileScannerContext
import org.sonar.plugins.java.api.query.QueryRule
import org.sonar.plugins.java.api.query.operation.generated.TreeKind
import org.sonar.plugins.java.api.query.operation.generated.isStatic
import org.sonar.plugins.java.api.query.operation.ofKind
import org.sonar.plugins.java.api.query.operation.report
import org.sonar.plugins.java.api.tree.Tree
import org.sonarsource.astquery.operation.builder.ManyBuilder
import org.sonarsource.astquery.operation.core.aggregate
import org.sonarsource.astquery.operation.core.where

@Rule(key = "S3030")
class StaticImportCountCheckKT : QueryRule {
  companion object {
    private const val DEFAULT_THRESHOLD = 4
  }

  @RuleProperty(key = "threshold", description = "The maximum number of static imports allowed", defaultValue = "" + DEFAULT_THRESHOLD)
  private var threshold = DEFAULT_THRESHOLD

  fun setThreshold(threshold: Int) {
    this.threshold = threshold
  }

  override fun createQuery(entry: ManyBuilder<Tree>) {
    entry
      .ofKind(TreeKind.IMPORT)
      .where { it.isStatic() }
      .aggregate()
      .report { context, imports ->
        if (imports.size > threshold) {
          val flow = imports.map { JavaFileScannerContext.Location("+1", it) }
          val message = "Reduce the number of \"static\" imports in this class from ${imports.size} to the maximum allowed $threshold."

          context.reportIssue(this, imports[0], message, flow, null)
        }
      }
  }
}
