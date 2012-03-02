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
 * DumpParserFactory.java
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
 * $Id: DumpParserFactory.java,v 1.11 2008-02-14 14:36:08 irockel Exp $
 */

package com.oracle.ateam.threadlogic.parsers;

import com.oracle.ateam.threadlogic.utils.DateMatcher;
import com.oracle.ateam.threadlogic.utils.PrefManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Vector;

/**
 * Factory for the dump parsers.
 * 
 * @author irockel
 */
public class DumpParserFactory {
  private static DumpParserFactory instance = null;
  private static final int HOTSPOT_VM = 0;
  private static final int JROCKIT_VM = 1;
  private static final int IBM_VM = 2;
  
  // Search for keyword jrockit or ibm specific tags in thread dumps if search for markers fail
  // Default to Hotspot for everything for else
  private static final String JROCKIT_TAG = "jrockit";
  
  // Even if there are ibm markers, harder to parse a wlst generated IBM Thread dump with the IBMParser
  // use the Sun HotspotParser still as special case as adding the whole file/markers is difficult...
  private static final String IBM_TAG = "com.ibm.misc.SignalDispatcher";
  
  private static final int[] VM_ID_LIST = { HOTSPOT_VM, JROCKIT_VM, IBM_VM };
  
  private static final String DEFAULT_HOTSPOT_MARKER = "Full thread dump Java HotSpot(TM)\n\n";

  private static final String DEFAULT_JROCKIT_MARKER = "===== FULL THREAD DUMP ===============\n\n";
  
  // Pass the Sun Markers also for IBM as its difficult for normal IBM parser to parse threads without full details normally present in IBM dump
  private static final String[] DEFAULT_MARKERS = { DEFAULT_HOTSPOT_MARKER, DEFAULT_JROCKIT_MARKER, DEFAULT_HOTSPOT_MARKER };
  
  private static final Vector<File> tempFileList = new Vector<File>();


  /**
   * singleton private constructor
   */
  private DumpParserFactory() {
  }

  /**
   * get the singleton instance of the factory
   * 
   * @return singleton instance
   */
  public static DumpParserFactory get() {
    if (instance == null) {
      instance = new DumpParserFactory();
    }

    return (instance);
  }
  
  protected void addToTempFileList(File tempFile) {
    tempFileList.add(tempFile);
  }
  
  protected void finalize() {
    // Clean up all temporary files created with additional markers...
    for(File tmpFile: tempFileList) {
      tmpFile.delete();      
    }
    tempFileList.clear();
  }

  /**
   * parses the given logfile for thread dumps and return a proper jdk parser
   * (either for Sun VM's or for JRockit/Bea VM's) and initializes the
   * DumpParser with the stream.
   * 
   * @param dumpFileStream
   *          the file stream to use for dump parsing.
   * @param threadStore
   *          the map to store the found thread dumps.
   * @param withCurrentTimeStamp
   *          only used by SunJDKParser for running in JConsole-Plugin-Mode, it
   *          then uses the current time stamp instead of a parsed one.
   * @return a proper dump parser for the given log file, null if no proper
   *         parser was found.
   */
  public DumpParser getDumpParserForLogfile(InputStream dumpFileStream, Map threadStore, boolean withCurrentTimeStamp,
      int startCounter) {
    return getDumpParserForLogfile(dumpFileStream, threadStore, withCurrentTimeStamp, startCounter, false, -1);
  }
    
  /**
   * parses the given logfile for thread dumps and return a proper jdk parser
   * (either for Sun VM's or for JRockit/Bea VM's) and initializes the
   * DumpParser with the stream.
   * 
   * @param dumpFileStream
   *          the file stream to use for dump parsing.
   * @param threadStore
   *          the map to store the found thread dumps.
   * @param withCurrentTimeStamp
   *          only used by SunJDKParser for running in JConsole-Plugin-Mode, it
   *          then uses the current time stamp instead of a parsed one.
   * @param retryWithMarkers
   *          Add additional markers based on JVM Type and recreate stream if earlier try failed due to absence of markers
   * @return a proper dump parser for the given log file, null if no proper
   *         parser was found.
   */    
  private DumpParser getDumpParserForLogfile(InputStream dumpFileStream, Map threadStore, boolean withCurrentTimeStamp,
      int startCounter, boolean retryWithMarkers, int nativeVMType) {  
    BufferedReader bis = null;
    int readAheadLimit = PrefManager.get().getStreamResetBuffer();
    int lineCounter = 0;
    DumpParser currentDumpParser = null;

    // Default to Hotspot unless we find any jrockit or ibm tags..
    int vmType = HOTSPOT_VM;
    boolean determinedJVMType = false;
    
    try {
      bis = new BufferedReader(new InputStreamReader(dumpFileStream));

      // reset current dump parser
      DateMatcher dm = new DateMatcher();
      boolean foundDate = false;
      String dateEntry = "";
      while (bis.ready() && (currentDumpParser == null)) {
        bis.mark(readAheadLimit);
        String line = bis.readLine();
        if (!foundDate) {
          dm.checkForDateMatch(line);          
          if (dm.isDefaultMatches()) {
            dateEntry = line;
            foundDate = true;
            System.out.println("Timestamp:" + dateEntry);
          } 
        }
        
        if (!determinedJVMType) {
          if (line.indexOf(JROCKIT_TAG) > 0) {                
            vmType = JROCKIT_VM;
            determinedJVMType = true;
          } else if (line.indexOf(IBM_TAG) > 0) {
            vmType = IBM_VM;
            determinedJVMType = true;
          }
           
        }
          
        if (WrappedSunJDKParser.checkForSupportedThreadDump(line)) {
          currentDumpParser = new WrappedSunJDKParser(bis, threadStore, lineCounter, withCurrentTimeStamp,
              startCounter, dm);
        } else if (HotspotParser.checkForSupportedThreadDump(line)) {
          if (nativeVMType  == IBM_VM ) {
            boolean isNonNativeHotspot = true;
            currentDumpParser = new HotspotParser(bis, threadStore, lineCounter, withCurrentTimeStamp, startCounter, dm, isNonNativeHotspot);          
          } else {
            currentDumpParser = new HotspotParser(bis, threadStore, lineCounter, withCurrentTimeStamp, startCounter, dm);          
          }
        } else if (JrockitParser.checkForSupportedThreadDump(line)) {
          currentDumpParser = new JrockitParser(bis, threadStore, lineCounter, dm);
        } else if (IBMJDKParser.checkForSupportedThreadDump(line)) {
          currentDumpParser = new IBMJDKParser(bis, threadStore, lineCounter, withCurrentTimeStamp, startCounter, dm);
        }
        lineCounter++;
      }
      // System.out.println("Selected Dump Parser: " +
      // currentDumpParser.getClass().getName());
      if ((currentDumpParser != null) && (bis != null)) {
        bis.reset();
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    }
    
    if ((currentDumpParser == null) && (!retryWithMarkers)) {
      
      System.out.println("Unable to find any associated markers from the thread dumps to parse the given thread dump");
      System.out.println("Will Attempt to add additional markers to help with parsing based on details from thread dump");
      String jvmTypeStr = "Sun Hotspot";
      switch(vmType) {
        case JROCKIT_VM: jvmTypeStr = "JRockit"; break;
        case IBM_VM: jvmTypeStr = "IBM"; break;
      }
    
    System.out.println("Creating temporary markers to match Thread Dumps of JVM Type: " + jvmTypeStr);
    System.out.println("\nWARNING!!!Wont be able to accurately parse and report on locks/dates/jvm versions due to limitations\n");
    
      try {
        dumpFileStream.reset();
        InputStream recreatedStream = cloneStreamWithMarkers(vmType, dumpFileStream);
        return getDumpParserForLogfile(recreatedStream, threadStore, withCurrentTimeStamp, startCounter, true, vmType);
      } catch(IOException ie) { }   
    }
    return currentDumpParser;
  }
  
  
  /**
   * Clone the stream and add additional markers to treat as full thread dump
   * for cases where the markers are missing
   */
  public InputStream cloneStreamWithMarkers(int VM_TYPE, InputStream is) throws IOException {
    try {
      File tmpFile = File.createTempFile("tlogic.tmp.", ".log");
      tmpFile.deleteOnExit();
      addToTempFileList(tmpFile);
      
      // Add the markers based on VM Type
      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
      bos.write(DEFAULT_MARKERS[VM_TYPE].getBytes());
      
      int read = 0;
      byte[] barr = new byte[5120];      
      while ( (read = is.read(barr, 0, 5120)) > 0) {
        bos.write(barr);
        bos.flush();
      }
      bos.flush();
      bos.close();

      BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tmpFile));    
      return bis;
    } catch(IOException e) {
      System.out.println("Unable to create a temporary file to wrap original content with additional markers: " + e.getMessage());
      e.printStackTrace();
      throw e;
    }
    
    

  }
  
}
