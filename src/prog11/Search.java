package prog11;

import prog02.GUI;
import java.util.*;

import javax.xml.stream.events.StartDocument;
public class Search {
	public static void main(String[] args) {
		// Specify the names of the disk files to read for pages and words
		// You can switch between different datasets by changing these filenames
		// String pageDiskName = "pagedisk-mary-ranked.txt";
		// String wordDiskName = "worddisk-mary.txt";
		String pageDiskName = "pagedisk-1-ranked.txt";
		String wordDiskName = "worddisk-1.txt";

		// Create a Browser instance (used for loading and parsing web pages)
		Browser browser = new BetterBrowser();
		// Create a SearchEngine instance (NotGPT implements SearchEngine interface)
		SearchEngine notGPT = new NotGPT();

		NotGPT g = (NotGPT) notGPT;

		// Read the page data from the specified page disk file
		g.pageDisk.read(pageDiskName);
		// Populate the urlToIndex map using data from pageDisk
		for (Map.Entry<Long, InfoFile> entry : g.pageDisk.entrySet())
			g.urlToIndex.put(entry.getValue().data, entry.getKey());

		// Read the word data from the specified word disk file
		g.wordDisk.read(wordDiskName);
		// Populate the wordToIndex map using data from wordDisk
		for (Map.Entry<Long, InfoFile> entry : g.wordDisk.entrySet())
			g.wordToIndex.put(entry.getValue().data, entry.getKey());

		// Print out the mappings and disk contents for debugging purposes
		System.out.println("map from URL to page index");
		System.out.println(g.urlToIndex);
		System.out.println("map from page index to page disk");
		System.out.println(g.pageDisk);
		System.out.println("map from word to word index");
		System.out.println(g.wordToIndex);
		System.out.println("map from word index to word file");
		System.out.println(g.wordDisk);

		// Initialize a list to hold the search keywords
		List<String> keyWords = new ArrayList<String>();

		// Conditional block to choose between test keywords and user input
		if (false) {
			// If set to true, use hardcoded test keywords
			keyWords.add("mary");
			keyWords.add("jack");
			keyWords.add("jill");
		} else {
			// Else, create a GUI for user to input search words
			GUI gui = new GUI("NotGPT");
			while (true) {
				// Prompt the user to enter search words via the GUI
				String input = gui.getInfo("Enter search words.");
				// If the user cancels or closes the dialog, exit the program
				if (input == null)
					return;
				// Split the input string into individual words based on whitespace
				String[] words = input.split("\\s+");
				// Clear any existing keywords from previous searches
				keyWords.clear();
				// Add each word to the keywords list
				for (String word : words)
					keyWords.add(word);
				// Perform the search with the specified keywords, requesting up to 5 results
				String[] urls = notGPT.search(keyWords, 5);
				// Build a result string to display to the user
				String res = "Found " + keyWords + " on";
				for (int i = 0; i < urls.length; i++)
					res = res + "\n" + BetterBrowser.inverseReversePathURL(urls[i]);
				// Display the search results in the GUI
				gui.sendMessage(res);
			}
		}

		// This part of the code will only execute if the 'if (false)' condition above is changed to 'if (true)'
		// Perform the search with the test keywords, requesting up to 5 results
		String[] urls = notGPT.search(keyWords, 5);

		// Print out the search results to the console
		System.out.println("Found " + keyWords + " on");
		for (int i = 0; i < urls.length; i++)
			System.out.println(BetterBrowser.inverseReversePathURL(urls[i]));
	}
}

