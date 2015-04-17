package Webcrawler;

/* 
 * Author: Lee Carraher
 * Organization: Universite of Cincinnati
 * Date: 4/23/2008
 * License GPL
 * */

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class WebCrawler {

	Map<String, Integer> pageINIndex;//urls and their number of incoming links

	Map<String, Integer> pageOUTIndex;//urls and their number of outgoing links

	SortedSet<String> pageQueue;// URLs to be searched

	ArrayList<CrawlerThread> threads;// array of available CrawlerThreads

	String FILE_TO_SAVE_TO = "/home/lee/Desktop/linksoutput.txt";
	
	RobotSafe robotSafe;

	/**This package creates a multithreaded domain limited web crawl starting at the specified
	 * URLS. 
	 * @param startingURLs list of seed URLS
	 * @param numberOfThreads
	 */
	public WebCrawler(String[] startingURLs, int numberOfThreads) {
		// set default for URL access
		URLConnection.setDefaultAllowUserInteraction(false);
		initCrawl(numberOfThreads, startingURLs);
		crawl();
	}

	/**Initialize the crawls datastructures
	 * @param numberOfThreads
	 * @param startingURLs
	 */
	public void initCrawl(int numberOfThreads, String[] startingURLs) {
		// initialize thread safe search data structures
		this.pageQueue = Collections
				.synchronizedSortedSet(new TreeSet<String>());
		this.pageINIndex = Collections
				.synchronizedMap(new HashMap<String, Integer>(10000));
		this.pageOUTIndex = Collections
				.synchronizedMap(new HashMap<String, Integer>(10000));

		// add the starting urls to the initial queue
		for (String s : startingURLs) {
			try {
				URL u = new URL(s);
				robotSafe = new RobotSafe(u);
				pageQueue.add(u.toExternalForm());
				
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Failed to seed URL : " + s);
			}
		}

		//create a list of crawlerThreads for running the crawl
		threads = new ArrayList<CrawlerThread>(numberOfThreads);
		for (int i = 0; i < numberOfThreads; i++) {
			threads.add(new CrawlerThread(i, this));
		}
	}

	/**Restarts dead threads.
	 */
	public void restartDeadThreads() {
		for (CrawlerThread ct : threads) {
			new Thread(ct).start();
		}
	}

	/** return the number of currently locked threads
	 * @return int deadThreads
	 */
	public int getNumberOfLiveThreads() {
		int i = 0;
		for (CrawlerThread ct : threads) {
			if (ct.locked)
				i++;
		}
		return i;
	}

	/**change this when we use threads
	 * start crawling. You are finished when all threads are not locked
	 * and the pageQueue is empty.
	 */
	public void crawl() {
		restartDeadThreads();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//		it may be a bit paranoid to write every 50 sites
		new Thread(new Runnable() {
			public void run() {
				int previousSize = 0;
				
				
				while (true) 
				{
					//wait a bit for the threads to start
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//avoid concurrent modifications by locking when reading/writing
					synchronized (this) 
					{
						if (pageOUTIndex.size() - previousSize > 50) 
						{
							printUpdate();
							previousSize = pageOUTIndex.size();
						}
					}
				}
			}
		}).start();

	}

	/**Print an updated map to the file specified as FILE_TO_SAVE_TO
	 * this could be specified on the command line, but i didnt feel like
	 * coding the parsing for it.
	 */
	public void printUpdate() {
		System.out.println("printing next update");
		PrintStream p;
		try {
			FileOutputStream out; // declare a file output object
			out = new FileOutputStream(FILE_TO_SAVE_TO);
			p = new PrintStream(out);
		} catch (Exception e) {
			System.out.println("file broked");
			return;
		}

		p.println(mapsToString());
		p.close();
	}

	/**Convert the current site queue to a string. This maybe useful if a crawl job
	 * is ended prematurely. One then could then start a new crawl with this list
	 * as the URL seeds to the main crawler method.
	 * @return
	 */
	synchronized String queueToString() {
		StringBuilder b = new StringBuilder();
		for (String u : pageQueue) {
			b.append(u + '\n');
		}
		return b.toString();
	}

	/**Convert the HashMaps IN links and OUT links to a readable String
	 * @return
	 */
	synchronized String mapsToString() {

		//append in links
		int countIN = 0;
		StringBuilder b = new StringBuilder();
		for (String u : pageINIndex.keySet()) {
			Integer index = pageINIndex.get(u);
			b.append(u + "->" + index.toString() + '\n');
			countIN += index.intValue();
		}

		b.append("\nNumber of Links IN: " + pageINIndex.size()
				+ "\tTotal Index: " + Integer.toString(countIN) + "\n\n\n\n\n");

		//appendout links
		int countOUT = 0;
		for (String u : pageINIndex.keySet()) {
			Integer index = pageINIndex.get(u);
			b.append(u + "->" + index.toString() + '\n');
			countOUT += index.intValue();
		}

		b.append("\nNumber of Links OUT: " + pageOUTIndex.size()
				+ "\tTotal Index: " + Integer.toString(countOUT) + "\n");
		return b.toString();
	}

	/**
	 * @param urls to seed the crawl with. these also server to act as the acceptable hosts
	 */
	public static void main(String[] args) {
		new WebCrawler(args, 10);

	}

}
