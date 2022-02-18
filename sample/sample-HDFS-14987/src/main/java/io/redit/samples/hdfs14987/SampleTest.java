package io.redit.samples.hdfs14987;

import io.redit.ReditRunner;
import io.redit.exceptions.RuntimeEngineException;
import io.redit.execution.CommandResults;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Arrays;

public class SampleTest {
    private static final Logger logger = LoggerFactory.getLogger(SampleTest.class);
    protected static ReditRunner runner;
    private static final String policy = "XOR-2-1-1024k";
    private static final String ecFilePath = "/test_ec";
    private static final String replicaFilePath = "/test_replica";
    private static final String testFile = "aa.txt";
    private static final String data = "hello hadoop hello hdfs";
    private static DistributedFileSystem dfs = null;

    @BeforeClass
    public static void before() throws RuntimeEngineException, ParserConfigurationException, IOException, SAXException, TransformerException, InterruptedException {

        runner = ReditRunner.run(ReditHelper.getDeployment());
        ReditHelper.startNodesInOrder(runner);
        ReditHelper.waitActive();
        logger.info("The cluster is UP!");

        ReditHelper.transitionToActive(1, runner);
        ReditHelper.checkNNs(runner);
        dfs = ReditHelper.getDFS(runner);
    }

    @AfterClass
    public static void after() {
        if (runner != null) {
            runner.stop();
        }
    }

    @Test
    public void sampleTest() throws RuntimeEngineException, IOException {

        dfs.enableErasureCodingPolicy(policy);
        dfs.mkdirs(new Path(ecFilePath));
        dfs.mkdirs(new Path(replicaFilePath));
        dfs.setErasureCodingPolicy(new Path(ecFilePath), policy);

        runner.runtime().runCommandInNode("nn1", "touch " + testFile + " && echo \"" + data + "\" >> " + testFile);
        runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hdfs dfs -put " + testFile + " " + ecFilePath);
        runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hdfs dfs -put " + testFile + " " + replicaFilePath);

        logger.info(String.valueOf(dfs.getFileEncryptionInfo(new Path(ecFilePath + "/" + testFile))));
        logger.info(Arrays.toString(dfs.getFileBlockLocations(new Path(ecFilePath + "/" + testFile),0 ,8)));

        String ViewBlocksCommand_ec = ReditHelper.getHadoopHomeDir() + "/bin/hdfs fsck " + ecFilePath + "/" + testFile + " -files -blocks -locations";
        logger.info(ViewBlocksCommand_ec);
        logger.info(runner.runtime().runCommandInNode("nn1", ViewBlocksCommand_ec).stdOut());
        // TODO  If use fsck, blocking on the command ViewBlocksCommand_ec.

        String ViewBlocksCommand_replica = ReditHelper.getHadoopHomeDir() + "/bin/hdfs fsck " + replicaFilePath + "/" + testFile + " -files -blocks -locations";
        logger.info(ViewBlocksCommand_replica);
        logger.info(runner.runtime().runCommandInNode("nn1", ViewBlocksCommand_replica).stdOut());

    }

}