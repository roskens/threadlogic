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
public class WLSMuxerThreadGroup extends ThreadGroup {    
  
  public WLSMuxerThreadGroup(String grpName) {
    super(grpName);
  }
  
  public void runAdvisory() {    
    super.runAdvisory();    
    runWLSMuxerAdvisory();
  }
  
  public void runWLSMuxerAdvisory() {
    if (getThreads().size() > 6) {
      ThreadAdvisory advisory = ThreadAdvisory.lookupThreadAdvisory("WebLogicMuxerThreads");
      addAdvisory(advisory);
    }
  } 
  
  public static void resetAdvisoriesBasedOnThread(ThreadInfo threadInfo, ArrayList<ThreadAdvisory> advisoryList) {

    boolean isAtWatchLevel = (threadInfo.getHealth() == HealthLevel.WATCH);
    
    if (isAtWatchLevel && (threadInfo.getState() == ThreadState.BLOCKED)) {
      
      // Ensure the lock is also held by another muxer thread
      // thats the nromal behavior
      LockInfo blockedForLock = threadInfo.getBlockedForLock();
      ThreadInfo ownerOfLock = blockedForLock.getLockOwner();
      if (ownerOfLock != null) {

        String blockingThreadName = ownerOfLock.getName().toLowerCase();

        // System.out.println("Blocked for lock: " +
        // blockedForLock.getLockId() + ", owner of lock: " +
        // blockingThreadName);

        if (blockingThreadName.contains("muxer")) {
          threadInfo.setHealth(HealthLevel.NORMAL);
          return;
        } else {
          advisoryList.remove(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLSMUXER_PROCESS_SOCKETS));            
          threadInfo.setHealth(HealthLevel.FATAL);
        }
      }

      
    } else if ( (threadInfo.getState() == ThreadState.WAITING)
                || (threadInfo.getState() == ThreadState.PARKING) 
              ) {
              
        // Check if this is from a IBM JVM
        // IBM JVM makes the poller marks it as CW/Waiting state as its waiting natively in poll
        // If not IBM, add warning about Muxer blocked in a bad state
        if (!threadInfo.isIBMJVM()) {

          ThreadAdvisory warningAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.MUXER_WAITING);        
          advisoryList.add(warningAdvisory);
          threadInfo.setHealth(warningAdvisory.getHealth());
          advisoryList.remove(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLSMUXER_PROCESS_SOCKETS));

      } else {
        // If IBM, dont add warning about Muxer blocked in a bad state
        // Also remove WAITING_WHILE_BLOCKING Advisory that might have got added to the Muxer if it was IBM JVM 
        // and the muxer thread appears in Condition Wait while holding lock and blocking other muxer threads
        advisoryList.remove(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WAITING_WHILE_BLOCKING));    
      }
    }

    // Make sure the Muxer thread not executing or handling requests itself.
    // it should only be dispatching requests to sub-systems instead of handling job itself
    if (threadInfo.getContent().contains("WorkAdapterImpl.run")) {
      ThreadAdvisory warningAdvisory = ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLS_SUBSYSTEM_REQUEST_OVERFLOW);
      advisoryList.add(warningAdvisory);
      threadInfo.setHealth(warningAdvisory.getHealth());
      advisoryList.remove(ThreadAdvisory.lookupThreadAdvisory(ThreadLogicConstants.WLSMUXER_PROCESS_SOCKETS));
    }
  }

  
  
  /**
   * creates the overview information for this thread group.
   */
  protected void createOverview() {
  
  setOverview(getBaseOverview() + getCritOverview());
}
}
