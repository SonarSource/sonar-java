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
package org.sonar.java.model.springcontext;

import javax.annotation.Nullable;

/**
 * Represents a single Spring autowiring dependency: the required type and an optional {@code @Qualifier} name.
 *
 * @param typeFqn   fully-qualified name of the required type
 * @param qualifier value of the {@code @Qualifier} annotation, or {@code null} if absent
 */
public record BeanDependency(String typeFqn, @Nullable String qualifier) {
}
