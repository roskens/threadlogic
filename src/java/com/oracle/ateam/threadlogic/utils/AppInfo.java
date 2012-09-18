/**
 * Copyright (c) 2012 egross, sabha.
 * 
 * ThreadLogic - parses thread dumps and provides analysis/guidance
 * It is based on the popular TDA tool.  Thank you!
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */
/*
 * AppInfo.java
 *
 * This file is part of TDA - Thread Dump Analysis Tool.
 *
 * TDA is free software; you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * TDA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with TDA; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * $Id: AppInfo.java,v 1.11 2010-01-18 17:42:45 irockel Exp $
 */
package com.oracle.ateam.threadlogic.utils;

import java.io.InputStream;
import java.util.Properties;

/**
 * provides static application information like name and version
 * 
 * @author irockel
 */
public class AppInfo {
  private static final String APP_SHORT_NAME = "ThreadLogic";
  private static final String APP_FULL_NAME = "ThreadLogic - We'll do the analysis for you!";
  private static final String VERSION = "1.1";
  private static String FULL_VERSION;
  private static String BUILD_DATE;
  

  private static final String COPYRIGHT = "2012-2020";

  static {
    try {
        InputStream is = AppInfo.class.getResourceAsStream("/META-INF/MANIFEST.MF");
        Properties props = new Properties();
        props.load(is);
        FULL_VERSION = props.getProperty("Build-Version");
        BUILD_DATE = props.getProperty("Built-Date");        
      } catch(Exception e) {
      }
    
      if (FULL_VERSION == null)        
        FULL_VERSION = VERSION;
      
      if (BUILD_DATE == null)
        BUILD_DATE = "";
  }
    
  /**
   * get info text for status bar if no real info is displayed.
   */
  public static String getStatusBarInfo() {
    return (APP_FULL_NAME + " " + FULL_VERSION + " " + BUILD_DATE);
  }

  public static String getAppInfo() {
    return (APP_FULL_NAME);
  }

  public static String getVersion() {
    return FULL_VERSION;
  }

  public static String getCopyright() {
    return (COPYRIGHT);
  }
}
