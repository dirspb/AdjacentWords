package ru.rozhnev.adjacentWords;

import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.stream.Collectors;

import static java.lang.Runtime.getRuntime;

public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println(printGC());
        WordReader reader = WordReader.create(args[0], args[1]);
        TextAnalyzer a = new TextAnalyzer(reader);
//        a.saveToFile(reader.getFileName()+".bin");
        System.out.println("Longest repeat: " + a.findLongestRepeat().size());
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
        String gcTimes = ManagementFactory.getGarbageCollectorMXBeans().stream()
                .map(mxBean -> mxBean.getName() + ":" + (mxBean.getCollectionTime() / 1000L) + "s")
                .collect(Collectors.joining(" ,"));
        System.gc();

        return "GC times are " + gcTimes +
                ". Used Memory:"
                + ((getRuntime().totalMemory() - getRuntime().freeMemory()) / 1024 / 1024) + "MiB";
    }

}
