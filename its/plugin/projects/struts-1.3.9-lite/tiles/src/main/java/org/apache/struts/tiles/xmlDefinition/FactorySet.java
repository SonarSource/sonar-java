/*
 * $Id: FactorySet.java 471754 2006-11-06 14:55:09Z husted $
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

package org.apache.struts.tiles.xmlDefinition;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;

import org.apache.struts.tiles.ComponentDefinition;
import org.apache.struts.tiles.ComponentDefinitionsFactory;
import org.apache.struts.tiles.DefinitionsFactoryException;
import org.apache.struts.tiles.FactoryNotFoundException;
import org.apache.struts.tiles.NoSuchDefinitionException;

/**
 * Component Definitions factory.
 * This factory contains several factories identified by a key. The
 * getDefinition() method first looks for the factory key, retrieves or creates this
 * factory and then calls its getDefinition().
 */
public abstract class FactorySet implements ComponentDefinitionsFactory
{

    /** Loaded factories */
  protected Map factories=null;

  /**
   * Extract key that will be used to get the sub factory.
   * @param name Name of requested definition.
   * @param request Current servlet request.
   * @param servletContext Current servlet context.
   * @return Object.
   */
  abstract protected Object getDefinitionsFactoryKey(String name, ServletRequest request, ServletContext servletContext);

  /**
   * Get default factory.
   * @return Default factory.
   */
  abstract protected DefinitionsFactory getDefaultFactory();

  /**
   * Get a factory by its key.
   * If key is <code>null</code>, return defaultFactory.
   * Search in loaded factories. If not found, create factory and store return value in
   * loaded factories.
   * @param key Key of requested definition.
   * @param request Current servlet request.
   * @param servletContext Current servlet context.
   * @throws DefinitionsFactoryException If an error occur while creating factory.
   */
  protected DefinitionsFactory getFactory(Object key, ServletRequest request, ServletContext servletContext)
    throws DefinitionsFactoryException
  {
  if(key == null )
    return getDefaultFactory();

  Object factory = factories.get( key );
  if( factory == null )
    {
      // synchronize creation to avoid double creation by separate threads.
      // Also, check if factory hasn't been created while waiting for synchronized
      // section.
    synchronized(factories)
      {
      factory = factories.get( key );
      if( factory == null )
        {
        factory = createFactory( key, request, servletContext);
        factories.put( key, factory );
        } // end if
      } // end synchronized
    } // end if
  return (DefinitionsFactory)factory;
  }

  /**
   * Get a definition by its name.
   *
   * @param name Name of requested definition.
   * @param request Current servlet request.
   * @param servletContext Current servlet context.
   * @throws NoSuchDefinitionException No definition found for specified name
   * @throws DefinitionsFactoryException General exception
   */
  public ComponentDefinition getDefinition(String name, ServletRequest request, ServletContext servletContext)
    throws NoSuchDefinitionException, DefinitionsFactoryException
  {
  if( factories == null )
    throw new FactoryNotFoundException( "No definitions factory defined" );

  Object key = getDefinitionsFactoryKey( name, request, servletContext);
  DefinitionsFactory factory = getFactory( key, request, servletContext);
  return factory.getDefinition( name, request, servletContext );
  }

  /**
   * Create a factory for specified key.
   * This method is called by getFactory() when the requested factory doesn't already exist.
   * Must return a factory, or a default one.
   * Real implementation needs to provide this method.
   * @param key Key of requested definition.
   * @param request Current servlet request.
   * @param servletContext Current servlet context
   * @throws DefinitionsFactoryException If an error occur while creating factory.
   */
  abstract protected DefinitionsFactory createFactory(Object key, ServletRequest request, ServletContext servletContext)
          throws DefinitionsFactoryException;

  /**
   * Init factory set.
   * @param servletContext Current servlet context
   * @param properties properties used to initialized factory set;
   */
  abstract public void initFactory(ServletContext servletContext, Map properties)
    throws DefinitionsFactoryException;

  /**
   * Constructor.
   */
  public FactorySet()
  {
  factories = new HashMap();
  }

    /**
     * Return String representation.
     * @return String representation.
     */
  public String toString()
    {
    Iterator i = factories.values().iterator();
    StringBuffer buff = new StringBuffer( "all FactorySet's factory : \n" );
    while( i.hasNext() )
      {
      buff.append( i.next().toString() ).append("\n");
      }
    return buff.toString();
    }

}
