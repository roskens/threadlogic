package com.oracle.ateam.threadlogic.monitors;

public class JRockitMonitorMap extends MonitorMap {

  public JRockitMonitorMap() {
    super();
  }

  public void parseAndAddThread(String line, String threadTitle, String currentThread) {
    if (line == null) {
      return;
    }
    if ((line.indexOf(":") > 0)) {
      int end = line.indexOf("[");
      String monitor = line.substring(line.indexOf(":") + 2, end != -1 ? end : line.length());
      if (line.trim().startsWith("-- Blocked trying") || line.trim().startsWith("- Parking to wait")) {
        addWaitToMonitor(monitor, threadTitle, currentThread);
      } else if (line.trim().startsWith("-- Waiting for notification on")) {
        addSleepToMonitor(monitor, threadTitle, currentThread);
      } else {
        addLockToMonitor(monitor, threadTitle, currentThread);
      }
    }
  }
}
