package prog11;

import java.util.*;

public class NotGPT implements SearchEngine {

    HardDisk pageDisk = new HardDisk();
    HardDisk wordDisk = new HardDisk();
    Map<String,Long> wordToIndex = new HashMap<>();
    Map<String,Long> urlToIndex = new TreeMap<>();

    public Long indexPage (String url) {
        Long index = pageDisk.newFile();
        InfoFile file = new InfoFile(url);
        pageDisk.put(index, file);
        urlToIndex.put(url, index);
        System.out.println("indexing page " + index + " " + file);
        return index;
    }

    public Long indexWord (String word) {
        Long index = wordDisk.newFile();
        InfoFile file = new InfoFile(word);
        wordDisk.put(index, file);
        wordToIndex.put(word, index);
        System.out.println("indexing word " + index + " " + file);
        return index;
    }

    public String[] search (List<String> searchWords, int numResults) {
        Iterator<String> wordIterator = searchWords.iterator();
        while (wordIterator.hasNext()) {
            String word = wordIterator.next();
            if (wordToIndex.get(word) == null)
                wordIterator.remove();
        }
        if (searchWords.isEmpty())
            return new String[0];
        Iterator<Long>[] wordPageIndexIterators = (Iterator<Long>[]) new Iterator[searchWords.size()];
        long[] currentPageIndex = new long[searchWords.size()];
        PageComparator pageComparator = new PageComparator();
        PriorityQueue<Long> bestPageIndices = new PriorityQueue<>(pageComparator);
        for (int i = 0; i < searchWords.size(); i++) {
            String word = searchWords.get(i);
            List<Long> pageIndices = null;
            Long index = wordToIndex.get(word);
            if (index != null) {
                InfoFile file = wordDisk.get(index);
                pageIndices = new ArrayList<>(file.indices);
            }
            wordPageIndexIterators[i] = pageIndices.iterator();
        }
        List<String> results = new ArrayList<>();
        while (getNextPageIndices(currentPageIndex, wordPageIndexIterators)) {
            if (allEqual(currentPageIndex)) {
                String url = "";
                for (Map.Entry<String, Long> entry : urlToIndex.entrySet()) {
                    if (entry.getValue().equals(currentPageIndex[0])) {
                        url = entry.getKey();
                        break;
                    }
                }
                if (!url.isEmpty()) {
                    System.out.println("Found a match! URL: " + url);
                    results.add(url);
                    if (bestPageIndices.size() != numResults) {
                        bestPageIndices.offer(currentPageIndex[0]);
                    } else {
                        if (pageComparator.compare(currentPageIndex[0], bestPageIndices.peek()) > 0) {
                            bestPageIndices.poll();
                            bestPageIndices.offer(currentPageIndex[0]);
                        }
                    }
                }
            }

        }

        String[] theResults = new String[bestPageIndices.size()];
        for (int i = bestPageIndices.size() - 1; i >= 0; i--) {
            InfoFile page = pageDisk.get(bestPageIndices.poll());
            theResults[i] = page.data;
        }

        return theResults;

    }
    /** Check if all elements in an array of long are equal.
     @param array an array of numbers
     @return true if all are equal, false otherwise
     */
    private boolean allEqual (long[] array) {
        if (array == null || array.length <= 1) return true;
        long first = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] != first)
                return false;
        }
        return true;
    }

    /** Get the largest element of an array of long.
     @param array an array of numbers
     @return largest element
     */
    private long getLargest (long[] array) {
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

    /** If all the elements of currentPageIndex are equal,
     set each one to the next() of its Iterator,
     but if any Iterator hasNext() is false, just return false.

     Otherwise, do that for every element not equal to the largest element.

     Return true.

     @param currentPageIndex array of current page indices
     @param wordPageIndexIterators array of iterators with next page indices
     @return true if all page indices are updated, false otherwise
     */
    private boolean getNextPageIndices (long[] currentPageIndex, Iterator<Long>[] wordPageIndexIterators) {
        if (allEqual(currentPageIndex)) {
            for (int i = 0; i < currentPageIndex.length; i++) {
                if (wordPageIndexIterators[i].hasNext()) {
                    currentPageIndex[i] = wordPageIndexIterators[i].next();
                } else {
                    return false;
                }
            }
            return true;
        } else {
            long largest = getLargest(currentPageIndex);
            for (int i = 0; i < currentPageIndex.length; i++) {
                if (currentPageIndex[i] != largest && wordPageIndexIterators[i].hasNext()) {
                    currentPageIndex[i] = wordPageIndexIterators[i].next();
                } else if (currentPageIndex[i] != largest) {
                    return false;
                }
            }
            return true;
        }
    }

    public void collect (Browser browser, List<String> startingURLS) {
        System.out.println("starting pages " + startingURLS);
        Queue<Long> indices = new ArrayDeque<>();
        for (String url : startingURLS) {
            if (!urlToIndex.containsKey(url)) {
                indices.add(indexPage(url));
            }
        }
        while (!indices.isEmpty()) {
            System.out.println("queue " + indices);
            Long pageIndex = indices.poll();
            InfoFile file = pageDisk.get(pageIndex);
            System.out.println("dequeued " + file);
            if (browser.loadPage(file.data)) {
                Set<String> seenURLs = new TreeSet<>();
                Set<String> seenWords = new TreeSet<>();
                System.out.println("urls " + browser.getURLs());
                for (String url : browser.getURLs()) {
                    if (!seenURLs.contains(url)) {
                        seenURLs.add(url);
                        Long index = urlToIndex.get(url);
                        if (index == null) {
                            index = indexPage(url);
                            indices.offer(index);
                        }
                        file.indices.add(index);
                    }
                }
                pageDisk.put(pageIndex, file);
                System.out.println("updated page file " + file);
                System.out.println("words " + browser.getWords());
                for (String word : browser.getWords()) {
                    Long wordIndex = null;
                    if (!seenWords.contains(word)) {
                        seenWords.add(word);
                        wordIndex = wordToIndex.get(word);
                        if (wordIndex == null) {
                            wordIndex = indexWord(word);
                        }
                        InfoFile wordFile = wordDisk.get(wordIndex);
                        wordFile.indices.add(pageIndex);
                        System.out.println("updated word file " + wordFile);
                    }
                }
            }
        }
    }

    public void rank (boolean fast) {
        int count = 0;
        for (Map.Entry<Long,InfoFile> entry : pageDisk.entrySet()) {
            InfoFile file = entry.getValue();
            if (file.indices.isEmpty())
                count++;
            file.influence = 1.0;
            file.influenceTemp = 0.0;
        }
        double defaultInfluence = 1.0 * count / pageDisk.size();
        if (fast) {
            for (int i = 0; i < 20; i++) {
                rankFast(defaultInfluence);
            }
        } else {
            for (int i = 0; i < 20; i++) {
                rankSlow(defaultInfluence);
            }
        }
    }

    void rankSlow (double defaultInfluence) {
        for (Map.Entry<Long,InfoFile> entry : pageDisk.entrySet()) {
            InfoFile file = entry.getValue();
            double influencePerIndex = file.influence / file.indices.size();
            for (long i: file.indices) {
                pageDisk.get(i).influenceTemp += influencePerIndex;
            }
        }
        for (Map.Entry<Long,InfoFile> entry : pageDisk.entrySet()) {
            long index = entry.getKey();
            InfoFile file = entry.getValue();
            file.influence = file.influenceTemp + defaultInfluence;
            file.influenceTemp = 0.0;
        }
    }

    void rankFast (double defaultInfluence) {
        List<Vote> votes = new ArrayList<>();
        for (Map.Entry<Long,InfoFile> entry : pageDisk.entrySet()) {
            InfoFile file = entry.getValue();
            double influencePerIndex = file.influence / file.indices.size();
            for (long i : file.indices) {
                votes.add(new Vote(i, influencePerIndex));
            }
        }
        Collections.sort(votes);
        Iterator<Vote> iterator = votes.iterator();
        Vote vote = iterator.next();
        for (Map.Entry<Long, InfoFile> entry : pageDisk.entrySet()) {
            long index = entry.getKey();
            InfoFile file = entry.getValue();
            file.influence = defaultInfluence;
            while (vote != null && vote.index <= index) {
                file.influence += vote.vote;
                if (iterator.hasNext())
                    vote = iterator.next();
                else
                    vote = null;
            }
        }
    }

    class PageComparator implements Comparator<Long> {

        @Override
        public int compare(Long pageIndex1, Long pageIndex2) {
            return Double.compare(pageDisk.get(pageIndex1).influence, pageDisk.get(pageIndex2).influence);
        }
    }

}

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

