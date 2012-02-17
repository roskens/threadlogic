package com.oracle.ateam.threadlogic;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;

public enum HealthLevel {

  IGNORE, NORMAL, WATCH, WARNING, FATAL;

  public String getBackgroundRGBCode() {
    switch (this) {
    case WATCH:
      // pale orange
      return "rgb(247, 181, 49)";
    case WARNING:
      // orange
      return "rgb(248, 116, 49)";
    case FATAL:
      // red
      return "rgb(255, 60, 60)";
    }
    return "rgb(72,138,199)";
  }

  public Color getColor() {
    switch (this) {
    case WATCH:
      // pale orange
      return new Color(247, 181, 49);
    case WARNING:
      // orange
      return new Color(248, 116, 49);
    case FATAL:
      // red
      return new Color(255, 60, 60);
    }
    // return blue
    return new Color(72, 138, 199);
  }

};
