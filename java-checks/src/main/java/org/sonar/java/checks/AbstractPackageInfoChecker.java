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

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.plugins.java.api.internal.EndOfAnalysis;
import org.sonar.java.checks.helpers.ExpressionsHelper;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.PackageDeclarationTree;

public abstract class AbstractPackageInfoChecker implements JavaFileScanner, EndOfAnalysis {
  private static final Logger LOG = Loggers.get(AbstractPackageInfoChecker.class);
  private static final String CACHE_KEY_PREFIX = "java:S1228;S4032:package:";

  private static String cacheKey(InputFile inputFile) {
    return CACHE_KEY_PREFIX + inputFile.key();
  }

  protected abstract void processFile(InputFileScannerContext context, String packageName);

  @Override
  public boolean scanWithoutParsing(InputFileScannerContext inputFileScannerContext) {
    return getPackageFromCache(inputFileScannerContext).map(packageName -> {
      processFileAndCacheIfApplicable(inputFileScannerContext, packageName);
      return true;
    }).orElse(false);
  }

  @Override
  public void scanFile(JavaFileScannerContext context) {
    PackageDeclarationTree packageDeclaration = context.getTree().packageDeclaration();
    String packageName = packageDeclaration == null ? null : ExpressionsHelper.concatenate(packageDeclaration.packageName());

    processFileAndCacheIfApplicable(context, packageName);
  }

  private void processFileAndCacheIfApplicable(InputFileScannerContext context, @Nullable String packageName) {
    if (context.getCacheContext().isCacheEnabled()) {
      writePackageNameToCache(context, packageName == null ? "" : packageName);
    }

    if (packageName == null || packageName.isEmpty()) {
      // default package
      return;
    }

    processFile(context, packageName);
  }

  protected static Optional<String> getPackageFromCache(InputFileScannerContext inputFileScannerContext) {
    var cacheKey = cacheKey(inputFileScannerContext.getInputFile());
    var bytes = inputFileScannerContext.getCacheContext().getReadCache().readBytes(cacheKey);
    return bytes != null ? Optional.of(new String(bytes, StandardCharsets.UTF_8)) : Optional.empty();
  }

  protected static void writePackageNameToCache(InputFileScannerContext context, String packageName) {
    var cacheKey = cacheKey(context.getInputFile());
    try {
      context.getCacheContext().getWriteCache().write(cacheKey, packageName.getBytes(StandardCharsets.UTF_8));
    } catch (IllegalArgumentException e) {
      LOG.trace(() -> String.format("Could not store data to cache key '%s': %s", cacheKey, e.getMessage()));
    }
  }
}
