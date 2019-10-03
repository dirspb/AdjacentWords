package ru.rozhnev.adjacentWords;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.System.arraycopy;

public class TextAnalyzer {
    private static final int AVERAGE_WORD_SIZE = 50;
    private static final int INT_SET_SIZE = 10;
    private static final Logger LOG = LoggerFactory.getLogger(TextAnalyzer.class);

    private final Dict dict;
    private final WordIndex wordIndex;

    public TextAnalyzer(WordReader reader) throws IOException {
        final long initSize = reader.getFileLength() / AVERAGE_WORD_SIZE;
        if (initSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(reader.getFileName() + " file is to big: " + reader.getFileLength() + " bytes");
        }
        LOG.info("Load data: start");
        dict = new Dict((int) initSize);
        wordIndex = new WordIndex((int) initSize);
        load(reader);
        LOG.info("Data load is done. Total " + wordIndex.size() + " words, " + dict.size() + " unique.");
    }

    private TextAnalyzer(Dict dict, WordIndex wordIndex) {
        this.dict = dict;
        this.wordIndex = wordIndex;
    }

    private void load(WordReader reader) throws IOException {
        try {
            String word = reader.readWord();
            while (word != null) {
                addWord(word);
                word = reader.readWord();
            }
        } finally {
            reader.close();
        }
    }

    @Hot
    public List<String> findLongestRepeat() {
        CircularFifoQueue<String> queue = new CircularFifoQueue<>(1);
        RepeatsCounter repeats = new RepeatsCounter();
        for (String word : wordIndex) {
            queue.add(word);
            if (hasRepeats(queue)) {
                repeats.clear();
                repeats.add(queue);
                queue = incSize(queue);
            }
        }
        return repeats.last;
    }

    @Hot
    private boolean hasRepeats(CircularFifoQueue<String> queue) {
        final List<int[]> sets = analyze(queue);
        return MyList.size(sets.get(0)) > 1;
    }

    private CircularFifoQueue<String> incSize(CircularFifoQueue<String> queue) {
        CircularFifoQueue<String> res = new CircularFifoQueue<>(queue.maxSize() + 1);
        res.addAll(queue);
        return res;
    }

    @Hot
    public void addWord(String word) {
        int index = wordIndex.size();
        dict.add(word, index);
        wordIndex.add(word);
    }

    public String analyze(String sentence) {
        return analyze(sentence.split(" "));
    }

    public String analyze(String... wordArray) {
        final List<String> words = Arrays.stream(wordArray)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        final List<int[]> sets = analyze(words);

        return "Words " + words + " are " +
                (MyList.isEmpty(sets.get(0)) ?
                        "not found" :
                        ("found " + MyList.size(sets.get(0)) + " times on positions: " + sets.toString())) +
                "\n  Preceding words: " + evalIndexWords(sets.get(0), -1) +
                "\n  Following words: " + evalIndexWords(sets.get(0), words.size());

    }

    @Hot
    private List<int[]> analyze(Collection<String> words) {
        final List<int[]> sets = dict.getCopy(words);
        int[] indices = new int[words.size()];
        //todo implement
        throw new NotImplementedException("TODO");
    }

    private String evalIndexWords(int[] set, int offset) {
        CountingSet res = new CountingSet();
        for (int index : set) {
            index += offset;
            if (index >= 0 && index < wordIndex.size()) {
                res.add(wordIndex.get(index));
            }
        }
        return res.toString();
    }

    private static FSTConfiguration createFSTConf() {
        final FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        conf.setShareReferences(false);
        conf.registerClass(Dict.class, TIntHashSet.class, String.class);
        return conf;
    }

    public void saveToFile(String fileName) throws IOException {
        LOG.info("Saving to file " + fileName + "...");
        FSTObjectOutput out = new FSTObjectOutput(new FileOutputStream(fileName), createFSTConf());
        out.writeObject(dict);
        final int dictSize = out.getWritten();
        LOG.info("Dict size id " + dictSize);
        out.writeObject(wordIndex);
        LOG.info("WordIndex size id " + (out.getWritten() - dictSize));
        out.close();
    }

    public static TextAnalyzer loadFromFile(String fileName) throws IOException, ClassNotFoundException {
        final FSTObjectInput objInput = new FSTObjectInput(new FileInputStream(fileName), createFSTConf());
        final Dict dict = (Dict) objInput.readObject();
        final WordIndex wordIndex = (WordIndex) objInput.readObject();
        objInput.close();
        return new TextAnalyzer(dict, wordIndex);
    }

    private static class CountingSet extends TObjectIntHashMap<String> {
        public CountingSet() {
        }

        public void add(String word) {
            put(word, get(word) + 1);
        }
    }

    private static class Dict extends THashMap<String, int[]> {

        public Dict() {
        }

        public Dict(int size) {
            super(size);
        }

        @Override
        @Nonnull
        @Hot
        public int[] get(Object key) {
            int[] set = super.get(key);
            if (set == null) {
                set = MyList.create(INT_SET_SIZE);
                put((String) key, set);
            }
            return set;
        }

        public List<int[]> getCopy(Collection<String> words) {
            ArrayList<int[]> sets = new ArrayList<>(words.size());
            for (String word : words) {
                sets.add(ArrayUtils.clone(get(word)));
            }
            return sets;
        }

        @Hot
        public void add(String word, int index) {
            int[] set = get(word);
            if (MyList.isFull(set)) {
                set = MyList.grow(set);
                put(word, set);
            }
            MyList.add(set, index);
        }
    }

    private static class RepeatsCounter {

        private final List<String> last = new ArrayList<>();

        public void clear() {
            last.clear();
        }

        public void add(CircularFifoQueue<String> queue) {
            last.clear();
            last.addAll(queue);
        }
    }

    /**
     * Contains static methods for manipulations with custom ArrayList of int.
     * Such lists are stored in {@code int[]} array with its size stored in {@code int[0]}.
     * Following array elements store actual values.
     */
    private static class MyList {
        public static int[] create(int size) {
            return new int[size];
        }

        public static boolean isEmpty(int[] set) {
            return set[0] == 0;
        }

        public static int size(int[] set) {
            return set[0];
        }

        public static String toString(int[] set) {
            return ArrayUtils.toString(set);
        }

        @Hot
        public static int[] grow(int[] set) {
            final int size = set[0];
            final int[] newSet = new int[size * 2];
            arraycopy(set, 0, newSet, 0, size);
            return newSet;
        }

        @Hot
        public static boolean isFull(int[] set) {
            final int size = set[0];
            return size == set.length;
        }

        @Hot
        public static void add(int[] set, int index) {
            final int size = set[0];
            set[size] = index;
            set[0] = size + 1;
        }
    }
}

