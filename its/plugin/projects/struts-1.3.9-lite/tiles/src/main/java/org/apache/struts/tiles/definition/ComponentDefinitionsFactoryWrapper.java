/*
 * $Id: ComponentDefinitionsFactoryWrapper.java 471754 2006-11-06 14:55:09Z husted $
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.struts.tiles.definition;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.apache.struts.tiles.ComponentDefinition;
import org.apache.struts.tiles.ComponentDefinitionsFactory;
import org.apache.struts.tiles.DefinitionsFactory;
import org.apache.struts.tiles.DefinitionsFactoryConfig;
import org.apache.struts.tiles.DefinitionsFactoryException;
import org.apache.struts.tiles.NoSuchDefinitionException;
import org.apache.struts.util.RequestUtils;

/**
 * Wrapper from new definition factory interface to old interface.
 * This class provides mapping from the old interface's life cycle to the new life cycle.
 * @since 20020708
 */
public class ComponentDefinitionsFactoryWrapper implements DefinitionsFactory {

    /**
     * The underlying factory.
     */
    private ComponentDefinitionsFactory factory = null;

    /**
     * Factory configuration,
     */
    private DefinitionsFactoryConfig config = null;

    /**
     * Constructor.
     * Create new wrapper for specified factory.
     * @param factory The factory to create a wrapper for.
     */
    public ComponentDefinitionsFactoryWrapper(ComponentDefinitionsFactory factory) {
        this.factory = factory;
    }

    /**
     * Constructor.
     * Create new wrapper.
     * The config object passed to init method should reference a factory implementing
     * {@link ComponentDefinitionsFactory}.
     */
    public ComponentDefinitionsFactoryWrapper() {
        super();
    }

    /**
     * Get requested definition.
     * @param name Name of the definition.
     * @param request The request we are processing.
     * @param servletContext Our servlet context.
     * @return ComponentDefition
     */
    public ComponentDefinition getDefinition(
        String name,
        ServletRequest request,
        ServletContext servletContext)
        throws NoSuchDefinitionException, DefinitionsFactoryException {

        return factory.getDefinition(name, request, servletContext);
    }

    /**
     * Call underlying factory init method.
     * @param config DefinitionsFactoryConfig.
     * @param servletContext Our servlet context.
     */
    public void init(DefinitionsFactoryConfig config, ServletContext servletContext)
        throws DefinitionsFactoryException {

        this.config = config;

        // create factory and initialize it
        if (factory == null) {
            factory = createFactoryInstance(config.getFactoryClassname());
        }

        factory.initFactory(servletContext, createConfigMap(config));
    }

    /**
     * Do nothing because old life cycle has no equivalent.
     */
    public void destroy() {
        factory = null;
    }

    /**
     * Set underlying factory configuration.
     * @param config DefinitionsFactoryConfig to use.
     * @param servletContext Our servlet context.
     *
     */
    public void setConfig(
        DefinitionsFactoryConfig config,
        ServletContext servletContext)
        throws DefinitionsFactoryException {

        ComponentDefinitionsFactory newFactory =
            createFactoryInstance(config.getFactoryClassname());

        newFactory.initFactory(servletContext, createConfigMap(config));
        factory = newFactory;
    }

    /**
     * Get underlying factory configuration.
     * @return DefinitionsFactoryConfig.
     */
    public DefinitionsFactoryConfig getConfig() {
        return config;
    }

    /**
     * Get internal factory.
     * @return The internal ComponentDefitionsFactory.
     */
    public ComponentDefinitionsFactory getInternalFactory() {
        return factory;
    }

    /**
     * Create Definition factory from provided classname which must implement {@link ComponentDefinitionsFactory}.
     * Factory class must extend {@link DefinitionsFactory}.
     * @param classname Class name of the factory to create.
     * @return newly created factory.
     * @throws DefinitionsFactoryException If an error occur while initializing factory
     */
    protected ComponentDefinitionsFactory createFactoryInstance(String classname)
        throws DefinitionsFactoryException {

        try {
            Class factoryClass = RequestUtils.applicationClass(classname);
            Object factory = factoryClass.newInstance();
            return (ComponentDefinitionsFactory) factory;

        } catch (ClassCastException ex) { // Bad classname
            throw new DefinitionsFactoryException(
                "Error - createDefinitionsFactory : Factory class '"
                    + classname
                    + " must implement 'DefinitionsFactory'.",
                ex);

        } catch (ClassNotFoundException ex) { // Bad classname
            throw new DefinitionsFactoryException(
                "Error - createDefinitionsFactory : Bad class name '"
                    + classname
                    + "'.",
                ex);

        } catch (InstantiationException ex) { // Bad constructor or error
            throw new DefinitionsFactoryException(ex);

        } catch (IllegalAccessException ex) {
            throw new DefinitionsFactoryException(ex);
        }

    }

    /**
     * Return String representation.
     * Calls toString() on underlying factory.
     * @return String representation.
     */
    public String toString() {
        return getInternalFactory().toString();
    }

    /**
     * Create map of configuration attributes from configuration object.
     * Mapping is done between old names and new names.
     * @param config The DefinitionsFactoryConfig to use.
     * @return Map Map of name/value pairs.
     */
    public static Map createConfigMap(DefinitionsFactoryConfig config) {
        Map map = new HashMap(config.getAttributes());
        // Add property attributes using old names
        map.put(
            DefinitionsFactoryConfig.DEFINITIONS_CONFIG_PARAMETER_NAME,
            config.getDefinitionConfigFiles());

        map.put(
            DefinitionsFactoryConfig.PARSER_VALIDATE_PARAMETER_NAME,
            (config.getParserValidate() ? Boolean.TRUE.toString() : Boolean.FALSE.toString()));

        if (!"org.apache.struts.tiles.xmlDefinition.I18nFactorySet"
            .equals(config.getFactoryClassname())) {

            map.put(
                DefinitionsFactoryConfig.FACTORY_CLASSNAME_PARAMETER_NAME,
                config.getFactoryClassname());
        }

        return map;
    }

}
