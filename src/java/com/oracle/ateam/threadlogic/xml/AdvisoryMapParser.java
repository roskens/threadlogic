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
 * Based on: http://www.java-samples.com/showtutorial.php?tutorialid=152
 */
package com.oracle.ateam.threadlogic.xml;

import com.oracle.ateam.threadlogic.HealthLevel;
import com.oracle.ateam.threadlogic.advisories.ThreadAdvisory;
import java.io.InputStream;
import java.util.ArrayList;


import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 *
 * @author saparam
 */
public class AdvisoryMapParser extends DefaultDomParser{
  
  private static final String xmlElementName = "Advisory";
  private InputStream input;
  private ArrayList<ThreadAdvisory> advisoryList;
  
  public AdvisoryMapParser(InputStream input){
    super();
    this.input = input;
    advisoryList = new ArrayList<ThreadAdvisory>();
	}

	public void run() throws Exception {
		
		//parse the xml file and get the dom object
		parseXmlFile(input);
		
		//get each of the Group elements and create the associated objects
    parseDocument(xmlElementName);
	}
	
	
	// Parse the AdvisoryMap.xml and create ThreadAdvisory from the Advisory Elements
	protected void parseDocument(String xmlElementName ){
    try {
		
      Element docEle = dom.getDocumentElement();		

      NodeList nl = docEle.getElementsByTagName(xmlElementName);
      if(nl != null && nl.getLength() > 0) {
        for(int i = 0 ; i < nl.getLength();i++) {

          //get the group element
          Element el = (Element)nl.item(i);

          ThreadAdvisory tadv = getThreadAdvisory(el);
          advisoryList.add(tadv);
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
	}


	private ThreadAdvisory getThreadAdvisory(Element grpEl) throws Exception {
		
    try {
		String pattern = getTextValue(grpEl,"Name");		
    String descrp = getTextValue(grpEl,"Descrp");
    String advice = getTextValue(grpEl,"Advice");
    String health = getTextValue(grpEl,"Health");
    String keyword = getTextValue(grpEl,"Keyword");
		
    String[] list = new String[1];
    
    if (keyword.contains(",")) {
      String[] keyTokens = keyword.split(",");
      
      keyword = keyTokens[0];
      list = new String[keyTokens.length];
      
      for (int i = 0; i < keyTokens.length;i++) {
        list[i] = keyTokens[i].trim();
      }      
    } else {
      list[0] = keyword;
    }
    
    //Create a new ThreadAdvisory with the value read from the xml nodes
		ThreadAdvisory tadv = new ThreadAdvisory(keyword, HealthLevel.valueOf(health), pattern, descrp, advice);
    
    if (list!= null && list.length > 1)
      tadv.setKeywordList(list);
    
    System.out.println("Parsed: " + tadv);
    
		return tadv;
    } catch (Exception e) { System.out.println("Error while creating ThreadAdvisory: " + e.getMessage());
      throw e;
    }
	}
  
  /**
   * @return the advisoryList
   */
  public ArrayList<ThreadAdvisory> getAdvisoryList() {
    return advisoryList;
  }
	
}

