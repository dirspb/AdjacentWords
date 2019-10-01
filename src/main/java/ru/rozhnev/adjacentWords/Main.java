package ru.rozhnev.adjacentWords;

import java.io.IOException;

public class Main {


    public static void main(String[] args) throws IOException {
        WordReader reader = new WordReader("data/testMy.txt", "UTF-8");
//        WordReader reader = new WordReader("data/log400.txt", "windows-1251");
//        WordReader reader = new WordReader("data/test.txt", "CP866");
//        WordReader reader = new WordReader("data/test-utf8.txt", "UTF-8");

        MarkovAnalyzer a = new MarkovAnalyzer(reader);
        a.saveToFile(reader.getFileName()+".bin");

        System.out.println(a.analyze(" I "));
        System.out.println(a.analyze(" see "));
        System.out.println(a.analyze(" I   see "));
        System.out.println(a.analyze(" I ", " see "));
        System.out.println(a.analyze("ci", "sara", "cola", "dove", "immenso", "gli", "astri", "dan", "suono"));
        System.out.println(a.analyze("bernard amp graefe verlag fur wehrwesen frankfurt a m"));
        System.out.println(a.analyze("bernard amp graefe verlag fur wehrwesen frankfurt a m"));


    }
}
