package Webcrawler;

/* 
 * Author: Lee Carraher
 * Organization: Universite of Cincinnati
 * Date: 4/23/2008
 * License GPL
 * */

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** This class contains the Crawler, which includes parser and methods for updating the site analysis
 * metrics.
 * @author lee
 *
 */
public class CrawlerThread implements Runnable {

	static Pattern htmlPattern = Pattern.compile("<a href=\"");//simple reference pattern, its regex, change if needed
	public boolean locked;// thread state
	Map<String, Integer> pageINIndex;
	Map<String, Integer> pageOUTIndex;
	SortedSet<String> pageQueue;
	WebCrawler parent;//kind of a dirty back reference, but its too big of a pain to fix
	int name;
	RobotSafe rs;
	
	
	
	
	/**
	 * @param pageINIndex
	 * @param pageOUTIndex
	 * @param pageQueue
	 * @param parent
	 */
	public CrawlerThread(int i, WebCrawler parent)
	{
		try{
		this.parent = parent;
		this.pageINIndex = parent.pageINIndex;
		this.pageOUTIndex = parent.pageOUTIndex;
		this.pageQueue = parent.pageQueue;
		name = i;
		this.rs = parent.robotSafe;
		}catch(Exception e){}
		
		locked=false;
	}

	
	
	/**look for hrefs and get the following strings
	 * @param lowerCaseContent
	 * @return a list of URLs
	 */
	public ArrayList<URL> parsePage(String lowerCaseContent, String host) {

		ArrayList<URL> links = new ArrayList<URL>(10);
		Matcher m = Pattern.compile("<a href=\"").matcher(lowerCaseContent);
		
		while (m.find()) {
			int b = m.end();
			int e = lowerCaseContent.indexOf("\"", b + 1);
			
			try
			{
				URL link = new URL(lowerCaseContent.substring(b, e));
				if(link.getProtocol().matches(".*https?") && link.getHost().endsWith(host))
				{
					//if(!link.getFile().matches(".*\\.pdf")&&!link.getFile().matches(".*\\.aspx?")&&!link.getFile().matches(".*\\.cgi"))
						links.add(link);
				}
			}
			catch(Exception ec){}//if the string fails to resolve to a url then its not a url, dont add it, lets not get picky
		}
		return links;
	}



	
	
	
	
	/**Get the source text from an html page
	 * @param url
	 * @return lowercase source
	 */
	String getPageText(URL url) 
	{
		String lcContent = "";
		try {
			BufferedReader urlStream = new BufferedReader(
					new InputStreamReader(url.openStream()));
			StringBuilder content = new StringBuilder();
			int readbyte;
			while ((readbyte = urlStream.read()) != -1) {
				content.append((char) readbyte);
			}
			urlStream.close();
			lcContent = content.toString().toLowerCase();
		} catch (Exception e) {
		}
		return lcContent;
	}

	
	
	
	
	
	
	/**Find the ending part of a URL's Host
	 * ie "www.ece.uc.edu" truncates to
	 * "uc.edu"
	 * this is used to restrict the search to a particular domain
	 * @param url
	 * @return
	 */
	public static String getHostFromURL(URL url)
	{
		String[] splitHost = url.getHost().split("\\.");
		int l = splitHost.length;
		String host = splitHost[l-2]+"."+splitHost[l-1];
		return host;
	}
	
	
	
	
	
	
	/**
	 * This is the primary method, it downloads the url then parses it for links
	 * the links are then added to the queue, outgoings are added to the outgoing index
	 * and incoming are added to an incomin index
	 */
	public  void addParse(String strURL)
	{
		URL url;
		try {
			url = new URL(strURL);
			ArrayList<URL> urls = parsePage(getPageText(url), getHostFromURL(url));
			
			
			//make sure multiple threads are not editing the data concurrently
			synchronized (parent) 
			{
				pageOUTIndex.put(url.toExternalForm(), urls.size());
				for(URL u:urls)
				{
					if(pageINIndex.containsKey(u.toExternalForm()) )
					{
						pageINIndex.put(u.toExternalForm(), pageINIndex.remove(u.toExternalForm())+1);
					}
					else
					{
						if(!pageOUTIndex.containsKey(u.toExternalForm()) && rs.safe(u.toExternalForm())){
							pageQueue.add(u.toExternalForm());
						}
							
						
						pageINIndex.put(u.toExternalForm(), 1);
					}
				}
				
//				//just check the OUT index for already parsed pages
//				//a page will be specified in the IN index prior to being parsed
//				//therefor the test should only include the OUT index
//				if(!pageOUTIndex.containsKey(url.toExternalForm()))
//				{
//					
//					pageOUTIndex.put(url.toExternalForm(), urls.size());
//					//iterate through urls and add them to the queue, or map
//					for(URL u:urls)
//					{
//						if(!pageINIndex.containsKey(u) )
//						{
//							//do not add links to self otherwise we get repeats in the queue
//							if(!pageQueue.contains(u.toExternalForm()) &&!pageOUTIndex.containsKey(u.toExternalForm()) )
//							{
//									pageQueue.add(u.toExternalForm());
//									pageINIndex.put(u.toExternalForm(), 1);
//							}
//						}
//						else
//						{
//							System.out.println(u.toExternalForm() + "" + pageINIndex.get(u));
//							pageINIndex.put(u.toExternalForm(), pageINIndex.remove(u)+1);//increment the count for this rediscovered link
//						}
//					}
//				}
			}
			
			
		} catch (MalformedURLException e) {//if the url is malformed catch it and continue
		}
	}
	
	
	/* 
	 * Main run method to parse pages from the queue
	 */
	public void run() 
	{
		locked = true;
		//parse pages until there aren't any left
		while(!pageQueue.isEmpty())
		{
			String url="";
			synchronized (parent) 
			{
				if(!pageQueue.isEmpty()){
					url = pageQueue.first();
					pageQueue.remove(url);
				}
			}

			
			addParse(url);
			
			//System.out.println(pageOUTIndex.size());
			//keep as many threads busy as possible
			parent.restartDeadThreads();
			

		}
		locked = false;
	}


}
