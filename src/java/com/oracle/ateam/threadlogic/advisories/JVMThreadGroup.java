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
import com.oracle.ateam.threadlogic.ThreadInfo;

/**
 *
 * @author saparam
 */
public class JVMThreadGroup extends ThreadGroup {    
  
  protected int gcThreads;
  protected boolean isFinalizerBlocked = false;
  
  public JVMThreadGroup(String grpName) {
    super(grpName);
  }
  
  public void runAdvisory() {    
    super.runAdvisory();    
    runJVMAdvisory();
  }
  
  public void runJVMAdvisory() {
    ArrayList<ThreadAdvisory> advisories = new ArrayList<ThreadAdvisory>();
    ArrayList<ThreadInfo> threads = this.getThreads();
    
    for (ThreadInfo thread : threads) {
      String threadName = thread.getName();
      if (threadName.startsWith(ThreadLogicConstants.FINALIZER_THREAD) && thread.isBlockedForLock()) {
        isFinalizerBlocked = true;
        thread.addAdvisory(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.FINALIZER_THREAD_BLOCKED));
        thread.setHealth(HealthLevel.FATAL);
        this.setHealth(HealthLevel.FATAL);
        advisories.addAll(thread.getAdvisories());

        continue;
      }

      if (threadName.toUpperCase().contains("GC ")) {
        ++gcThreads;
      }
    }

    if (gcThreads > 20) {
      ThreadAdvisory advisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.PARALLEL_GC_THREADS);
      advisories.add(advisory);
    }
    
    this.addAdvisories(advisories);
  }  
  
  public String getJVMOverview() {
    StringBuffer statData = new StringBuffer();
    statData.append("<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Number of Parallel GC Threads </td><td><b><font face=System>");
    statData.append(this.gcThreads);
    
    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Is Finalizer blocked </td><td><b><font face=System>");
    statData.append(this.isFinalizerBlocked);
    statData.append("</b></td></tr>\n\n");            

    /*
    statData.append("<tr bgcolor=\"#dddddd\"><td><font face=System "
        + ">Number of BPEL Engine Threads </td><td><b><font face=System>");
    statData.append(this.bpelEngineThreads);

    statData.append("</b></td></tr>\n\n<tr bgcolor=\"#eeeeee\"><td><font face=System "
        + ">Number of SOA JMS Consumer Threads </td><td><b><font face=System>");
    statData.append(this.soaJMSConsumerThreads);
     */
    
    statData.append("</b></td></tr>\n\n");
    return statData.toString();
  }  
  
  /**
   * creates the overview information for this thread group.
   */
  protected void createOverview() {
  
  setOverview(getBaseOverview() + getJVMOverview() + getCritOverview());
}
}
