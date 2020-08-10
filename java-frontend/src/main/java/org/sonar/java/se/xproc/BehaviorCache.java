/*
 * SonarQube Java
 * Copyright (C) 2012-2020 SonarSource SA
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
package org.sonar.java.se.xproc;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.bytecode.se.BytecodeEGWalker;
import org.sonar.java.model.JUtils;
import org.sonar.java.model.Sema;
import org.sonar.java.se.SymbolicExecutionVisitor;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.MethodTree;

public class BehaviorCache {

  private static final Logger LOG = Loggers.get(BehaviorCache.class);

  private final SquidClassLoader classLoader;
  private final boolean crossFileEnabled;
  private SymbolicExecutionVisitor sev;
  private Sema semanticModel;

  @VisibleForTesting
  public Map<String, MethodBehavior> behaviors = new LinkedHashMap<>();

  @VisibleForTesting
  final Map<String, MethodBehavior> hardcodedBehaviors = hardcodedBehaviors();

  private static Map<String, MethodBehavior> defaultHardcodedBehaviors = null;
  private static final Type LIST_OF_METHOD_BEHAVIORS_TYPE = new TypeToken<List<MethodBehavior>>() {}.getType();

  private Map<String, MethodBehavior> hardcodedBehaviors() {
    if (defaultHardcodedBehaviors == null) {
      defaultHardcodedBehaviors = loadHardcodedBehaviors();
    }
    return new LinkedHashMap<>(defaultHardcodedBehaviors);
  }

  private Map<String, MethodBehavior> loadHardcodedBehaviors() {
    Map<String, MethodBehavior> result = new LinkedHashMap<>();
    Gson gson = MethodBehaviorJsonAdapter.gson(semanticModel);
    // one of the method behavior list, as resource target
    URL hardcodedMethodBehaviorsURL = BehaviorCache.class.getResource("java.lang.json");
    if (hardcodedMethodBehaviorsURL == null || !new File(hardcodedMethodBehaviorsURL.getPath()).exists()) {
      LOG.debug("Unable to load hardcoded method behaviors");
      return Collections.emptyMap();
    }
    File[] serializedMethodBehaviors = new File(hardcodedMethodBehaviorsURL.getPath())
      .getParentFile()
      .listFiles((dir, filename) -> filename.endsWith(".json"));
    for (File serialized : serializedMethodBehaviors) {
      try (Reader reader = Files.newBufferedReader(serialized.toPath(), StandardCharsets.UTF_8)) {
        List<MethodBehavior> deserialized = gson.fromJson(reader, LIST_OF_METHOD_BEHAVIORS_TYPE);
        deserialized.forEach(methodBehavior -> result.put(methodBehavior.signature(), methodBehavior));
      } catch (IOException e) {
        LOG.error(String.format("Unable to load hardcoded method behaviors of \"%s\". Defaulting to no hardcoded method behaviors.", serialized.getName()), e);
        return Collections.emptyMap();
      }
    }
    return result;
  }

  public BehaviorCache(SquidClassLoader classLoader) {
    this(classLoader, true);
  }

  public BehaviorCache(SquidClassLoader classLoader, boolean crossFileEnabled) {
    this.classLoader = classLoader;
    this.crossFileEnabled = crossFileEnabled;
  }

  public void setFileContext(@Nullable SymbolicExecutionVisitor sev,@Nullable Sema semanticModel) {
    this.sev = sev;
    this.semanticModel = semanticModel;
  }

  public void cleanup() {
    behaviors.clear();
  }

  public MethodBehavior methodBehaviorForSymbol(Symbol.MethodSymbol symbol) {
    String signature = symbol.signature();
    boolean varArgs = JUtils.isVarArgsMethod(symbol);
    return behaviors.computeIfAbsent(signature, k -> new MethodBehavior(signature, varArgs));
  }

  public MethodBehavior methodBehaviorForSymbol(String signature) {
    return hardcodedBehaviors.computeIfAbsent(signature, k -> new MethodBehavior(signature));
  }

  @CheckForNull
  public MethodBehavior get(Symbol.MethodSymbol symbol) {
    return get(symbol.signature(), symbol);
  }

  @CheckForNull
  public MethodBehavior get(String signature) {
    return get(signature, null);
  }

  @CheckForNull
  private MethodBehavior get(String signature, @Nullable Symbol.MethodSymbol symbol) {
    MethodBehavior mb = behaviors.get(signature);
    if(mb != null) {
      return mb;
    }
    if (symbol != null) {
      MethodTree declaration = symbol.declaration();
      if (SymbolicExecutionVisitor.methodCanNotBeOverriden(symbol) && declaration != null) {
        sev.execute(declaration);
        return behaviors.get(signature);
      }
    }

    // disabled x-file analysis, behavior based on source code can still be used
    if (!crossFileEnabled && !hardcodedBehaviors.containsKey(signature)) {
      return null;
    }

    if (!hardcodedBehaviors.containsKey(signature)) {
      // TODO remove me
      new BytecodeEGWalker(this, semanticModel).getMethodBehavior(signature, classLoader);
    }
    return hardcodedBehaviors.get(signature);
  }

  /**
   * Do not trigger any new computation of method behavior, just check if there is a known method behavior for the symbol.
   *
   * @param signature The targeted method.
   * @return null for methods having no computed method behavior yet, or its method behavior, based on source or hardcoded set
   */
  @CheckForNull
  public MethodBehavior peek(String signature) {
    // directly query the cache, to not trigger computation of new method behaviors
    MethodBehavior mb = behaviors.get(signature);
    if (mb != null) {
      return mb;
    }
    // check for hardcoded signatures
    return hardcodedBehaviors.get(signature);
  }
}
