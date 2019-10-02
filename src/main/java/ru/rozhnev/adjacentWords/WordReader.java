package ru.rozhnev.adjacentWords;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public interface WordReader {
    int BUFF_SIZE = 1024 * 1024;
    int PROGRESS_LOG_PERIOD = 3 * 1000 * 1000 - 1;

    static WordReader create(String fileName, String charsetName) throws FileNotFoundException, UnsupportedEncodingException {
        return new BufferedInputStreamWordReader(fileName, charsetName);
    }

    @Nullable
    String readWord() throws IOException;

    void close() throws IOException;

    long getFileLength();

    String getFileName();
}
