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
package org.sonar.java.checks.spring;

import java.util.List;
import org.sonar.java.model.springcontext.SpringContextModel;

/**
 * A check that reasons over the whole, already-populated {@link SpringContextModel} rather than visiting a
 * single file's AST, since some Spring configuration issues (e.g. ambiguous autowiring) can only be detected
 * once every bean across the analyzed scope is known. Implementations are run once, at the end of the
 * analysis, by {@code SpringContextModelSensor}.
 */
public interface SpringContextCheck {

  List<SpringContextIssue> execute(SpringContextModel model);

}
