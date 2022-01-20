package io.redit.samples.zookeeper;

import io.redit.ReditRunner;
import io.redit.exceptions.RuntimeEngineException;
import io.redit.execution.CommandResults;
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

    @BeforeClass
    public static void before() throws RuntimeEngineException, InterruptedException, ParserConfigurationException, IOException, SAXException, TransformerException {
        runner = ReditRunner.run(ReditHelper.getDeployment(3));
        ReditHelper.startNodesInOrder(runner);
       // ReditHelper.checkZookeeper(runner);
        Thread.sleep(10000);
//        ReditHelper.waitActive();
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
       // CommandResults res = runner.runtime().runCommandInNode("zk1" , "get /hadoop-ha | bin/zkCli.sh -server localhost:2181");
        CommandResults res = runner.runtime().runCommandInNode("zk1" , "echo stat |nc 127.0.0.1 2181");
        logger.info(res.toString());
        logger.info(res.stdOut());
    }
}