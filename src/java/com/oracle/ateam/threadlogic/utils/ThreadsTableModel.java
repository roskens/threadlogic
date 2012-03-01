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
 * ThreadsTableModel.java
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
 * $Id: ThreadsTableModel.java,v 1.6 2008-04-27 20:31:14 irockel Exp $
 */
package com.oracle.ateam.threadlogic.utils;

import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;

import java.math.BigInteger;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * table model for displaying thread overview.
 * 
 * @author irockel
 */
public class ThreadsTableModel extends AbstractTableModel {

  protected Vector elements;

  protected String[] columnNames;

  /**
   * 
   * @param root
   */
  public ThreadsTableModel(DefaultMutableTreeNode rootNode) {
    // transform child nodes in proper vector.
    if (rootNode != null) {
      elements = new Vector();
      for (int i = 0; i < rootNode.getChildCount(); i++) {
        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) rootNode.getChildAt(i);
        elements.add(childNode.getUserObject());
        if (columnNames == null) {
          Object entry = childNode.getUserObject();
          if (entry instanceof ThreadInfo) {

            ThreadInfo ti = (ThreadInfo) entry;
            if (ti.getTokens().length > 3) {
              columnNames = new String[] { "Name", "Thread Group", "Health", "Advisories", "Native-ID", "Thread-ID", "State", };
            } else {
              columnNames = new String[] { "Name", "Thread-ID", "State" };
            }
          }
        }
      }
    }
  }

  public String getColumnName(int col) {
    return columnNames[col];
  }

  public int getRowCount() {
    return (elements.size());
  }

  public int getColumnCount() {
    return (columnNames.length);
  }

  public Object getValueAt(int rowIndex, int columnIndex) {
    ThreadInfo ti = ((ThreadInfo) elements.elementAt(rowIndex));

    String[] columns = ti.getTokens();
    if (getColumnCount() > 3) {
      // if (columnIndex > 1 && columnIndex < 5) {
      // return new Long(columns[columnIndex]);
      // / } else {

      switch(columnIndex) {
        case (0): 
          String scrubbedName = columns[0].replaceAll("\\[.*\\] ", ""); 
          return scrubbedName;
        case (1): return ti.getThreadGroup().getThreadGroupName();  
        case (2): return ti.getHealth();  
        case (3): return getAdvisoryNames(ti);        
        case (6): return ti.getState();
        default:
          String val = columns[columnIndex - 3];
          if (val== null || val.equals("?"))
            return null;
          
          if (val.startsWith("0x"))
            return new BigInteger(val.substring(2), 16);
          
          return new BigInteger(val);
      }      
      
    } else {
      if (columnIndex == 1) {
        return new BigInteger(columns[columnIndex]);
      } else {
        return columns[columnIndex];
      }
    }
  }

  private String getAdvisoryNames(ThreadInfo ti) {
    StringBuffer sbuf = new StringBuffer();
    boolean firstEntry = true;
    for (ThreadAdvisory tdadv : ti.getAdvisories()) {
      // if (tdadv.getHealth().ordinal() >= HealthLevel.WATCH.ordinal()) {
      if (!firstEntry)
        sbuf.append(", ");
      sbuf.append(tdadv.getPattern());
      firstEntry = false;
      // }
    }
    return sbuf.toString();    
  }
  
  /**
   * get the thread info object at the specified line
   * 
   * @param rowIndex
   *          the row index
   * @return thread info object at this line.
   */
  public Object getInfoObjectAtRow(int rowIndex) {
    return (rowIndex >= 0 && rowIndex < getRowCount() ? elements.get(rowIndex) : null);
  }

  /**
   * @inherited
   */
  public Class getColumnClass(int columnIndex) {
    if (columnIndex > 3 && columnIndex < 6) {
      return BigInteger.class;
    } else {
      return String.class;
    }
  }

  /**
   * search for the specified (partial) name in thread names
   * 
   * @param startRow
   *          row to start the search
   * @param name
   *          the (partial) name
   * @return the index of the row or -1 if not found.
   */
  public int searchRowWithName(int startRow, String name) {
    int i = startRow;
    boolean found = false;
    while (!found && (i < getRowCount())) {
      ThreadInfo ti = ((ThreadInfo) getInfoObjectAtRow(i++));
      if (ti == null)
        continue;
      found = ti.getName().indexOf(name) >= 0;
      
    }

    return (found ? i - 1 : -1);
  }

}
