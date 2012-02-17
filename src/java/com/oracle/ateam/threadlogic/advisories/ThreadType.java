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
package com.oracle.ateam.threadlogic.advisories;

public enum ThreadType {

  SOA, IWAY, JMS_ADAPTER, AQ_ADAPTER, LDAP, COHERENCE, IAS_CACHE, ORCL, TIMER, MERCURY_DIAGNOSTICS, WILY, WLS_JMS, WLS_MUXER, WLS_EMBEDDED_LDAP, WLS_TIMER, WLS, JVM, GC, FINALIZER, MISC_VM, CUSTOM_POOL, UNKNOWN;
}
