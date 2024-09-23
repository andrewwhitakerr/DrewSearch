package prog11;

import java.util.*;

/** The meat of the project, containing the search algorithm -- my part of the assignment */
public class NotGPT implements SearchEngine {

    /* Create and initialize variables that reference
     * new instances of needed data structures.
     */
    HardDisk pageDisk = new HardDisk();
    HardDisk wordDisk = new HardDisk();
    Map<String, Long> wordToIndex = new HashMap<>();
    Map<String, Long> urlToIndex = new TreeMap<>();

    /**
     * Indexes a web page URL into the search engine's data structures.
     * <p>
     * This method creates a new entry for the given URL in the `pageDisk` storage and updates
     * the `urlToIndex` map with the URL and its corresponding index. It effectively registers the
     * page within the search engine, allowing it to be associated with words and retrieved during
     * search queries.
     *
     * @param url The URL of the web page to be indexed.
     * @return The unique index assigned to the page.
     */
    public Long indexPage(String url) {
        Long index = pageDisk.newFile();
        InfoFile file = new InfoFile(url);
        pageDisk.put(index, file);
        urlToIndex.put(url, index);
        System.out.println("indexing page " + index + " " + file);
        return index;
    }

    /**
     * Indexes a word into the search engine's data structures.
     * <p>
     * This method creates a new entry for the given word in the `wordDisk` storage and updates
     * the `wordToIndex` map with the word and its corresponding index. This allows the word to be
     * associated with pages and included in search operations.
     *
     * @param word The word to be indexed.
     * @return The unique index assigned to the word.
     */
    public Long indexWord(String word) {
        Long index = wordDisk.newFile();
        InfoFile file = new InfoFile(word);
        wordDisk.put(index, file);
        wordToIndex.put(word, index);
        System.out.println("indexing word " + index + " " + file);
        return index;
    }

    /**
     * Searches for web pages containing all specified search words and returns them in order of importance (as
     * determined by the rank method within this class).
     * <p>
     * This method identifies pages that contain all the specified search words by intersecting their page indices.
     * It then uses the precomputed importance values (calculated by the `rank` methods) to order these pages.
     * The pages are added to a priority queue that maintains them in order based on their importance.
     * Finally, it returns up to `numResults` URLs of these pages in decreasing order of importance.
     *
     * @param searchWords A list of words to search for across indexed pages.
     * @param numResults  The maximum number of search results to return.
     * @return An array of URLs of pages that contain all the specified search words, ordered by precomputed importance.
     */
    public String[] search(List<String> searchWords, int numResults) {
        // Create an iterator over the search words
        Iterator<String> wordIterator = searchWords.iterator();
        // Remove any words from searchWords that are not in the index
        while (wordIterator.hasNext()) {
            String word = wordIterator.next();
            if (wordToIndex.get(word) == null)
                wordIterator.remove();
        }
        // If no valid search words remain, return an empty array
        if (searchWords.isEmpty())
            return new String[0];

        // Initialize an array of iterators for page indices of each search word
        Iterator<Long>[] wordPageIndexIterators = (Iterator<Long>[]) new Iterator[searchWords.size()];
        // Array to hold the current page index from each iterator
        long[] currentPageIndex = new long[searchWords.size()];
        // Comparator to sort pages based on their importance (influence)
        PageComparator pageComparator = new PageComparator();
        // Priority queue to store the best page indices in order of decreasing importance
        PriorityQueue<Long> bestPageIndices = new PriorityQueue<>(pageComparator);

        // For each search word, get its associated page indices and initialize iterators
        for (int i = 0; i < searchWords.size(); i++) {
            String word = searchWords.get(i);
            List<Long> pageIndices = null;
            // Get the index of the word from the wordToIndex map
            Long index = wordToIndex.get(word);

            if (index != null) {
                // Retrieve the InfoFile containing the page indices for this word
                InfoFile file = wordDisk.get(index);
                // Copy the page indices to a new list
                pageIndices = new ArrayList<>(file.indices);
            }
            // Initialize the iterator for this word's page indices
            wordPageIndexIterators[i] = pageIndices.iterator();
        }

        // Loop until there are no more page indices to process
        List<String> results = new ArrayList<>();
        while (getNextPageIndices(currentPageIndex, wordPageIndexIterators)) {
            // Check if all current page indices are equal (all words are on the same page)
            if (allEqual(currentPageIndex)) {
                String url = "";
                // Find the URL associated with the current page index
                for (Map.Entry<String, Long> entry : urlToIndex.entrySet()) {
                    if (entry.getValue().equals(currentPageIndex[0])) {
                        url = entry.getKey();
                        break;
                    }
                }
                // If a valid URL is found, add the URL to the results list
                if (!url.isEmpty()) {
                    System.out.println("Found a match! URL: " + url);
                    results.add(url);
                    /* If we haven't reached the desired number of results,
                     * add the current page index to the priority queue */
                    if (bestPageIndices.size() != numResults) {
                        bestPageIndices.offer(currentPageIndex[0]);
                    } else {
                        // Compare the importance of the current page with the least important in the queue
                        if (pageComparator.compare(currentPageIndex[0], bestPageIndices.peek()) > 0) {
                            // Replace the least important page index with the current one
                            bestPageIndices.poll();
                            bestPageIndices.offer(currentPageIndex[0]);
                        }
                    }
                }
            }

        }
        /* Create the array of results by iteratively polling page data
           from the bestPageIndices queue */
        String[] theResults = new String[bestPageIndices.size()];
        for (int i = bestPageIndices.size() - 1; i >= 0; i--) {
            InfoFile page = pageDisk.get(bestPageIndices.poll());
            theResults[i] = page.data;
        }

        return theResults;

    }

    /**
     * Check if all elements in an array of long are equal.
     *
     * @param array an array of numbers
     * @return true if all are equal, false otherwise
     */
    private boolean allEqual(long[] array) {
        if (array == null || array.length <= 1) return true;
        long first = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] != first)
                return false;
        }
        return true;
    }

    /**
     * Get the largest element of an array of long.
     *
     * @param array an array of numbers
     * @return largest element
     */
    private long getLargest(long[] array) {
        if (array == null)
            throw new IllegalArgumentException("Array cannot be null");
        else if (array.length == 0)
            throw new IllegalArgumentException("Array cannot be empty");
        long currentLargest = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > currentLargest)
                currentLargest = array[i];
        }
        return currentLargest;
    }

    /**
     * If all the elements of currentPageIndex are equal,
     * set each one to the next() of its Iterator,
     * but if any Iterator hasNext() is false, just return false.
     *
     * Otherwise, do that for every element not equal to the largest element.
     *
     * Return true.
     *
     * @param currentPageIndex       array of current page indices
     * @param wordPageIndexIterators array of iterators with next page indices
     * @return true if all page indices are updated, false otherwise
     */
    private boolean getNextPageIndices(long[] currentPageIndex, Iterator<Long>[] wordPageIndexIterators) {
        // If all current page indices are equal, advance all iterators to the next page index
        if (allEqual(currentPageIndex)) {
            for (int i = 0; i < currentPageIndex.length; i++) {
                // If the iterator has a next element, update currentPageIndex[i]
                if (wordPageIndexIterators[i].hasNext()) {
                    currentPageIndex[i] = wordPageIndexIterators[i].next();
                } else {
                    // If any iterator is exhausted, return false to indicate no more common pages
                    return false;
                }
            }
            return true;
        } else {
            // Find the largest page index among currentPageIndex
            long largest = getLargest(currentPageIndex);
            for (int i = 0; i < currentPageIndex.length; i++) {
                // If currentPageIndex[i] is less than the largest, advance its iterator
                if (currentPageIndex[i] != largest && wordPageIndexIterators[i].hasNext()) {
                    currentPageIndex[i] = wordPageIndexIterators[i].next();
                } else if (currentPageIndex[i] != largest) {
                    // If any iterator cannot be advanced further, return false
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Collects web pages starting from the specified URLs and builds the index.
     *
     * <p>This method performs a web crawl starting from the `startingURLs`, indexing pages,
     * extracting words and links, and building the data structures needed for searching and ranking.
     * It updates both the `pageDisk` and `wordDisk` with the collected information.</p>
     *
     * @param browser      A `Browser` instance used to load and parse web pages.
     * @param startingURLs A list of starting URLs to begin the crawl.
     */
    public void collect(Browser browser, List<String> startingURLs) {
        System.out.println("starting pages " + startingURLs);
        // Queue to hold indices of pages to process
        Queue<Long> indices = new ArrayDeque<>();
        // Index the starting URLs if they are not already indexed
        for (String url : startingURLs) {
            if (!urlToIndex.containsKey(url)) {
                indices.add(indexPage(url));
            }
        }
        // Process pages in the queue
        while (!indices.isEmpty()) {
            System.out.println("queue " + indices);
            // Dequeue the next page index
            Long pageIndex = indices.poll();
            // Retrieve the page's InfoFile from pageDisk
            InfoFile file = pageDisk.get(pageIndex);
            System.out.println("dequeued " + file);
            // Load the page content using the browser
            if (browser.loadPage(file.data)) {
                // Sets to keep track of seen URLs and words to avoid duplicates
                Set<String> seenURLs = new TreeSet<>();
                Set<String> seenWords = new TreeSet<>();
                System.out.println("urls " + browser.getURLs());
                // Process all URLs found on the page
                for (String url : browser.getURLs()) {
                    if (!seenURLs.contains(url)) {
                        seenURLs.add(url);
                        // Get or index the URL
                        Long index = urlToIndex.get(url);
                        if (index == null) {
                            index = indexPage(url);
                            // Add the new page index to the queue for processing
                            indices.offer(index);
                        }
                        // Add the index to the current page's list of indices (outgoing links)
                        file.indices.add(index);
                    }
                }
                // Update the pageDisk with the new InfoFile
                pageDisk.put(pageIndex, file);
                System.out.println("updated page file " + file);
                System.out.println("words " + browser.getWords());
                // Process all words found on the page
                for (String word : browser.getWords()) {
                    if (!seenWords.contains(word)) {
                        seenWords.add(word);
                        // Get or index the word
                        Long wordIndex = wordToIndex.get(word);
                        if (wordIndex == null) {
                            wordIndex = indexWord(word);
                        }
                        // Retrieve the word's InfoFile from wordDisk
                        InfoFile wordFile = wordDisk.get(wordIndex);
                        // Add the current page index to the word's list of indices (pages containing the word)
                        wordFile.indices.add(pageIndex);
                        System.out.println("updated word file " + wordFile);
                    }
                }
            }
        }
    }

    /**
     * Computes the importance (influence) of each page using an iterative algorithm.
     *
     * <p>This method initializes the influence scores of all pages and then performs multiple
     * iterations to propagate influence scores across the network of pages. It can use either
     * a fast or slow implementation based on the `fast` parameter.</p>
     *
     * @param fast If `true`, uses the faster ranking algorithm; otherwise, uses the slower one.
     */
    public void rank(boolean fast) {
        int count = 0;
        // Initialize influence scores for all pages
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            InfoFile file = entry.getValue();
            // Count pages with no outgoing links
            if (file.indices.isEmpty())
                count++;
            file.influence = 1.0;
            file.influenceTemp = 0.0;
        }
        // Compute the default influence to distribute to pages with no incoming links
        double defaultInfluence = 1.0 * count / pageDisk.size();
        // Perform ranking iterations
        if (fast) {
            // Use the faster ranking algorithm
            for (int i = 0; i < 20; i++) {
                rankFast(defaultInfluence);
            }
        } else {
            // Use the slower ranking algorithm
            for (int i = 0; i < 20; i++) {
                rankSlow(defaultInfluence);
            }
        }
    }

    /**
     * Performs a slower version of the ranking algorithm to compute page influences.
     *
     * <p>This method distributes the influence of each page evenly among its outgoing links,
     * accumulates the influence contributions in a temporary variable, and then updates the
     * influence scores. It repeats this process for a specified number of iterations.</p>
     *
     * @param defaultInfluence The influence to assign to pages with no incoming links.
     */
    void rankSlow(double defaultInfluence) {
        // First pass: distribute influence to linked pages
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            InfoFile file = entry.getValue();
            // Distribute influence evenly among outgoing links
            double influencePerIndex = file.influence / file.indices.size();
            for (long i : file.indices) {
                // Accumulate influence in influenceTemp
                pageDisk.get(i).influenceTemp += influencePerIndex;
            }
        }
        // Second pass: update influence scores and reset influenceTemp
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            long index = entry.getKey();
            InfoFile file = entry.getValue();
            // Update influence with accumulated value plus default influence
            file.influence = file.influenceTemp + defaultInfluence;
            // Reset influenceTemp for the next iteration
            file.influenceTemp = 0.0;
        }
    }

    /**
     * Performs a faster version of the ranking algorithm using sorted votes.
     *
     * <p>This method collects all influence contributions (votes) into a list, sorts them,
     * and then efficiently updates the influence scores of pages by iterating over the sorted
     * votes and pages simultaneously. This reduces the number of lookups and improves performance.</p>
     *
     * @param defaultInfluence The influence to assign to pages with no incoming links.
     */
    void rankFast(double defaultInfluence) {
        // List to hold votes (influence contributions) to pages
        List<Vote> votes = new ArrayList<>();
        // First pass: collect votes from all pages
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            InfoFile file = entry.getValue();
            // Distribute influence evenly among outgoing links
            double influencePerIndex = file.influence / file.indices.size();
            for (long i : file.indices) {
                // Create a Vote object representing influence contribution to page i
                votes.add(new Vote(i, influencePerIndex));
            }
        }
        // Sort the votes by index to optimize the update process
        Collections.sort(votes);
        // Iterator over the sorted votes
        Iterator<Vote> iterator = votes.iterator();
        Vote vote = iterator.hasNext() ? iterator.next() : null;
        // Second pass: update influence scores using sorted votes
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            long index = entry.getKey();
            InfoFile file = entry.getValue();
            // Start with the default influence
            file.influence = defaultInfluence;
            // Add influence from votes with matching index
            while (vote != null && vote.index <= index) {
                if (vote.index == index) {
                    file.influence += vote.vote;
                }
                if (iterator.hasNext())
                    vote = iterator.next();
                else
                    vote = null;
            }
        }
    }

    /**
     * Comparator for comparing pages based on their influence scores.
     *
     * <p>This comparator is used in a `PriorityQueue` to order pages by their importance.
     * Pages with higher influence scores are considered greater.</p>
     */
    class PageComparator implements Comparator<Long> {

        @Override
        public int compare(Long pageIndex1, Long pageIndex2) {
            // Compare pages based on their influence scores
            return Double.compare(pageDisk.get(pageIndex1).influence, pageDisk.get(pageIndex2).influence);
        }
    }

}

/**
 * Represents an influence contribution (vote) to a page.
 *
 * <p>This class holds the index of the page receiving the vote and the amount of influence
 * contributed. It implements `Comparable` to allow sorting by page index, which optimizes
 * the influence update process in the `rankFast` method.</p>
 */
class Vote implements Comparable<Vote> {

    long index;
    double vote;

    public Vote (long index, double vote) {
        this.index = index;
        this.vote = vote;
    }

    public int compareTo(Vote o) {
        if (index != o.index)
            return Long.compare(index, o.index);
        else
            return Double.compare(vote, o.vote);
    }

}

