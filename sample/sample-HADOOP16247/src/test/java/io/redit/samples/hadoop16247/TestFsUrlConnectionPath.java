package io.redit.samples.hadoop16247;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FsUrlStreamHandlerFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;

/**
 + * Test case for FsUrlConnection with relativePath and SPACE.
 + */
public class TestFsUrlConnectionPath {

    private static final Logger logger = LoggerFactory.getLogger(TestFsUrlConnectionPath.class);
    private static final String CURRENT = Paths.get("").toAbsolutePath().toString();
    private static final String ABSOLUTE_PATH = "file:" + CURRENT + "/abs.txt";
    private static final String RELATIVE_PATH = "file:relative.txt";
    private static final String ABSOLUTE_PATH_W_SPACE = "file:" + CURRENT + "/abs 1.txt";
    private static final String RELATIVE_PATH_W_SPACE = "file:relative 1.txt";
    private static final String ABSOLUTE_PATH_W_ENCODED_SPACE = "file:" + CURRENT + "/abs%201.txt";
    private static final String RELATIVE_PATH_W_ENCODED_SPACE = "file:relative%201.txt";
    private static final String DATA = "data";


    @BeforeClass
    public static void initialize() throws IOException{
        write(ABSOLUTE_PATH.substring(5), DATA);
        write(RELATIVE_PATH.substring(5), DATA);
        write(ABSOLUTE_PATH_W_SPACE.substring(5), DATA);
        write(RELATIVE_PATH_W_SPACE.substring(5), DATA);
        URL.setURLStreamHandlerFactory(new FsUrlStreamHandlerFactory());
    }

    @AfterClass
    public static void cleanup(){
        delete(ABSOLUTE_PATH.substring(5));
        delete(RELATIVE_PATH.substring(5));
        delete(ABSOLUTE_PATH_W_SPACE.substring(5));
        delete(RELATIVE_PATH_W_SPACE.substring(5));
    }

    public static void delete(String path){
        File file = new File(path);
        file.delete();
    }

    public static void write(String path, String data) throws IOException{
        File file = new File(path);
        file.createNewFile();
        FileWriter fw = new FileWriter(file);
        fw.write(data);
        fw.close();
    }

    public static int readStream(String path) throws Exception{
        URL url = new URL(path);
        InputStream is = url.openStream(); // Exception
        return is.available();
    }


    @Test
    public void testAbsolutePath() throws Exception{
        logger.info("readStream: " + ABSOLUTE_PATH);
        int length = readStream(ABSOLUTE_PATH);
        logger.info("read length: " + length);
        Assert.assertTrue(length > 1);
    }

    @Test
    public void testRelativePath() throws Exception{
        logger.info("readStream: " + RELATIVE_PATH);
        int length = readStream(RELATIVE_PATH);
        logger.info("read length: " + length);
        Assert.assertTrue(length > 1);
    }

    @Test
    public void testAbsolutePathWithSpace() throws Exception{
        logger.info("readStream: " + ABSOLUTE_PATH_W_ENCODED_SPACE);
        int length = readStream(ABSOLUTE_PATH_W_ENCODED_SPACE);
        logger.info("read length: " + length);
        Assert.assertTrue(length > 1);
    }

    @Test
    public void testRelativePathWithSpace() throws Exception{
        logger.info("readStream: " + RELATIVE_PATH_W_ENCODED_SPACE);
        int length = readStream(RELATIVE_PATH_W_ENCODED_SPACE);
        logger.info("read length: " + length);
        Assert.assertTrue(length > 1);
    }
}
