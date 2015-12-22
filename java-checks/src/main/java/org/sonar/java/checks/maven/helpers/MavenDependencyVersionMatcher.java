/*
 * SonarQube Java
 * Copyright (C) 2012-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.java.checks.maven.helpers;

import com.google.common.base.Preconditions;
import org.sonar.java.checks.maven.AbstractNamingConvention;
import org.sonar.maven.model.LocatedAttribute;
import org.sonar.maven.model.maven2.Dependency;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.regex.Pattern;

/**
 * Matchers targeting versions of dependencies.
 */
public abstract class MavenDependencyVersionMatcher extends MavenDependencyAbstractMatcher {

  /**
   * Provide a version matcher matching any given version
   * @return a new instance of {@link AlwaysMatchingVersionMatcher}
   */
  protected static MavenDependencyVersionMatcher alwaysMatchingVersionMatcher() {
    return new AlwaysMatchingVersionMatcher();
  }

  /**
   * Provide a dependency version matcher for a given check.
   *
   * <br />
   *
   * Details regarding supported formats of parameter <code>version</code>:
   * <ul>
   *   <li>java regular expression can be used to define pattern: "<code>1.3.*</code>"</li>
   *   <li>dash-delimited range (without version qualifier). Examples: </li>
   *   <ul>
   *     <li>"<code>1.0-3.1</code>" : any version between 1.0 and 3.1</li>
   *     <li>"<code>1.0-*</code>" : any version after version 1.0</li>
   *     <li>"<code>*-3.1</code>" : any version before version 3.1</li>
   *   </ul>
   *   <li>empty (when version is not relevant): ""</li>
   * </ul>
   *
   * @param version the version expected for the dependency
   * @param callingCheckKey the check requiring the matcher
   * @return
   */
  public static MavenDependencyVersionMatcher fromString(String version, String callingCheckKey) {
    if (version.isEmpty() || isWildCard(version)) {
      return new AlwaysMatchingVersionMatcher();
    }
    if (version.contains("-")) {
      String[] bounds = version.split("-");
      return new RangedVersionMatcher(bounds[0], bounds[1], callingCheckKey);
    }
    return new PatternVersionMatcher(version, callingCheckKey);
  }

  /**
   * Matcher which always match a dependency, whatever its version is.
   */
  private static class AlwaysMatchingVersionMatcher extends MavenDependencyVersionMatcher {
    @Override
    public boolean matches(Dependency dependency) {
      return true;
    }
  }

  /**
   * Matcher which requires the dependency version to match a provided given pattern
   */
  private static class PatternVersionMatcher extends MavenDependencyVersionMatcher {

    private Pattern pattern = null;

    public PatternVersionMatcher(String version, String callingCheckKey) {
      pattern = AbstractNamingConvention.compileRegex(version, callingCheckKey);
    }

    @Override
    public boolean matches(Dependency dependency) {
      return attributeMatchesPattern(dependency.getVersion(), pattern);
    }
  }

  /**
   * Matcher which requires a range of version defining a lower bound and an upper bound.
   */
  private static class RangedVersionMatcher extends MavenDependencyVersionMatcher {
    @Nullable
    private final ArtifactVersion lowerBound;
    @Nullable
    private final ArtifactVersion upperBound;

    public RangedVersionMatcher(String lowerBound, String upperBound, String callingCheckKey) {
      this.lowerBound = isWildCard(lowerBound) ? null : getVersion(lowerBound, callingCheckKey);
      this.upperBound = isWildCard(upperBound) ? null : getVersion(upperBound, callingCheckKey);
      // check that we are not bypassing both bounds
      Preconditions.checkArgument(!(this.lowerBound == null && this.upperBound == null));
    }

    @Override
    public boolean matches(Dependency dependency) {
      LocatedAttribute version = dependency.getVersion();
      if (version == null || version.getValue().trim().isEmpty()) {
        // the dependency has no version, so it does not match the range
        return false;
      }
      ArtifactVersion dependencyVersion = getVersionSilently(version.getValue());
      if (dependencyVersion == null) {
        // unable to parse version, ignore this dependency
        return false;
      }
      boolean matchesLowerBound = lowerBound == null || dependencyVersion.isAfterOrEqual(lowerBound);
      boolean matchesUpperBound = upperBound == null || upperBound.isAfterOrEqual(dependencyVersion);
      return matchesLowerBound && matchesUpperBound;
    }

    /**
     * Build a {@link ArtifactVersion} from a String, without failing in case of exception while parsing.
     * @param version the raw version as string
     * @return the {@link ArtifactVersion} corresponding to the provided version as string
     */
    @CheckForNull
    private static ArtifactVersion getVersionSilently(String version) {
      try {
        return ArtifactVersion.parseString(version);
      } catch (NumberFormatException e) {
        // do nothing, version will be ignored
      }
      return null;
    }

    /**
     * Build a {@link ArtifactVersion} from a String, throwing an {@link IllegalArgumentException} if failed to parse value.
     * @param version the raw version as string
     * @param callingCheckKey the check requiring the matcher
     * @return the {@link ArtifactVersion} corresponding to the provided version as string
     */
    private static ArtifactVersion getVersion(String version, String callingCheckKey) {
      try {
        return ArtifactVersion.parseString(version);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("[" + callingCheckKey + "] Provided version does not match expected pattern:"
          + " <major version>.<minor version>.<incremental version> (recieved: " + version + ")", e);
      }
    }

    /**
     * Expected format: <code>[major version].[minor version].[incremental version]-[qualifier]</code>
     *
     * <br />
     *
     * Handled formats:
     * <ul>
     *   <li><code>1.0.1-SNAPSHOT</code></li>
     *   <li><code>2.4</code></li>
     *   <li><code>1</code></li>
     *   <li><code>0.5-alpha</code></li>
     * </ul>
     */
    private static class ArtifactVersion {
      private static final int HANDLED_VERSIONS = 3;
      private final Integer[] versions = new Integer[HANDLED_VERSIONS];

      private static ArtifactVersion parseString(String version) {
        String[] split = version.split("\\.");
        if (version.contains("-")) {
          // ignore qualifier
          split = version.split("-")[0].split("\\.");
        }
        ArtifactVersion result = new ArtifactVersion();
        for (int i = 0; i < Math.min(HANDLED_VERSIONS, split.length); i++) {
          result.versions[i] = Integer.parseInt(split[i]);
        }
        return result;
      }

      public boolean isAfterOrEqual(ArtifactVersion v) {
        return isAfterOrEqual(v, 0);
      }

      public boolean isAfterOrEqual(ArtifactVersion v, int i) {
        if (i >= HANDLED_VERSIONS) {
          return true;
        }
        Integer local = versions[i];
        Integer target = v.versions[i];

        if (target == null) {
          return true;
        } else if (local == null) {
          return false;
        }

        int compare = local.compareTo(target);
        return (compare > 0) || (compare == 0 && isAfterOrEqual(v, i + 1));
      }
    }
  }
}
