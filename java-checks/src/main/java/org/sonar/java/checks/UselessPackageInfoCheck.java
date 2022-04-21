/*
 * SonarQube Java
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.java.checks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.java.AnalysisException;
import org.sonar.java.EndOfAnalysisCheck;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.caching.CacheContext;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;

@Rule(key = "S4032")
public class UselessPackageInfoCheck implements JavaFileScanner, EndOfAnalysisCheck {

  private static final Logger LOG = Loggers.get(UselessPackageInfoCheck.class);
  private static final String CACHE_KEY_PREFIX = "java:S4032:package:";

  private final Map<String, InputFileScannerContext> unneededPackageInfoFiles = new HashMap<>();
  private final Set<String> knownPackagesWithOtherFiles = new HashSet<>();

  @Override
  public boolean scanWithoutParsing(InputFileScannerContext inputFileScannerContext) {
    return getPackageFromCache(inputFileScannerContext).map(packageName -> {
      processFile(inputFileScannerContext, packageName);
      return true;
    }).orElse(false);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    PackageDeclarationTree packageDeclaration = context.getTree().packageDeclaration();
    String packageName = packageDeclaration == null ? null : ExpressionsHelper.concatenate(packageDeclaration.packageName());

    processFile(context, packageName);
  }

  private void processFile(InputFileScannerContext context, @Nullable String packageName) {
    if (packageName == null) {
      // default package
      return;
    }

    if (context.getCacheContext().isCacheEnabled()) {
      writePackageNameToCache(context, packageName);
    }

    if (knownPackagesWithOtherFiles.contains(packageName)) {
      // already processed package
      return;
    }

    File packageDirectory = context.getInputFile().file().getParentFile();
    File packageInfoFile = new File(packageDirectory, "package-info.java");
    boolean hasOtherFiles = !isOnlyFileFromPackage(packageDirectory, packageInfoFile);

    if (hasOtherFiles) {
      knownPackagesWithOtherFiles.add(packageName);
    } else if (packageInfoFile.isFile()) {
      unneededPackageInfoFiles.put(packageName, context);
    }
  }

  @Override
  public void endOfAnalysis(CacheContext cacheContext) {
    unneededPackageInfoFiles.keySet().removeAll(knownPackagesWithOtherFiles);
    for (var uselessPackageInfoFileContext : unneededPackageInfoFiles.values()) {
      uselessPackageInfoFileContext.addIssueOnFile(this, "Remove this package.");
    }
    unneededPackageInfoFiles.clear();
    knownPackagesWithOtherFiles.clear();
  }

  private static boolean isOnlyFileFromPackage(File packageDirectory, File file) {
    File[] filesInPackage = packageDirectory.listFiles(f -> !f.equals(file));
    return filesInPackage != null && filesInPackage.length == 0;
  }

  private static String cacheKey(InputFile inputFile) {
    return CACHE_KEY_PREFIX + inputFile.key();
  }

  private static Optional<String> getPackageFromCache(InputFileScannerContext inputFileScannerContext) {
    var cacheKey = cacheKey(inputFileScannerContext.getInputFile());
    try (var in = inputFileScannerContext.getCacheContext().getReadCache().read(cacheKey)) {
      return Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
    } catch (IllegalArgumentException e) {
      LOG.debug(() -> String.format(
        "Could not load cached package for key '%s' due to a '%s': %s.", cacheKey, e.getClass().getSimpleName(), e.getMessage()
      ));
    } catch (IOException e) {
      throw new AnalysisException(String.format("IOException while trying to read cached data for key '%s'", cacheKey), e);
    }

    return Optional.empty();
  }

  private static void writePackageNameToCache(InputFileScannerContext context, String packageName) {
    var cacheKey = cacheKey(context.getInputFile());
    try {
      context.getCacheContext().getWriteCache().write(cacheKey, packageName.getBytes(StandardCharsets.UTF_8));
    } catch (IllegalArgumentException e) {
      throw new AnalysisException(String.format("Could not store data to cache key '%s'", cacheKey), e);
    }
  }
}
