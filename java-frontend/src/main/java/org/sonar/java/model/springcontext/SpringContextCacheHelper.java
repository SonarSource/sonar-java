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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sonar.java.serialization.BeanDefinitionHolderTypeAdapter;
import org.sonar.java.serialization.InjectionPointTypeAdapter;
import org.sonar.java.serialization.TextSpanTypeAdapter;

import static org.sonar.java.serialization.JsonUtils.BEANS;
import static org.sonar.java.serialization.JsonUtils.PACKAGES;
import static org.sonar.java.serialization.JsonUtils.deserializeStrings;
import static org.sonar.java.serialization.JsonUtils.parseDocument;
import static org.sonar.java.serialization.JsonUtils.requiredArray;
import static org.sonar.java.serialization.JsonUtils.writeDocument;
import static org.sonar.java.serialization.JsonUtils.writeStrings;

/**
 * Format of the cache entries the Spring context gatherers keep for each file.
 *
 * <p>Every entry is a JSON object carrying a {@code version} field, currently
 * {@value #CACHE_FORMAT_VERSION}. The version is shared by all gatherers: bumping it whenever any entry's
 * shape changes invalidates all previously cached entries, which are then recomputed on the next analysis.
 *
 * <p>This class only owns that versioned envelope. The shape of the beans it wraps is defined by
 * {@link BeanDefinitionHolderTypeAdapter}, {@link InjectionPointTypeAdapter} and {@link TextSpanTypeAdapter}, while
 * reading and writing the entries themselves is handled by
 * {@link org.sonar.java.caching.FileCachingCheck}, which the gatherers implement.
 */
final class SpringContextCacheHelper {

  private static final int CACHE_FORMAT_VERSION = 1;

  private SpringContextCacheHelper() {
  }

  static byte[] serializeBeans(List<BeanDefinitionHolder.InputFileData> beans) {
    String document = writeDocument(CACHE_FORMAT_VERSION, out -> {
      out.name(BEANS);
      out.beginArray();
      for (BeanDefinitionHolder.InputFileData bean : beans) {
        BeanDefinitionHolderTypeAdapter.getInstance().write(out, bean);
      }
      out.endArray();
    });
    return toBytes(document);
  }

  static List<BeanDefinitionHolder.InputFileData> deserializeBeans(byte[] data) {
    var beans = requiredArray(readDocument(data), BEANS);
    List<BeanDefinitionHolder.InputFileData> result = new ArrayList<>();
    for (JsonElement bean : beans) {
      result.add(BeanDefinitionHolderTypeAdapter.getInstance().fromJsonTree(bean));
    }
    return result;
  }

  static byte[] serializeComponentScanPackages(Collection<String> packages) {
    String document = writeDocument(CACHE_FORMAT_VERSION, out -> {
      out.name(PACKAGES);
      writeStrings(out, packages);
    });
    return toBytes(document);
  }

  static Set<String> deserializeComponentScanPackages(byte[] data) {
    return deserializeStrings(requiredArray(readDocument(data), PACKAGES));
  }

  private static byte[] toBytes(String document) {
    return document.getBytes(StandardCharsets.UTF_8);
  }

  private static JsonObject readDocument(byte[] data) {
    return parseDocument(new String(data, StandardCharsets.UTF_8), CACHE_FORMAT_VERSION);
  }
}
