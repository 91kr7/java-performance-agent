package com.cmdev.profiler.instrument.io;

import java.io.*;
import java.util.zip.GZIPOutputStream;

public class ZipUtils {

    public static void gzipAndDeleteFile(String filePath) {
        try {
            File fileToGzip = new File(filePath);
            if (!fileToGzip.exists() || !fileToGzip.isFile()) {
                throw new FileNotFoundException("File not found: " + filePath);
            }

            String gzipFilePath = filePath + ".gz";

            try (FileInputStream fis = new FileInputStream(fileToGzip);
                 FileOutputStream fos = new FileOutputStream(gzipFilePath);
                 GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    gzos.write(buffer, 0, length);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (!fileToGzip.delete()) {
                throw new IOException("Unable to delete original file: " + filePath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
