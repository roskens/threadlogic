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
package com.oracle.ateam.threadlogic.categories;


import com.oracle.ateam.threadlogic.filter.*;
import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.advisories.ThreadLogicConstants;
import com.oracle.ateam.threadlogic.ThreadInfo;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup;
import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroupFactory;
import com.oracle.ateam.threadlogic.advisories.ThreadGroup.HotCallPattern;
import com.oracle.ateam.threadlogic.xml.ComplexGroup;
import com.oracle.ateam.threadlogic.xml.GroupsDefnParser;
import com.oracle.ateam.threadlogic.xml.SimpleGroup;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import javax.swing.tree.DefaultMutableTreeNode;

public class ExternalizedNestedThreadGroupsCategory extends NestedCategory {

  private Category threads;
  private String overview = null;
  private ThreadGroup unknownThreadGroup;
  
  private CompositeFilter wlsCompositeFilter;
  private CompositeFilter nonwlsCompositeFilter;
  private CompositeFilter unknownCompositeFilter;
  private NestedCategory nestedWLSCategory, nestedNonWLSCategory;
  
  private LinkedList<ThreadInfo> threadLinkedList = new LinkedList<ThreadInfo>();
  private ArrayList<ThreadGroup> threadGroupList = new ArrayList<ThreadGroup>();
  private ArrayList<ThreadGroup> wlsThreadGroupList = new ArrayList<ThreadGroup>();
  private ArrayList<ThreadGroup> nonWlsThreadGroupList = new ArrayList<ThreadGroup>();
   
  
  private Filter ldapThreadsFilter, muxerThreadsFilter, aqAdapterThreadsFilter;
  
  private ArrayList<Filter> allWLSFilterList, allNonWLSFilterList;
  private static ArrayList<Filter> allNonWLSStaticFilterList, allWLSStaticFilterList;
  
  private Filter wlsJMSFilter = new Filter("WLS JMS Threads", "(weblogic.jms)|(weblogic.messaging)", 2, false, false, true);
  
  private Filter allWLSThreadsFilter1 = new Filter("WLS Threads1", 
                                                    "(weblogic)|(oracle.integration)|(com.octetstring.vde)|(orabpel)|(dms)|"
                                                    + "(HTTPClient)|(oracle.integration)|(oracle.mds)|(oracle.ias)|(oracle)|"
                                                    + "(com.tangosol.coherence)", 
                                                    2, false, false, true);
  
  private Filter allWLSThreadsFilter2 = new Filter("WLS Threads2", 
                                                    "(Weblogic)|(orabpel)|(weblogic)|(oracle.dfw)|(JPS)|(WsMgmt)|(Fabric)", 
                                                    0, false, false, true);
  
  
  public static String ADVISORY_PATH_SEPARATOR = "|";
  public static String DICTIONARY_KEYS;
  public static String THREADTYPEMAPPER_KEYS;
  public static final Hashtable<String, Filter> allKnownFilterMap = new Hashtable<String, Filter>();

  static {
    init();
  }

  // Cache the Group Definitions and clone the saved filters...
  // instead of reading each time... for each TD 
  
  private static void init() {
    allWLSStaticFilterList = createFilterList(ThreadLogicConstants.WLS_THREADGROUP_DEFN_XML);
    allNonWLSStaticFilterList = createFilterList(ThreadLogicConstants.NONWLS_THREADGROUP_DEFN_XML);
  }

  private static ArrayList<Filter> createFilterList(String groupsDefnXml) {
    ArrayList<Filter> filterArr = new ArrayList<Filter>();
    try {
      ClassLoader cl = ThreadLogicConstants.class.getClassLoader();
      
      System.out.println("Attempting to load GroupsDefn from file: " + groupsDefnXml);
      
      GroupsDefnParser groupsDefnParser = new GroupsDefnParser(cl.getResourceAsStream(groupsDefnXml));
      groupsDefnParser.run();
      ArrayList<SimpleGroup> simpleGrpList = groupsDefnParser.getSimpleGrpList();
      ArrayList<ComplexGroup> complexGrpList = groupsDefnParser.getComplexGrpList();

      for (SimpleGroup smpGrp : simpleGrpList) {
        generateSimpleFilter(smpGrp, filterArr);
      }

      for (ComplexGroup cmplxGrp : complexGrpList) {
        generateCompositeFilter(cmplxGrp, filterArr);
      }

    } catch (Exception e) {
      System.out.println("Unable to load or parse the Group Definition Resource:" + e.getMessage());
      e.printStackTrace();
    }
    return filterArr;
  }

  private static void generateSimpleFilter(SimpleGroup smpGrp, ArrayList<Filter> filterList) {

    String filterName = smpGrp.getName() + " Threads";
    ArrayList<String> patternList = smpGrp.getPatternList();

    String pattern = "";
    int count = patternList.size();
    if (count <= 0) {
      return;
    }

    if (count == 1) {

      pattern = patternList.get(0);

    } else if (count > 1) {

      StringBuffer sbuf = new StringBuffer("(" + patternList.get(0) + ")");
      for (int i = 1; i < count; i++) {
        sbuf.append("|(" + patternList.get(i) + ")");
      }
      pattern = sbuf.toString();
    }

    int filterRuleToApply = Filter.HAS_IN_STACK_RULE;
    if (smpGrp.getMatchLocation().equals("name")) {
      filterRuleToApply = Filter.HAS_IN_TITLE_RULE;
    }

    Filter simpleFilter = new Filter(filterName, pattern, filterRuleToApply, false, false, smpGrp.isInclusion());
    simpleFilter.setExcludedAdvisories(smpGrp.getExcludedAdvisories());
    simpleFilter.setInfo(filterName);

    //System.out.println("SimpleFilter:" + filterName + ", patternList:" + pattern);    
    allKnownFilterMap.put(filterName, simpleFilter);

    if (smpGrp.isVisible()) {
      filterList.add(simpleFilter);
    }

    return;
  }

  private static void generateCompositeFilter(ComplexGroup cmplxGrp, ArrayList<Filter> filterList) {
    String filterName = cmplxGrp.getName() + " Threads";

    CompositeFilter compositeFilter = new CompositeFilter(filterName);
    compositeFilter.setExcludedAdvisories(cmplxGrp.getExcludedAdvisories());
    compositeFilter.setInfo(filterName);

    for (String simpleGrpKey : cmplxGrp.getInclusionList()) {
      Filter simpleFilter = allKnownFilterMap.get(simpleGrpKey + " Threads");
      if (simpleFilter == null) {
        System.out.println("ERROR: Simple Group referred by name:" + simpleGrpKey + " not declared previously or name mismatch!!, Fix the error");
        Thread.dumpStack();
        continue;
      }

      compositeFilter.addFilter(simpleFilter, true);
    }

    for (String simpleGrpKey : cmplxGrp.getExclusionList()) {
      Filter simpleFilter = allKnownFilterMap.get(simpleGrpKey + " Threads");
      if (simpleFilter == null) {
        System.out.println("ERROR: Simple Group referred by name:" + simpleGrpKey + " not declared previously or name mismatch!!, Fix the error");
        Thread.dumpStack();
        continue;
      }

      compositeFilter.addFilter(simpleFilter, false);
    }

    allKnownFilterMap.put(filterName, compositeFilter);

    if (cmplxGrp.isVisible()) {
      filterList.add(compositeFilter);
    }

    return;
  }

  private static void createFilter(String groupDefinition, ArrayList<Filter> filterList) {

    String[] nameDefinition = groupDefinition.split("=");

    String filterName = nameDefinition[0] + " Threads";
    String filterDefinition = nameDefinition[1];

    String[] tokens = filterDefinition.split(":");
    boolean displayGroup = tokens[1].trim().startsWith("YES");

    // Check if its  a simple group definition
    if (filterDefinition.startsWith("SIM")) {

      /*
       * Sample entry for simple filter types...
       * IWay Adapter=SIMPLE:YES:com.ibi.adapters.util:STACK:INCLUDE
       * SAP Connector=SIMPLE:YES:com.sap.conn.jco:STACK:INCLUDE
       */

      String pattern = tokens[2].trim();

      // The pattern itself can be comprised of multiple sets separated by |
      // wrap the keys with ()

      String[] patternList = new String[]{};
      if (pattern.contains("|")) {
        patternList = pattern.split("\\|");

        int count = patternList.length;
        StringBuffer sbuf = new StringBuffer("(" + patternList[0] + ")");
        for (int i = 1; i < count; i++) {
          sbuf.append("|(" + patternList[i] + ")");
        }
        pattern = sbuf.toString();
      }

      boolean includeType = tokens[4].trim().toLowerCase().startsWith("inc");
      String searchAgainst = tokens[3].trim().toLowerCase();

      int filterRuleToApply = Filter.HAS_IN_STACK_RULE;
      if (searchAgainst.equals("name")) {
        filterRuleToApply = Filter.HAS_IN_TITLE_RULE;
      }

      Filter simpleFilter = new Filter(filterName, pattern, filterRuleToApply, false, false, includeType);
      simpleFilter.setInfo(filterName);
      //System.out.println("Created Simple Filter:" + filterName + ", with Pattern: " +  pattern
      //        + ", against Filter rule: " + filterRuleToApply + ", for inclusion?: " + includeType);
      allKnownFilterMap.put(filterName, simpleFilter);

      if (displayGroup) {
        filterList.add(simpleFilter);
      }

      return;
    }

    // This is a complex group definition using Composite filter
    /*
     * # NAME OF COMPLEX Group=COMPLEX:Should be displayed [YES|NO]#SIMPLE_GROUP_NAME:INCLUDE|EXCLUDE|SIMPLE_GROUP_NAME:INCLUDE|EXCLUDE
     * # Example: SOA Threads=COMPLEX:YES#IWay Adapter:EXCLUDE|BPEL:INCLUDE
     */

    //System.out.println("Complex Group Name:" + filterName + ", filter Defn: " + filterDefinition);
    String simpleGroupList = filterDefinition.split("#")[1];
    String[] simpleGroups = simpleGroupList.split("\\|");

    CompositeFilter compositeFilter = new CompositeFilter(filterName);
    compositeFilter.setInfo(filterName);

    for (String group : simpleGroups) {
      // Include or Exclude the filter associated with that referred simple group
      String[] simpleGrpTokens = group.split(":");
      Filter simpleFilter = allKnownFilterMap.get(simpleGrpTokens[0] + " Threads");
      if (simpleFilter == null) {
        System.out.println("ERROR: Simple Filter referred by name:" + simpleGrpTokens[0] + " not declared previously or name mismatch!!, Fix the error");
        Thread.dumpStack();
        continue;
      }

      boolean includeType = simpleGrpTokens[1].trim().toLowerCase().startsWith("inc");

      //System.out.println("Referred Simple Filter:" + simpleFilter + ", for inclusion?: " + includeType);

      compositeFilter.addFilter(simpleFilter, includeType);
    }
    allKnownFilterMap.put(filterName, compositeFilter);

    if (displayGroup) {
      //System.out.println("Adding Filter:" + filterName + " to top displayed list");
      filterList.add(compositeFilter);
    }
  }

  private void cloneDefinedFilters() {

    allWLSFilterList = new ArrayList<Filter>();
    allNonWLSFilterList = new ArrayList<Filter>();

    for (Filter filter : allNonWLSStaticFilterList) {
      allNonWLSFilterList.add(filter);
    }

    for (Filter filter : allWLSStaticFilterList) {
      allWLSFilterList.add(filter);
    }
  }

  public ExternalizedNestedThreadGroupsCategory() {
    super("Thread Groups");
    cloneDefinedFilters();
  }

  public Category getThreads() {
    return threads;
  }

  public void setThreads(Category threads) {
    this.threads = threads;
    for (int i = 0; i < threads.getNodeCount(); i++) {
      ThreadInfo ti = (ThreadInfo) ((DefaultMutableTreeNode) threads.getNodeAt(i)).getUserObject();
      threadLinkedList.add(ti);
    }

    addFilters();

    // Sort the thread groups and nested threads by health
    this.threadGroupList = ThreadGroup.sortByHealth(this.threadGroupList);
  }

  public Collection<ThreadGroup> getThreadGroups() {
    return this.threadGroupList;
  }

  public Collection<ThreadGroup> getWLSThreadGroups() {
    return this.wlsThreadGroupList;
  }

  public Collection<ThreadGroup> getNonWLSThreadGroups() {
    return this.nonWlsThreadGroupList;
  }

  public NestedCategory getWLSThreadsCategory() {
    return nestedWLSCategory;
  }

  public NestedCategory getNonWLSThreadsCategory() {
    return nestedNonWLSCategory;
  }

  private void createNonWLSFilterCategories() {

    nonwlsCompositeFilter = new CompositeFilter("Non-WLS Thread Groups");
    nonwlsCompositeFilter.setInfo("Non-WebLogic Thread Groups");

    // Exclude all wls related threads for it
    nonwlsCompositeFilter.addFilter(allWLSThreadsFilter1, false);
    nonwlsCompositeFilter.addFilter(allWLSThreadsFilter2, false);

    addToFilters(nonwlsCompositeFilter);

    nestedNonWLSCategory = getSubCategory(nonwlsCompositeFilter.getName());

    addUnknownThreadGroupFilter();

    for (Filter filter : allNonWLSFilterList) {
      nestedNonWLSCategory.addToFilters(filter);
    }
  }

  private void addUnknownThreadGroupFilter() {

    unknownCompositeFilter = new CompositeFilter("Unknown or Custom Threads");
    unknownThreadGroup = ThreadGroupFactory.createThreadGroup(unknownCompositeFilter.getName());
    threadGroupList.add(unknownThreadGroup);

    for (Filter filter : allNonWLSFilterList) {
      unknownCompositeFilter.addFilter(filter, false);
    }

    for (Filter filter : allWLSFilterList) {
      unknownCompositeFilter.addFilter(filter, false);
    }

    // Add the unknownCompositeFilter to the allNonWLSFilterList
    allNonWLSFilterList.add(unknownCompositeFilter);
  }

  private void createWLSFilterCategories() {

    wlsCompositeFilter = new CompositeFilter("WLS Thread Groups");
    wlsCompositeFilter.setInfo("WebLogic Thread Groups");

    // Include all wls related threads for it
    wlsCompositeFilter.addFilter(allWLSThreadsFilter1, true);
    wlsCompositeFilter.addFilter(allWLSThreadsFilter2, true);

    addToFilters(wlsCompositeFilter);

    nestedWLSCategory = getSubCategory(wlsCompositeFilter.getName());

    // Create a new filter for captuing just the wls & wls jms threads that dont fall under any known wls thread groups
    CompositeFilter wlsJMSThreadsFilter = new CompositeFilter("WLS JMS Threads");
    wlsJMSThreadsFilter.addFilter(wlsJMSFilter, true);
    nestedWLSCategory.addToFilters(wlsJMSThreadsFilter);

    CompositeFilter wlsThreadsFilter = new CompositeFilter("WLS Threads");
    wlsThreadsFilter.addFilter(allWLSThreadsFilter1, true);
    wlsThreadsFilter.addFilter(allWLSThreadsFilter2, true);
    // Exclude wls jms from pure wls related group
    wlsThreadsFilter.addFilter(wlsJMSFilter, false);
    nestedWLSCategory.addToFilters(wlsThreadsFilter);

    for (Filter filter : allWLSFilterList) {
      nestedWLSCategory.addToFilters(filter);
      wlsThreadsFilter.addFilter(filter, false);
      wlsJMSThreadsFilter.addFilter(filter, false);
    }

    allWLSFilterList.add(wlsJMSThreadsFilter);
    allWLSFilterList.add(wlsThreadsFilter);
  }

  private void addFilters() {

    createWLSFilterCategories();
    createNonWLSFilterCategories();

    // Create references to the Muxer, AQ Adapter and LDAP Filters as they are referred for Exclusion for the nested Filter for Socket Read
    for (Filter filter : allKnownFilterMap.values()) {
      if (filter instanceof CompositeFilter) {
        continue;
      }

      String filterName = filter.getName().toLowerCase();
      if (filterName.contains("muxer")) {
        muxerThreadsFilter = filter;
      } else if (filterName.startsWith("ldap")) {
        ldapThreadsFilter = filter;
      } else if (filterName.contains("aq adapter")) {
        aqAdapterThreadsFilter = filter;
      }
    }

    LinkedList<ThreadInfo> pendingThreadList = new LinkedList<ThreadInfo>(threadLinkedList);
    createThreadGroups(pendingThreadList, allWLSFilterList, true, nestedWLSCategory);
    createThreadGroups(pendingThreadList, allNonWLSFilterList, false, nestedNonWLSCategory);

    // For the rest of the unknown type threads, add them to the unknown group
    for (ThreadInfo ti : pendingThreadList) {
      unknownThreadGroup.addThread(ti);
    }
    createThreadGroupNestedCategories(unknownThreadGroup, unknownCompositeFilter, nestedNonWLSCategory);
  }

  private void createThreadGroups(LinkedList<ThreadInfo> pendingThreadList, ArrayList<Filter> filterList, boolean isWLSThreadGroup, NestedCategory parentCategory) {
    for (Filter filter : filterList) {
      String name = filter.getName();

      // Special processing for Unknown thread group
      // only the remaining threads have to be added to Unknown thread group
      if (name.contains("Unknown")) {
        continue;
      }

      ThreadGroup tg = ThreadGroupFactory.createThreadGroup(name);
      ArrayList<String> excludedAdvisories = filter.getExcludedAdvisories();
      if (excludedAdvisories != null && excludedAdvisories.size() > 0) {
        for(String advisoryId: filter.getExcludedAdvisories()) {

          System.out.println(name + " > Adding exclusion for:" + advisoryId);
          ThreadAdvisory tadv = ThreadAdvisory.lookupThreadAdvisoryByName(advisoryId);
          System.out.println("Found ThreadAdvisory :" + tadv);
          if (tadv != null)
            tg.addToExclusionList(tadv);
        }      
      }

      boolean foundAtleastOneThread = false;
      for (Iterator<ThreadInfo> iterator = pendingThreadList.iterator(); iterator.hasNext();) {
        ThreadInfo ti = iterator.next();
        if (filter.matches(ti)) {
          //System.out.println("Found Match against filter: " + filter.getName() + ", for Thread:" + ti.getName());
          tg.addThread(ti);
          iterator.remove();
          foundAtleastOneThread = true;
        }
      }

      if (foundAtleastOneThread) {
        threadGroupList.add(tg);

        if (isWLSThreadGroup) {
          wlsThreadGroupList.add(tg);
        } else {
          nonWlsThreadGroupList.add(tg);
        }
      }

      createThreadGroupNestedCategories(tg, filter, parentCategory);
    }

  }

  private void createThreadGroupNestedCategories(ThreadGroup tg, Filter associatedFilter, NestedCategory parentCategory) {

    tg.runAdvisory();
    associatedFilter.setInfo(tg.getOverview());


    NestedCategory nestedCategory = parentCategory.getSubCategory(associatedFilter.getName());
    HealthLevelAdvisoryFilter warningFilter = new HealthLevelAdvisoryFilter("Threads at Warning Or Above",
            HealthLevel.WARNING);
    nestedCategory.addToFilters(warningFilter);
    // nestedCategory.addToFilters(blockedFilter);
    // nestedCategory.addToFilters(stuckFilter);
    // nestedCategory.setAsBlockedIcon();

    CompositeFilter readsCompositeFilter = new CompositeFilter("Reading Data From Remote Endpoint");
    readsCompositeFilter.setInfo("The thread is waiting for a remote response or still reading incoming request (via socket or rmi call)");
    Filter waitingOnRemote = new Filter("Reading Data From Remote Endpoint", "(socketRead)|(ResponseImpl.waitForData)",
            2, false, false, true);
    readsCompositeFilter.addFilter(waitingOnRemote, true);
    readsCompositeFilter.addFilter(ldapThreadsFilter, false);
    readsCompositeFilter.addFilter(muxerThreadsFilter, false);
    readsCompositeFilter.addFilter(aqAdapterThreadsFilter, false);
    nestedCategory.addToFilters(readsCompositeFilter);

    ArrayList<HotCallPattern> hotPatterns = tg.getHotPatterns();
    if (hotPatterns.size() > 0) {
      int count = 1;
      ThreadAdvisory hotcallPatternAdvsiory = ThreadAdvisory.getHotPatternAdvisory();
      for (HotCallPattern hotcall : hotPatterns) {
        HotCallPatternFilter fil = new HotCallPatternFilter("Hot Call Pattern - " + count, hotcall.geThreadPattern());
        //System.out.println("\nAdding Hot call pattern for Group:" + tg.getName() + ", and Hot call:" + hotcall.geThreadPattern() + "\n\n");
        String color = hotcallPatternAdvsiory.getHealth().getBackgroundRGBCode();
        StringBuffer sb = new StringBuffer("<p style=\"background-color:" + color + ";\"><font face=System size=-1>");

        sb.append("Associated Advisory:" + hotcallPatternAdvsiory.getPattern()
                + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Keyword: " + hotcallPatternAdvsiory.getKeyword()
                + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Denotes: " + hotcallPatternAdvsiory.getDescrp()
                + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;HealthLevel: " + hotcallPatternAdvsiory.getHealth()
                + "<br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Suggested Advice: " + hotcallPatternAdvsiory.getAdvice() + "<br><br>");
        sb.append("</font></p>");

        fil.setInfo(sb.toString() + "<BR> <pre> Multiple Threads are exhibiting following call execution pattern:\n"
                + hotcall.geThreadPattern() + "</pre>");
        nestedCategory.addToFilters(fil);
        count++;
      }
    }

  }
}
