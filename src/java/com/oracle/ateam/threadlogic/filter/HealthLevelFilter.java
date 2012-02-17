package com.oracle.ateam.threadlogic.filter;

import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.ThreadLogicElement;

public class HealthLevelFilter extends Filter {
  
  private HealthLevel health = HealthLevel.WATCH;
  
  public HealthLevelFilter() {
    setName("Minimum Health Level Filter");
    setEnabled(true);
    setGeneralFilter(true);
  }

  public boolean matches(ThreadLogicElement tle) {
    return tle.getHealth().ordinal() >= health.ordinal();
  }

  public HealthLevel getHealth() {
    return health;
  }

  public void setHealth(HealthLevel health) {
    this.health = health;
  }
}
