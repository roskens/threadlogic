package com.oracle.ateam.threadlogic.monitors;

public class IBMMonitorMap extends MonitorMap {

  public IBMMonitorMap() {
    super();
  }

  public void parseAndAddThread(String line, String threadTitle, String currentThread) {
    if (line == null) {
      return;
    }
    if ((line.indexOf('@') > 0)) {
      String monitor = line.substring(line.indexOf('@'));
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
