package Webcrawler;

/* 
 * Author: Lee Carraher
 * Organization: Universite of Cincinnati
 * Date: 4/23/2008
 * License GPL
 * */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class RobotSafe {
	public static final String DISALLOW = "Disallow:";
	public static Pattern p = Pattern.compile(DISALLOW);
	
	
		ArrayList<String> robotForbiddenURLs;
		
		public RobotSafe(URL hosts)
		{

				robotForbiddenURLs= robotSafe(hosts);
		}
	
		ArrayList<String> robotSafe(URL url) {		
		
			ArrayList<String> disallows = new ArrayList<String>();
			try {
			
			String strHost = url.getHost();

			// form URL of the robots.txt file
			String strRobot = "http://" + strHost + "/robots.txt";
			URL urlRobot = new URL(strRobot);
		    InputStream urlRobotStream = urlRobot.openStream();

		    StringBuilder robotsData;
		    int numRead = urlRobotStream.read();
		    robotsData = new StringBuilder();
		    
		    while ((numRead=urlRobotStream.read()) != -1) 
		    {
		    	robotsData.append((char)numRead);
			
		    }
		    String searchable = robotsData.toString();
		    urlRobotStream.close();
		    Matcher m = p.matcher(searchable);
		    
		    while(m.find())
		    {
		    	int start = m.end();
		    	int end = searchable.indexOf("\n", start);
		    	robotForbiddenURLs.add(searchable.substring(start,end));
		    }
		    
		    
		    
		} catch (Exception e) 
		{
		    
		}
		return disallows;
	 }
		
		public boolean safe(String url)
		{
			for(String forbid:robotForbiddenURLs)
				if(url.contains(forbid))return false;
				
			return true;
		}
		
}
