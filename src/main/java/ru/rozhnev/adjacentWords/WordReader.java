package ru.rozhnev.adjacentWords;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import static java.lang.Character.*;

public class WordReader extends InputStreamReader {

    private static final int BUFF_SIZE = 1024 * 1024;
    private static final Logger LOG = LoggerFactory.getLogger(WordReader.class);
    private static final long PROGRESS_LOG_PERIOD = 10 * 1000 * 1000-1;

    private final String fileName;
    private final long length;
    private final long startTime;

    private long lastReport = 0;
    private long progress = 0;
    private int totalWordsSize = 0;
    private int wordsCount = 0;

    public WordReader(String fileName, String charsetName)
            throws FileNotFoundException, UnsupportedEncodingException {
        super(new BufferedInputStream(new FileInputStream(fileName), BUFF_SIZE), charsetName);
        length = new File(fileName).length();
        this.fileName = fileName;
        startTime = System.currentTimeMillis();
        LOG.info("Starting load of " + fileName + ", length is " + length);
    }

    @Nullable
    public String readWord() throws IOException {
        //todo CharBuffer?
        CharArrayWriter writer = new CharArrayWriter();
        while (true) {
            int c = read();
            if (c == -1) {
                break;
            }
            progress++;
            if (!isAlphabetic(c)) {
                if (writer.size() > 0) {
                    break;
                } else {
                    continue;//skip several whitespaces
                }
            }
            writer.write(toLowerCase(c));
        }
        if (writer.size() == 0) {
            return null;
        }
        wordsCount++;
        totalWordsSize += writer.size();
        if (needLogProgress()) {
            LOG.info(logProgress());
        }
        return writer.toString();
    }

    private int toLowerCaseWin1251(int b) {
        if (0xC0 <= b && b <= 0xDF) {
            return b + 0x20;
        }
        throw new NotImplementedException();
    }

    private boolean isLetterWin1251(int b) {
        return b >= 0xC0 //rus letters
                || (0x41 <= b && b <= 0x5A) //latin little
                || (0x61 <= b && b <= 0x7A); //latin capital
    }

    private boolean needLogProgress() {
        return wordsCount - lastReport > PROGRESS_LOG_PERIOD;
    }

    private String logProgress() {
        lastReport = wordsCount;
        return wordsCount + " words (" + 100L * progress / length + "%) loaded";
    }

    @Override
    public void close() throws IOException {
        super.close();
        LOG.info("Load time is " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
        LOG.info("Average word length is " + 1D * totalWordsSize / wordsCount);
    }

    public long getFileLength() {
        return length;
    }

    public String getFileName() {
        return fileName;
    }

}
