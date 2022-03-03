package io.redit.samples.hdfs14852;

import io.redit.ReditRunner;
import io.redit.exceptions.RuntimeEngineException;
import org.apache.hadoop.hdfs.protocol.Block;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import org.apache.hadoop.hdfs.server.blockmanagement.*;
import static org.junit.Assert.assertFalse;

public class SampleTest {
    private static final Logger logger = LoggerFactory.getLogger(SampleTest.class);
    protected static ReditRunner runner;

    @BeforeClass
    public static void before() throws RuntimeEngineException, ParserConfigurationException, IOException, SAXException, TransformerException, InterruptedException {

//        runner = ReditRunner.run(ReditHelper.getDeployment());
//        ReditHelper.startNodesInOrder(runner);
//        ReditHelper.waitActive();
//        logger.info("The cluster is UP!");
//
//        ReditHelper.transitionToActive(1, runner);
//        ReditHelper.checkNNs(runner);

    }

    @AfterClass
    public static void after() {
        if (runner != null) {
            runner.stop();
        }
    }

    @Test
    public void sampleTest1() {

        LowRedundancyBlocks neededReconstruction = new LowRedundancyBlocks();
        BlockInfo block = new BlockInfoContiguous(new Block(), (short)1024);
        neededReconstruction.add(block, 2, 0, 1, 3);
        neededReconstruction.add(block, 0, 0, 0, 3);
        neededReconstruction.remove(block, LowRedundancyBlocks.LEVEL);
        assertFalse("Should not contain the block.", neededReconstruction.contains(block));

    }

    // HDFS-14852.007.patch: 如果在给定优先级的队列中未找到该块，请尝试从所有队列中删除该块。
    @Test
    public void sampleTest2() {

        LowRedundancyBlocks neededReconstruction = new LowRedundancyBlocks();
        BlockInfo block = new BlockInfoContiguous(new Block(), (short)1024);
        neededReconstruction.add(block, 2, 0, 1, 3);
        neededReconstruction.add(block, 0, 0, 0, 3);
        neededReconstruction.remove2(block, LowRedundancyBlocks.LEVEL);
        assertFalse("Should not contain the block.", neededReconstruction.contains(block));

    }

}