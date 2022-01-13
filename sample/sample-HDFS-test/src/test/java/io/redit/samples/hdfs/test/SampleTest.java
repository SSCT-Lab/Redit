package io.redit.samples.hdfs.test;

import com.google.common.base.Supplier;
import io.redit.ReditRunner;
import io.redit.exceptions.RuntimeEngineException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.AppendTestUtil;
import org.apache.hadoop.hdfs.DFSConfigKeys;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertFalse;

public class SampleTest {

    private static final Logger logger = LoggerFactory.getLogger(SampleTest.class);
    protected static ReditRunner runner;
    private static final int BLOCK_SIZE = 4096;
    private static FSDataOutputStream stm;
    private static final Path TEST_PATH = new Path("/test-file");

    @BeforeClass
    public static void before() throws RuntimeEngineException, InterruptedException, ParserConfigurationException, IOException, SAXException, TransformerException, TimeoutException {

        runner = ReditRunner.run(ReditHelper.getDeployment());
        ReditHelper.startNodesInOrder(runner);
        ReditHelper.waitActive();
        logger.info("The cluster is UP!");


        ReditHelper.transitionToActive(1, runner);

        FileSystem fs = ReditHelper.getFileSystem(runner);
        stm = fs.create(TEST_PATH);

        // write a half block
        AppendTestUtil.write(stm, 0, BLOCK_SIZE / 2);
        stm.hflush();

        final Configuration conf = new Configuration();
        // Disable permissions so that another user can recover the lease.
        conf.setBoolean(DFSConfigKeys.DFS_PERMISSIONS_ENABLED_KEY, false);
        conf.setInt(DFSConfigKeys.DFS_BLOCK_SIZE_KEY, BLOCK_SIZE);
        DistributedFileSystem fsOtherUser = createFsAsOtherUser(conf);
        assertFalse(fsOtherUser.recoverLease(TEST_PATH));

        runner.runtime().enforceOrder("t1", () -> {
            logger.info("Failing over to NN 1");

            ReditHelper.transitionToStandby(1, runner);
            ReditHelper.transitionToActive(2, runner);
        });

        runner.runtime().waitForRunSequenceCompletion(30);

        loopRecoverLease(fsOtherUser, TEST_PATH);

        AppendTestUtil.check(fs, TEST_PATH, BLOCK_SIZE/2);

    }

    @AfterClass
    public static void after() {
        IOUtils.closeStream(stm);
        if (runner != null) {
            runner.stop();
        }
    }


    @Test
    public void sampleTest() throws RuntimeEngineException, SQLException, ClassNotFoundException, TimeoutException {

    }

    private static DistributedFileSystem createFsAsOtherUser(final Configuration conf)
            throws IOException, InterruptedException {
        return (DistributedFileSystem) UserGroupInformation.createUserForTesting(
                "otheruser", new String[] { "othergroup"})
                .doAs(new PrivilegedExceptionAction<FileSystem>() {
                    @Override
                    public FileSystem run() throws Exception {
                        return ReditHelper.getFileSystem(runner);
                    }
                });
    }

    /**
     * Try to recover the lease on the given file for up to 60 seconds.
     * @param fsOtherUser the filesystem to use for the recoverLease call
     * @param testPath the path on which to run lease recovery
     * @throws TimeoutException if lease recover does not succeed within 60
     * seconds
     * @throws InterruptedException if the thread is interrupted
     */
    private static void loopRecoverLease(final FileSystem fsOtherUser, final Path testPath) throws TimeoutException, InterruptedException {
        try {
            GenericTestUtils.waitFor(new Supplier<Boolean>() {
                @Override
                public Boolean get() {
                    boolean success;
                    try {
                        success = ((DistributedFileSystem)fsOtherUser)
                                .recoverLease(testPath);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    if (!success) {
                        logger.info("Waiting to recover lease successfully");
                    }
                    return success;
                }
            }, 1000, 60000);
        } catch (TimeoutException e) {
            throw new TimeoutException("Timed out recovering lease for " + testPath);
        }
    }
}