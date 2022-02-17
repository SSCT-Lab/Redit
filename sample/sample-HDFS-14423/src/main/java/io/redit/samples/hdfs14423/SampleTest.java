package io.redit.samples.hdfs14423;

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

public class SampleTest {
    private static final Logger logger = LoggerFactory.getLogger(SampleTest.class);
    protected static ReditRunner runner;

    @BeforeClass
    public static void before() throws RuntimeEngineException, ParserConfigurationException, IOException, SAXException, TransformerException, InterruptedException {

        runner = ReditRunner.run(ReditHelper.getDeployment());
        ReditHelper.startNodesInOrder(runner);
        ReditHelper.waitActive();
        logger.info("The cluster is UP!");

        ReditHelper.transitionToActive(1, runner);
        ReditHelper.checkNNs(runner);
    }

    @AfterClass
    public static void after() {
        if (runner != null) {
            runner.stop();
        }
    }

    @Test
    public void sampleTest() throws RuntimeEngineException {

        CommandResults percent1= runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hdfs dfs -touchz /%");
        logger.info(percent1.command() + " : " + percent1.stdOut());
        CommandResults percent2= runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hdfs dfs -cat /%");
        logger.info(percent2.command() + " : " + percent2.stdOut());

        CommandResults plus1= runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hdfs dfs -touchz /a+b");
        logger.info(plus1.command() + " : " + plus1.stdOut());
        CommandResults plus2= runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hdfs dfs -mkdir /c+d");
        logger.info(plus2.command() + " : " + plus2.stdOut());
        CommandResults plusls= runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hdfs dfs -ls /");
        logger.info(plusls.command() + " : " + plusls.stdOut());

    }
}