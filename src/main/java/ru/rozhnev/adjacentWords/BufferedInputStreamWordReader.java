package ru.rozhnev.adjacentWords;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;

import static java.lang.Character.isAlphabetic;
import static java.lang.Character.toLowerCase;

public class BufferedInputStreamWordReader implements WordReader {

    private static final Logger LOG = LoggerFactory.getLogger(BufferedInputStreamWordReader.class);

    private final String fileName;
    private final long length;
    private final long startTime;
    private final InputStreamReader isr;
    private final FileInputStream fis;
    private final FileChannel chan;

    private int lastReport = 0;
    private int totalWordsSize = 0;
    private int wordsCount = 0;

    public BufferedInputStreamWordReader(String fileName, String charsetName)
            throws FileNotFoundException, UnsupportedEncodingException {
        fis = new FileInputStream(fileName);
        chan = fis.getChannel();
        isr = new InputStreamReader(new BufferedInputStream(fis, BUFF_SIZE), charsetName);
        length = new File(fileName).length();
        this.fileName = fileName;
        startTime = System.currentTimeMillis();
        LOG.info("Starting load of " + fileName + " ("+charsetName+"), length is " + length);
    }

    @Override
    @Nullable
    public String readWord() throws IOException {
        //todo CharBuffer?
        StringBuilder buf = new StringBuilder(16);
        while (true) {
            int c = isr.read();
            if (c == -1) {
                break;
            }
            if (!isAlphabetic(c)) {
                if (buf.length() > 0) {
                    break;
                } else {
                    continue;//skip several whitespaces
                }
            }
            buf.append((char)toLowerCase(c));
        }
        if (buf.length() == 0) {
            return null;
        }
        wordsCount++;
        totalWordsSize += buf.length();
        if (needLogProgress()) {
            LOG.info(logProgress());
        }
        return buf.toString().intern();
    }

    private boolean needLogProgress() {
        return wordsCount - lastReport > PROGRESS_LOG_PERIOD;
    }

    private String logProgress() throws IOException {
        lastReport = wordsCount;
        int elapsed = (int) ((System.currentTimeMillis() - startTime) / 1000L);
        int wSpeed = wordsCount / elapsed;
        int bSpeed = (int) (chan.position() * 60 / 1024 / 1024 / elapsed);
        return wordsCount + " words (" + 100L * chan.position() / length + "%) loaded, "
                +wSpeed+" w/s,"
                +bSpeed+" MiB/min,";
    }

    @Override
    public void close() throws IOException {
        isr.close();
        LOG.info("Load time is " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
        LOG.info("Average word length is " + 1D * totalWordsSize / wordsCount);
    }

    @Override
    public long getFileLength() {
        return length;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

}
