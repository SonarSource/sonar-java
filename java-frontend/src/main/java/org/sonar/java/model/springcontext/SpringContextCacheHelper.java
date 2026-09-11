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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.java.model.springcontext.BeanDefinitionGatherer.BeanData;
import org.sonar.java.reporting.AnalyzerMessage;
import org.sonar.plugins.java.api.InputFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext;

/**
 * Shared per-file caching mechanics for Spring context gatherers, including their JSON serialization format.
 *
 * <p>Every cache entry is a JSON object carrying a {@code version} field, currently
 * {@value #CACHE_FORMAT_VERSION}. The version is shared by all gatherers: bumping it whenever any entry's
 * shape changes invalidates all previously cached entries, which are then recomputed on the next analysis.
 */
final class SpringContextCacheHelper {

  private static final int CACHE_FORMAT_VERSION = 1;
  private static final String BEAN_CACHE_KEY_PREFIX = "java:spring:bean-definitions:";
  private static final String COMPONENT_SCAN_CACHE_KEY_PREFIX = "java:spring:component-scan-packages:";
  private static final String VERSION = "version";
  private static final String BEANS = "beans";
  private static final String PACKAGES = "packages";
  private static final String NAME = "name";
  private static final String TYPE = "type";
  private static final String PACKAGE = "package";
  private static final String SPAN = "span";
  private static final String PRIMARY = "primary";
  private static final String PROFILES = "profiles";
  private static final String DEPENDENCIES = "dependencies";
  private static final String INJECTION_POINTS = "injectionPoints";
  private static final String TYPE_HIERARCHY = "typeHierarchy";
  private static final String START_LINE = "startLine";
  private static final String START_CHARACTER = "startCharacter";
  private static final String END_LINE = "endLine";
  private static final String END_CHARACTER = "endCharacter";

  private SpringContextCacheHelper() {
  }

  /**
   * Builds the per-file cache key used to store/retrieve a gatherer's data for the file currently being scanned.
   */
  private static String cacheKey(String cacheKeyPrefix, InputFileScannerContext context) {
    return cacheKeyPrefix + context.getInputFile().key();
  }

  /**
   * Writes a serialized entry to the write cache. A second write under the same key within the same analysis
   * is silently ignored: only the first write for a given file is kept.
   */
  private static void writeToCache(InputFileScannerContext context, Logger log, String cacheKey, String data) {
    try {
      context.getCacheContext().getWriteCache().write(cacheKey, data.getBytes(StandardCharsets.UTF_8));
    } catch (IllegalArgumentException e) {
      log.trace("Tried to write multiple times to cache key '{}'. Ignoring writes after the first.", cacheKey);
    }
  }

  /**
   * Reads and deserializes an entry written during a prior analysis.
   *
   * <p>Any {@link RuntimeException} thrown by {@code deserializer} is treated as a cache miss. The entry is
   * carried over to the write cache via {@code copyFromPrevious} only on successful deserialization, so a
   * corrupt entry is deliberately dropped and rewritten once the file has been re-parsed.
   *
   * @return The deserialized data, or {@link Optional#empty()} if there is no entry or it could not be read.
   */
  private static <T> Optional<T> readFromCache(InputFileScannerContext context, Logger log, String cacheKey, Function<String, T> deserializer) {
    var bytes = context.getCacheContext().getReadCache().readBytes(cacheKey);
    if (bytes == null) {
      return Optional.empty();
    }
    String content = new String(bytes, StandardCharsets.UTF_8);
    try {
      T result = deserializer.apply(content);
      context.getCacheContext().getWriteCache().copyFromPrevious(cacheKey);
      return Optional.of(result);
    } catch (RuntimeException e) {
      log.trace("Failed to deserialize cached data for '{}', will re-parse.", cacheKey, e);
      return Optional.empty();
    }
  }

  /**
   * Serializes and writes this file's beans to the write cache, for reuse by {@link #readBeanDefinitionsFromCache}
   * during the next incremental analysis.
   *
   * @param context Context of the file being scanned, used to build the cache key and access the write cache.
   * @param log     Logger of the calling gatherer, used to trace ignored duplicate writes.
   * @param beans   The beans collected from this file.
   */
  static void writeBeanDefinitionsToCache(JavaFileScannerContext context, Logger log, List<BeanData> beans) {
    writeToCache(context, log, cacheKey(BEAN_CACHE_KEY_PREFIX, context), serializeBeans(beans).toString());
  }

  /**
   * Restores bean definitions from their JSON representation, associating every location with the current file.
   *
   * @param context Context of the file being scanned, used to build the cache key and access the read cache.
   * @param log     Logger of the calling gatherer, used to trace failed accesses to cached data.
   */
  static Optional<List<BeanData>> readBeanDefinitionsFromCache(InputFileScannerContext context, Logger log) {
    var cacheKey = cacheKey(BEAN_CACHE_KEY_PREFIX, context);
    return readFromCache(context, log, cacheKey, content -> deserializeBeans(content, context.getInputFile()));
  }

  static void writeComponentScanPackagesToCache(InputFileScannerContext context, Logger log, Collection<String> packages) {
    var document = newDocument();
    document.add(PACKAGES, serializeStrings(packages));
    writeToCache(context, log, cacheKey(COMPONENT_SCAN_CACHE_KEY_PREFIX, context), document.toString());
  }

  static Optional<List<String>> readComponentScanPackagesFromCache(InputFileScannerContext context, Logger log) {
    var cacheKey = cacheKey(COMPONENT_SCAN_CACHE_KEY_PREFIX, context);
    return readFromCache(context, log, cacheKey, SpringContextCacheHelper::deserializePackages);
  }

  private static JsonObject serializeBeans(List<BeanData> beans) {
    var document = newDocument();
    var serializedBeans = new JsonArray();
    beans.stream().map(SpringContextCacheHelper::serializeBean).forEach(serializedBeans::add);
    document.add(BEANS, serializedBeans);
    return document;
  }

  private static JsonObject serializeBean(BeanData bean) {
    var serializedBean = new JsonObject();
    serializedBean.addProperty(NAME, bean.beanName());
    serializedBean.addProperty(TYPE, bean.type());
    serializedBean.addProperty(PACKAGE, bean.beanPackage());
    serializedBean.add(SPAN, serializeSpan(bean.textSpan()));
    serializedBean.addProperty(PRIMARY, bean.isPrimary());
    serializedBean.addProperty(PROFILES, bean.profiles());
    var dependencies = new JsonArray();
    bean.dependencyInjectionPoints().forEach((type, points) -> dependencies.add(serializeDependency(type, points)));
    serializedBean.add(DEPENDENCIES, dependencies);
    serializedBean.add(TYPE_HIERARCHY, serializeStrings(bean.typeHierarchy()));
    return serializedBean;
  }

  private static JsonArray serializeStrings(Collection<String> values) {
    var serializedValues = new JsonArray();
    values.forEach(serializedValues::add);
    return serializedValues;
  }

  private static JsonObject serializeDependency(String type, Set<InjectionPoint> points) {
    var serializedDependency = new JsonObject();
    serializedDependency.addProperty(TYPE, type);
    var injectionPoints = new JsonArray();
    points.stream().map(SpringContextCacheHelper::serializeInjectionPoint).forEach(injectionPoints::add);
    serializedDependency.add(INJECTION_POINTS, injectionPoints);
    return serializedDependency;
  }

  private static JsonObject serializeInjectionPoint(InjectionPoint injectionPoint) {
    var serializedInjectionPoint = new JsonObject();
    serializedInjectionPoint.addProperty(NAME, injectionPoint.name());
    serializedInjectionPoint.add(SPAN, serializeSpan(injectionPoint.location().mainLocation()));
    return serializedInjectionPoint;
  }

  private static JsonObject serializeSpan(AnalyzerMessage.TextSpan span) {
    var serializedSpan = new JsonObject();
    serializedSpan.addProperty(START_LINE, span.startLine);
    serializedSpan.addProperty(START_CHARACTER, span.startCharacter);
    serializedSpan.addProperty(END_LINE, span.endLine);
    serializedSpan.addProperty(END_CHARACTER, span.endCharacter);
    return serializedSpan;
  }

  private static List<BeanData> deserializeBeans(String content, InputFile inputFile) {
    var beans = requiredArray(parseDocument(content), BEANS);
    List<BeanData> result = new ArrayList<>();
    for (JsonElement bean : beans) {
      result.add(deserializeBean(requiredObject(bean), inputFile));
    }
    return result;
  }

  private static BeanData deserializeBean(JsonObject serializedBean, InputFile inputFile) {
    String beanName = requiredString(serializedBean, NAME);
    String type = requiredString(serializedBean, TYPE);
    String beanPackage = requiredString(serializedBean, PACKAGE);
    var span = deserializeSpan(requiredObject(serializedBean, SPAN));
    boolean isPrimary = requiredBoolean(serializedBean, PRIMARY);
    String profiles = nullableString(serializedBean, PROFILES);
    Map<String, Set<InjectionPoint>> injectionPoints = deserializeDependencies(requiredArray(serializedBean, DEPENDENCIES), inputFile);
    Set<String> typeHierarchy = deserializeStrings(requiredArray(serializedBean, TYPE_HIERARCHY));
    return new BeanData(beanName, type, beanPackage, inputFile, span, isPrimary, profiles,
      BeanDefinitionGatherer.projectToNames(injectionPoints), injectionPoints, typeHierarchy);
  }

  private static Map<String, Set<InjectionPoint>> deserializeDependencies(JsonArray dependencies, InputFile inputFile) {
    Map<String, Set<InjectionPoint>> result = new LinkedHashMap<>();
    for (JsonElement dependency : dependencies) {
      JsonObject serializedDependency = requiredObject(dependency);
      String type = requiredString(serializedDependency, TYPE);
      result.put(type, deserializeInjectionPoints(requiredArray(serializedDependency, INJECTION_POINTS), inputFile));
    }
    return result;
  }

  private static Set<InjectionPoint> deserializeInjectionPoints(JsonArray injectionPoints, InputFile inputFile) {
    Set<InjectionPoint> result = new LinkedHashSet<>();
    for (JsonElement injectionPoint : injectionPoints) {
      JsonObject serializedInjectionPoint = requiredObject(injectionPoint);
      result.add(new InjectionPoint(
        requiredString(serializedInjectionPoint, NAME),
        new BeanLocation(inputFile, deserializeSpan(requiredObject(serializedInjectionPoint, SPAN)))));
    }
    return result;
  }

  private static List<String> deserializePackages(String content) {
    return List.copyOf(deserializeStrings(requiredArray(parseDocument(content), PACKAGES)));
  }

  private static Set<String> deserializeStrings(JsonArray values) {
    Set<String> result = new LinkedHashSet<>();
    for (JsonElement value : values) {
      if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
        throw new IllegalArgumentException("Expected a JSON string, got: " + value);
      }
      result.add(value.getAsString());
    }
    return result;
  }

  private static AnalyzerMessage.TextSpan deserializeSpan(JsonObject serializedSpan) {
    int startLine = requiredInt(serializedSpan, START_LINE);
    int startCharacter = requiredInt(serializedSpan, START_CHARACTER);
    int endLine = requiredInt(serializedSpan, END_LINE);
    int endCharacter = requiredInt(serializedSpan, END_CHARACTER);
    if (startLine < 1 || startCharacter < 0 || endLine < startLine || endCharacter < 0 || (startLine == endLine && endCharacter < startCharacter)) {
      throw new IllegalArgumentException("Invalid source span: " + serializedSpan);
    }
    return new AnalyzerMessage.TextSpan(startLine, startCharacter, endLine, endCharacter);
  }

  private static JsonObject parseDocument(String content) {
    JsonElement element = JsonParser.parseString(content);
    JsonObject document = requiredObject(element);
    int version = requiredInt(document, VERSION);
    if (version != CACHE_FORMAT_VERSION) {
      throw new IllegalArgumentException("Unsupported cache format version: " + version);
    }
    return document;
  }

  private static JsonObject newDocument() {
    var document = new JsonObject();
    document.addProperty(VERSION, CACHE_FORMAT_VERSION);
    return document;
  }

  private static JsonObject requiredObject(JsonElement element) {
    if (!element.isJsonObject()) {
      throw new IllegalArgumentException("Expected a JSON object, got: " + element);
    }
    return element.getAsJsonObject();
  }

  private static JsonObject requiredObject(JsonObject object, String property) {
    return requiredObject(requiredElement(object, property));
  }

  private static JsonArray requiredArray(JsonObject object, String property) {
    JsonElement element = requiredElement(object, property);
    if (!element.isJsonArray()) {
      throw new IllegalArgumentException("Expected a JSON array for property '" + property + "', got: " + element);
    }
    return element.getAsJsonArray();
  }

  private static String requiredString(JsonObject object, String property) {
    JsonElement element = requiredElement(object, property);
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
      throw new IllegalArgumentException("Expected a JSON string for property '" + property + "', got: " + element);
    }
    return element.getAsString();
  }

  private static String nullableString(JsonObject object, String property) {
    JsonElement element = requiredElement(object, property);
    if (element.isJsonNull()) {
      return null;
    }
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
      throw new IllegalArgumentException("Expected a JSON string or null for property '" + property + "', got: " + element);
    }
    return element.getAsString();
  }

  private static boolean requiredBoolean(JsonObject object, String property) {
    JsonPrimitive primitive = requiredPrimitive(object, property);
    if (!primitive.isBoolean()) {
      throw new IllegalArgumentException("Expected a JSON boolean for property '" + property + "', got: " + primitive);
    }
    return primitive.getAsBoolean();
  }

  private static int requiredInt(JsonObject object, String property) {
    JsonPrimitive primitive = requiredPrimitive(object, property);
    if (!primitive.isNumber()) {
      throw new IllegalArgumentException("Expected a JSON integer for property '" + property + "', got: " + primitive);
    }
    return primitive.getAsBigDecimal().intValueExact();
  }

  private static JsonPrimitive requiredPrimitive(JsonObject object, String property) {
    JsonElement element = requiredElement(object, property);
    if (!element.isJsonPrimitive()) {
      throw new IllegalArgumentException("Expected a JSON primitive for property '" + property + "', got: " + element);
    }
    return element.getAsJsonPrimitive();
  }

  private static JsonElement requiredElement(JsonObject object, String property) {
    JsonElement element = object.get(property);
    if (element == null) {
      throw new IllegalArgumentException("Missing JSON property '" + property + "'");
    }
    return element;
  }
}
