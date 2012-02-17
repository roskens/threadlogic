/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.ateam.threadlogic.filter;

import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;

import java.util.ArrayList;

/**
 * 
 * @author saparam
 */
public class HealthLevelAdvisoryFilter extends Filter {

  HealthLevel minHealthLevel;

  public HealthLevelAdvisoryFilter(String name, HealthLevel minHealthLevel) {
    setName(name);
    this.minHealthLevel = minHealthLevel;
  }

  public boolean matches(ThreadInfo ti, boolean forceEnabled) {

    ArrayList<ThreadAdvisory> advisories = ti.getAdvisories();
    for (ThreadAdvisory adv : advisories) {
      if (adv.getHealth().ordinal() >= minHealthLevel.ordinal())
        return true;
    }
    return false;
  }

  public ArrayList<ThreadAdvisory> getCriticalAdvisories(ThreadInfo ti) {
    ArrayList<ThreadAdvisory> advisories = ti.getAdvisories();
    ArrayList<ThreadAdvisory> critAdvisories = new ArrayList<ThreadAdvisory>();
    for (ThreadAdvisory adv : advisories) {
      if (adv.getHealth().ordinal() >= minHealthLevel.ordinal())
        critAdvisories.add(adv);
    }

    return critAdvisories;
  }
}

