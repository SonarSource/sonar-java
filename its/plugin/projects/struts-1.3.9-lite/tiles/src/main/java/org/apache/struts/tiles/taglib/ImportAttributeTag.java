/*
 * $Id: ImportAttributeTag.java 504721 2007-02-07 22:22:21Z bayard $
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


package org.apache.struts.tiles.taglib;

import java.util.Iterator;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.struts.tiles.taglib.util.TagUtils;
import org.apache.struts.tiles.ComponentContext;


/**
  *  Import attribute from component to requested scope.
  *  Attribute name and scope are optional. If not specified, all component
  *  attributes are imported in page scope.
 */

public class ImportAttributeTag extends TagSupport {

    /**
     * Class name of object.
     */
    private String  name = null;


    /**
     * The scope name.
     */
    private String scopeName = null;

    /**
     * The scope value.
     */
    private int scope = PageContext.PAGE_SCOPE;
    /**
     * Are errors ignored. This is the property for attribute
     * <code>ignore</code>.
     * Default value is <code>false</code>, which throws an exception.
     * Only "attribute not found" - errors are ignored.
     */
  protected boolean isErrorIgnored = false;


    /**
     * Release all allocated resources.
     */
    public void release() {

        super.release();
        name = null;
        scopeName = null;
        scope = PageContext.PAGE_SCOPE;
        isErrorIgnored = false;
    }

    /**
     * Get the name.
     * @return Name.
     */
    public String getName()
     {
     return (this.name);
     }


    /**
     * Set the name.
     * @param name The new name
     */
    public void setName(String name)
     {
     this.name = name;
     }

    /**
     * Set the scope.
     * @param scope Scope.
     */
    public void setScope(String scope)
      {
      this.scopeName = scope;
      }

    /**
     * Get scope.
     * @return Scope.
     */
  public String getScope()
  {
  return scopeName;
  }

    /**
     * Set ignore flag.
     * @param ignore default: <code>false</code>: Exception is thrown when
     * attribute is not found, set to <code>
     * true</code> to ignore missing attributes silently
     */
  public void setIgnore(boolean ignore)
    {
    this.isErrorIgnored = ignore;
    }

    /**
     * Get ignore flag.
     * @return default: <code>false</code>: Exception is thrown when attribute
     * is not found, set to <code>
     * true</code> to ignore missing attributes silently
     */
  public boolean getIgnore()
  {
  return isErrorIgnored;
  }

    // --------------------------------------------------------- Public Methods


    /**
     * Expose the requested property from component context.
     *
     * @exception JspException On errors processing tag.
     */
public int doStartTag() throws JspException
    {
      // retrieve component context
    ComponentContext compContext =
        (ComponentContext)pageContext.getAttribute(
            ComponentConstants.COMPONENT_CONTEXT, PageContext.REQUEST_SCOPE);
    if( compContext == null )
        throw new JspException ( "Error - tag importAttribute : "
            + "no tiles context found." );

      // set scope
    scope = TagUtils.getScope( scopeName, PageContext.PAGE_SCOPE );

      // push attribute in requested context.
    if( name != null )
      {
      Object value = compContext.getAttribute(name);
        // Check if value exist and if we must send a runtime exception
      if( value == null ) 
        {
        if(!isErrorIgnored) 
          {
          throw new JspException ( "Error - tag importAttribute : property '"+
              name + "' not found in context. Check tag syntax" );
          }
        }
       else 
        {
        pageContext.setAttribute(name, value, scope);
        }
      }
     else
      { // set all attributes
      Iterator names = compContext.getAttributeNames();
      while(names.hasNext())
        {
        String name = (String)names.next();
        if(name == null ) {
          if(!isErrorIgnored)
            throw new JspException ( "Error - tag importAttribute : "
                + "encountered an attribute with key 'null'" );
          else
            return SKIP_BODY;
        }

        Object value = compContext.getAttribute(name);
        // Check if value exist and if we must send a runtime exception
        if( value == null ) {
          if(!isErrorIgnored) {
            throw new JspException ( "Error - tag importAttribute : property '"
                + name + "' has a value of 'null'" );
          }
        }
        pageContext.setAttribute(name, value, scope);
        } // end loop
      } // end else

      // Continue processing this page
    return SKIP_BODY;
    }

    /**
     * Clean up after processing this enumeration.
     *
     * @exception JspException On errors processing tag.
     */
  public int doEndTag() throws JspException
    {
    return (EVAL_PAGE);
    }

}
