package ru.rozhnev.adjacentWords;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println(printGC());
//        WordReader reader = WordReader.create("data/testMy.txt", "UTF-8");
//        WordReader reader = WordReader.create("data/log400.txt", "windows-1251");
        WordReader reader = WordReader.create("data/test.txt", "CP866");
//        WordReader reader = WordReader.create("data/test200.txt", "CP866");
//        WordReader reader = WordReader.create("data/hebrew.txt", "UTF-8");

        MarkovAnalyzer a = new MarkovAnalyzer(reader);
//        a.saveToFile(reader.getFileName()+".bin");
        System.out.println(printGcUsage());

//        System.out.println(a.analyze(" I "));
//        System.out.println(a.analyze(" see "));
//        System.out.println(a.analyze("פריז"));
        System.out.println(a.analyze(" I   see "));
        System.out.println(a.analyze("ci", "sara", "cola", "dove", "immenso", "gli", "astri", "dan", "suono"));
        System.out.println(a.analyze("bernard amp graefe verlag fur wehrwesen frankfurt a m"));
        System.out.println(printGcUsage());
    }

    private static String printGC() {
        return ManagementFactory.getGarbageCollectorMXBeans().stream().map(GarbageCollectorMXBean::getName).collect(Collectors.joining(", "));
    }
    private static String printGcUsage() {
        long millis = ManagementFactory.getGarbageCollectorMXBeans().stream().map(GarbageCollectorMXBean::getCollectionTime).reduce(Long::sum).get();
        System.gc();
        Runtime runtime = Runtime.getRuntime();

        return "GC time is " + (millis/1000) + " s " +
                "Used Memory:"
                + ((runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
    }

}
