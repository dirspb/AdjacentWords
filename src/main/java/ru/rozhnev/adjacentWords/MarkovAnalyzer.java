package ru.rozhnev.adjacentWords;

import com.sun.deploy.net.MessageHeader;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang.StringUtils;
import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Long.min;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isNotBlank;

public class MarkovAnalyzer {
    private static final int AVERAGE_WORD_SIZE = 5;
    private static final int INT_SET_SIZE = 10;
    private static final Logger LOG = LoggerFactory.getLogger(MarkovAnalyzer.class);

    private final Dict dict;
    private final List<String> wordIndex;

    public MarkovAnalyzer(WordReader reader) throws IOException {
        final long initSize = reader.getFileLength() / AVERAGE_WORD_SIZE;
        if (initSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(reader.getFileName() + " file is to big: " + reader.getFileLength() + " bytes");
        }

        dict = new Dict((int) initSize);
        wordIndex = new ArrayList<>((int) initSize);
        load(reader);
    }

    private MarkovAnalyzer(Dict dict, List<String> wordIndex) {
        this.dict = dict;
        this.wordIndex = wordIndex;
    }

    private void load(WordReader reader) throws IOException {
        LOG.info("Load data: start");
        try {
            String word = reader.readWord();
            while (word != null) {
                addWord(word);
                word = reader.readWord();
            }
        } finally {
            reader.close();
            LOG.info("Data load is done. Total " + wordIndex.size() + " words, " + dict.size() + " unique.");
        }
    }

    public void addWord(String word) {
        int index = wordIndex.size();
        dict.get(word).add(index);
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

        final List<TIntSet> sets = new ArrayList<>(words.size());
        int minSetIndex = -1;
        int minSetSize = Integer.MAX_VALUE;
        for (String word : words) {
            final TIntSet set = dict.get(word);
            sets.add(new TIntHashSet(set));
            if (set.size() < minSetSize) {
                minSetSize = set.size();
                minSetIndex = sets.size() - 1;//i
            }
        }
        TIntSet minSet = sets.get(minSetIndex);
        for (int i = 0; i < words.size(); i++) {
            filterSets(minSet, sets.get(i), i - minSetIndex);
        }

        return "Words " + words + " are " +
                (sets.get(0).isEmpty() ?
                        "not found" :
                        ("found " + sets.get(0).size() + " times on positions: " + sets.toString())) +
                "\n  Preceding words: " + evalIndexWords(sets.get(0), -1) +
                "\n  Following words: " + evalIndexWords(sets.get(0), words.size());

    }

    private String evalIndexWords(TIntSet set, int offset) {
        TIntIterator i = set.iterator();
        CountingSet res = new CountingSet();
        while (i.hasNext()) {
            int index = i.next() + offset;
            if (index >= 0 && index < wordIndex.size()) {
                res.add(wordIndex.get(index));
            }
        }
        return res.toString();
    }

    private void filterSets(TIntSet set1, TIntSet set2, int offset) {
        if (set1 != set2) {
            removeIf(set2, set1, -offset);
            removeIf(set1, set2, offset);
        }
    }

    private void removeIf(TIntSet set1, TIntSet set2, int offset) {
        for (TIntIterator iterator = set1.iterator(); iterator.hasNext(); ) {
            int next = iterator.next();
            if (!set2.contains(next + offset)) {
                iterator.remove();
            }
        }
    }

    private static FSTConfiguration createFSTConf() {
        final FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        conf.setShareReferences(false);
        conf.registerClass(Dict.class, TIntHashSet.class, String.class);
        return conf;
    }

    public void saveToFile(String fileName) throws IOException {
        FSTObjectOutput out = new FSTObjectOutput(new FileOutputStream(fileName), createFSTConf());
        out.writeObject(dict);
        out.writeObject(wordIndex);
        out.close();
    }

    public static MarkovAnalyzer loadFromFile(String fileName) throws IOException, ClassNotFoundException {
        final FSTObjectInput objInput = new FSTObjectInput(new FileInputStream(fileName), createFSTConf());
        final Dict dict = (Dict) objInput.readObject();
        final List<String> wordIndex = (List<String>) objInput.readObject();
        objInput.close();
        return new MarkovAnalyzer(dict, wordIndex);
    }

    private static class CountingSet extends TObjectIntHashMap<String> {
        public CountingSet() {
        }

        public void add(String word) {
            put(word, get(word) + 1);
        }
    }

    private static class Dict extends THashMap<String, TIntSet> {

        public Dict() {
        }

        public Dict(int size) {
            super(size);
        }

        @Override
        public TIntSet get(Object key) {
            TIntSet set = super.get(key);
            if (set == null) {
                set = new TIntHashSet(INT_SET_SIZE);
                put((String) key, set);
            }
            return set;
        }
    }

//    public static class Node{
//        public final FreqMap before = new FreqMap();
//        public final FreqMap after = new FreqMap();
//        @Override
//        public String toString() {
//            return "Node{" +
//                    "before=" + before +
//                    ", after=" + after +
//                    '}';
//        }
//    }

//    public static class FreqMap extends HashMap<String, Integer>{
//        public static final int ONE = 1;
//
//        public void add(String word) {
//            if (!containsKey(word)) {
//                put(word, ONE);
//            } else {
//                put(word, get(word) + ONE);
//            }
//        }
//    }
}

