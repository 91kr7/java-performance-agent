package com.cmdev.profiler.instrument.io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import static com.cmdev.profiler.instrument.io.ZipUtils.gzipAndDeleteFile;

public class PerformanceFileWriter {

    private final BufferedWriter writer;
    private final String filePath;

    public PerformanceFileWriter(String filePath) {
        try {
            this.filePath = filePath;
            this.writer = new BufferedWriter(new FileWriter(this.filePath), 16 * 1024);
        } catch (IOException e) {
            System.err.println("[CMDev] PerformanceFileWriter: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void writeLine(String riga) throws IOException {
        writer.write(riga);
        writer.newLine();
    }

    public void close() throws IOException {
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.err.println("[CMDev] Error closing writer: " + e.getMessage());
            throw e;
        }
        try {
            gzipAndDeleteFile(this.filePath);
        } catch (Exception e) {
            System.err.println("[CMDev] Error zipping file: " + e.getMessage());
        }
    }
}
