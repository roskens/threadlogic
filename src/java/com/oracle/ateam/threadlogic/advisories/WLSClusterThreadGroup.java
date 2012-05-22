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
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.ateam.threadlogic.advisories;

import java.util.ArrayList;

import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.LockInfo;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.ThreadState;

/**
 *
 * @author saparam
 */
public class WLSClusterThreadGroup extends ThreadGroup {    
  
  public WLSClusterThreadGroup(String grpName) {
    super(grpName);
  }
  
  public void runAdvisory() {    
    super.runAdvisory();    
    runWLSClusterAdvisory();
  }
  
  public void runWLSClusterAdvisory() {
    if (getThreads().size() > 5) {
      ThreadAdvisory advisory = ThreadAdvisory.lookupThreadAdvisory("WLS Clustering unhealthy");
      addAdvisory(advisory);
      if (this.getHealth().ordinal() < advisory.getHealth().ordinal());
        this.setHealth(advisory.getHealth());
        
      for(ThreadInfo ti: threads) {
        ti.addAdvisory(advisory);
        if (ti.getHealth().ordinal() < advisory.getHealth().ordinal()) {
          ti.setHealth(advisory.getHealth());
        }
      }      
    }
  } 
  
  
  /**
   * creates the overview information for this thread group.
   */
  protected void createOverview() {

    setOverview(getBaseOverview() + getCritOverview());
  }
}
