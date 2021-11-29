package io.redit.samples.hdfs.test;

import io.redit.ReditRunner;
import io.redit.exceptions.RuntimeEngineException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

public class SampleTest {

    private static final Logger logger = LoggerFactory.getLogger(SampleTest.class);
    protected static ReditRunner runner;
    private static final int BLOCK_SIZE = 4096;

    @BeforeClass
    public static void before() throws RuntimeEngineException, InterruptedException, ParserConfigurationException, IOException, SAXException, TransformerException {

        final Configuration conf = new Configuration();
        // Disable permissions so that another user can recover the lease.
        conf.setBoolean(DFSConfigKeys.DFS_PERMISSIONS_ENABLED_KEY, false);
        conf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);

        FSDataOutputStream stm = null;

        runner = ReditRunner.run(ReditHelper.getDeployment());
        ReditHelper.startNodesInOrder(runner);
        ReditHelper.waitActive();

        logger.info("The cluster is UP!");
    }

    @AfterClass
    public static void after() {
        if (runner != null) {
            runner.stop();
        }
    }


    @Test
    public void sampleTest() throws RuntimeEngineException, SQLException, ClassNotFoundException, TimeoutException {

    }
}