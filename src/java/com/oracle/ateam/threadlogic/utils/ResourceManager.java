/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.oracle.ateam.threadlogic.utils;

import java.util.ResourceBundle;

/**
 * 
 * @author irockel
 */
public class ResourceManager {
  private static ResourceBundle locale;

  public static String translate(String key) {
    if (locale == null) {
      locale = ResourceBundle.getBundle("com/oracle/ateam/threadlogic/locale");
    }

    return (locale.getString(key));
  }

}
