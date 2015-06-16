/*
 * $Id: InitDefinitionsTag.java 471754 2006-11-06 14:55:09Z husted $
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

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.struts.tiles.DefinitionsFactory;
import org.apache.struts.tiles.DefinitionsFactoryConfig;
import org.apache.struts.tiles.DefinitionsFactoryException;
import org.apache.struts.tiles.TilesUtil;

  /**
   * Init definitions factory.
   */
public class InitDefinitionsTag extends TagSupport
    implements ComponentConstants {


  private String filename = null;
  private String classname = null;

  /**
   * Default constructor.
   */
  public InitDefinitionsTag() {
    super();
  }

    /**
     * Release all allocated resources.
     */
    public void release() {

        super.release();
        filename = null;
    }

    /**
     * Set file.
     */
  public void setFile(String name){
    this.filename = name;
  }

    /**
     * Set classname.
     */
  public void setClassname(String classname){
    this.classname = classname;
  }

    /**
     * Do start tag.
     */
  public int doStartTag() throws JspException
  {
  DefinitionsFactory factory =
      TilesUtil.getDefinitionsFactory(pageContext.getRequest(),
          pageContext.getServletContext());
  if(factory != null )
    return SKIP_BODY;

  DefinitionsFactoryConfig factoryConfig = new DefinitionsFactoryConfig();
  factoryConfig.setFactoryClassname( classname );
  factoryConfig.setDefinitionConfigFiles( filename );

  try
    {
    factory = TilesUtil.createDefinitionsFactory(
        pageContext.getServletContext(), factoryConfig);
    }
   catch( DefinitionsFactoryException ex )
      {
      ex.printStackTrace();
      throw new JspException( ex );
      }
  return SKIP_BODY;
  }

    /**
     * Do end tag.
     */
  public int doEndTag() throws JspException {
    return EVAL_PAGE;
  }

}
