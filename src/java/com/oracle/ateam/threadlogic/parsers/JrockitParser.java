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
 * $Id: BeaJDKParser.java,v 1.12 2010-01-03 14:23:09 irockel Exp $
 */

package com.oracle.ateam.threadlogic.parsers;

import com.oracle.ateam.threadlogic.monitors.JRockitMonitorMap;
import com.oracle.ateam.threadlogic.parsers.AbstractDumpParser.LineChecker;
import com.oracle.ateam.threadlogic.utils.DateMatcher;
import com.oracle.ateam.threadlogic.utils.IconFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 * Parses Bea/JRockit Thread Dumps
 * 
 * @author irockel
 */
public class JrockitParser extends AbstractDumpParser {
  // private boolean foundClassHistograms = false;
  // private boolean withCurrentTimeStamp = false;

  /**
   * constructs a new instance of a bea jdk parser
   * 
   * @param dumpFileStream
   *          the dump file stream to read.
   * @param threadStore
   *          the thread store to store the thread informations in.
   */
  public JrockitParser(BufferedReader bis, Map threadStore, int lineCounter, DateMatcher dm) {
    super(bis, dm);
    this.threadStore = threadStore;
    this.lineCounter = lineCounter;
    this.lineChecker = new LineChecker();
    this.lineChecker.setFullDumpPattern("(.*FULL THREAD DUMP.*)");
    this.lineChecker.setAtPattern("(.*at.*)");
    this.lineChecker.setWaitingOnPattern("(.*-- Waiting for notification on.*)");
    this.lineChecker.setParkingToWaitPattern("(.*-- Parking to wait for.*)");
    this.lineChecker.setWaitingToPattern("(.*-- Blocked trying to get lock.*)");
    this.lineChecker.setLockedPattern("(.*-- (Holding lock|Lock released while waiting).*)");
    this.lineChecker.setEndOfDumpPattern("(.*(END OF THREAD DUMP|Blocked lock chains|Open lock chains).*)");
  }

  protected void initVars() {
    LOCKED = "-- Holding lock:";
    BLOCKED_FOR_LOCK = "-- Blocked trying to get lock:";
    GENERAL_WAITING = "- Waiting for notification:";
  }

  /**
   * parse the next thread dump from the stream passed with the constructor.
   * 
   * @returns null if no more thread dumps were found.
   */

  public MutableTreeNode parseNext() {
    this.mmap = new JRockitMonitorMap();
    return super.parseNext();
  }

  protected String linkifyMonitor(String line) {
    if (line != null) {
      int colon = line.indexOf(":");
      int last = line.indexOf('[') >= 0 ? line.indexOf('[') : line.length();
      String begin = colon != -1 ? line.substring(0, colon + 2) : "";
      String monitor = colon != -1 ? line.substring(colon + 2, last) : line;
      String end = line.substring(last);
      monitor = "<a href=\"monitor://" + monitor + "\">" + monitor;
      monitor = monitor.substring(0, monitor.length()) + "</a>";
      return (begin + monitor + end);
    } else {
      return (line);
    }
  }

  public boolean isFoundClassHistograms() {
    // bea parser doesn't support class histograms
    return false;
  }

  public void parseLoggcFile(InputStream loggcFileStream, DefaultMutableTreeNode root) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  public void setDumpHistogramCounter(int value) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  /**
   * check if the passed logline contains the beginning of a Bea jdk thread
   * dump.
   * 
   * @param logLine
   *          the line of the logfile to test
   * @return true, if the start of a bea thread dump is detected.
   */
  public static boolean checkForSupportedThreadDump(String logLine) {
    return (logLine.trim().indexOf("===== FULL THREAD DUMP ===============") >= 0);
  }

  protected String[] getThreadTokens(String name) {
    String patternMask = "^.*\"([^\"]+)\".*id=([^ ]+).*tid=([^ ]+).*"
        + "prio=[^ ]+ ([^,]+,? ?[^,]+?,? ?[^,]+?,? ?[^,]+?)(, daemon)?$";
    
    String[] tokens = new String[] {};
    
    try {
      Pattern p = Pattern.compile(patternMask);
      Matcher m = p.matcher(name);

      System.out.println(m.matches());
      for (int iLoop = 1; iLoop < m.groupCount(); iLoop++) {
        System.out.println(iLoop + ": " + m.group(iLoop));
      }

      tokens = new String[7];
      tokens[0] = m.group(1); // name
      tokens[1] = m.group(3); // tid
      tokens[2] = m.group(2); // nid
      tokens[3] = m.group(4); // State

    } catch(Exception e) { 

      System.out.println("WARNING!! Unable to parse Thread Tokens with name:" + name  );
      e.printStackTrace();
    }

    return (tokens);
  }

  @Override
  public boolean checkForClassHistogram(DefaultMutableTreeNode threadDump) throws IOException {
    return false;
  }

  @Override
  public String linkifyDeadlockInfo(String line) {
    // TODO Auto-generated method stub
    return null;
  }
}
