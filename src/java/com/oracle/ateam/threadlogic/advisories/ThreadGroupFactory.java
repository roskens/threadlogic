/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.ateam.threadlogic.advisories;


/**
 *
 * @author saparam
 */
public class ThreadGroupFactory {
  
  public static ThreadGroup createThreadGroup(String grpName) {

    if (grpName.contains("SOA"))
      return new SOAThreadGroup(grpName);
    
    if (grpName.contains("Muxer"))
      return new WLSMuxerThreadGroup(grpName);
    
    if (grpName.contains("JVM"))
      return new JVMThreadGroup(grpName);
    
    return new ThreadGroup(grpName);
  }
}
