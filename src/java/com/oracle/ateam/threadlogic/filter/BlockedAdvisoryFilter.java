/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.ateam.threadlogic.filter;

import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.ThreadState;

import java.util.ArrayList;

/**
 * 
 * @author saparam
 */
public class BlockedAdvisoryFilter extends Filter {

  public BlockedAdvisoryFilter(String name) {
    setName(name);

  }

  public boolean matches(ThreadInfo ti, boolean forceEnabled) {

    return (ti.getState() == ThreadState.BLOCKED);
  }

}
