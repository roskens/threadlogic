/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.ateam.threadlogic.filter;

import com.oracle.ateam.threadlogic.ThreadInfo;

import java.util.ArrayList;

/**
 * 
 * @author saparam
 */
public class HotCallPatternFilter extends Filter {

  String callPattern;

  public HotCallPatternFilter(String name, String pattern) {
    setName(name);
    this.callPattern = pattern;

  }

  public boolean matches(ThreadInfo ti, boolean forceEnabled) {

    boolean result = ti.getContent().contains(callPattern);
    return result;
  }

}
