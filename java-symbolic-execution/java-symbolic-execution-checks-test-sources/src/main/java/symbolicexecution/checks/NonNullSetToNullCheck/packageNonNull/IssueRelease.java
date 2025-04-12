/*
 * Copyright (C) 2024-2025 SonarSource SA
 * All rights reserved
 * mailto:info AT sonarsource DOT com
 */
package symbolicexecution.checks.NonNullSetToNullCheck.packageNonNull;

import javax.annotation.Nullable;

/**
 * Object that IssuesReleasesService returns.
 *
 * @param issueDto issue instance
 * @param scaIssue a ScaIssue instance
 * @param releaseDependenciesDto a ScaReleaseDependenciesDto instance
 * @param assigneeUser the user assigned to this IssueRelease
 */
public record IssueRelease(
  String issueDto,
  String scaIssue,
  String releaseDependenciesDto,
  @Nullable User assigneeUser) {
}
