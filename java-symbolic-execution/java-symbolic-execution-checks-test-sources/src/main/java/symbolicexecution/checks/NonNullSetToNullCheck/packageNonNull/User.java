/*
 * Copyright (C) 2025-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package symbolicexecution.checks.NonNullSetToNullCheck.packageNonNull;

import javax.annotation.Nullable;

public record User(
  String login,
  String name,
  @Nullable String avatar,
  boolean active) {
}
