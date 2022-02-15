package io.redit.samples.hdfs14679;

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
    private static final String xmlPath = ReditHelper.getHadoopHomeDir() + "/etc/hadoop/user_ec_policies.xml.template";

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

        CommandResults listPolicies = runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hdfs ec -listPolicies");
        logger.info(listPolicies.command() + " : " + listPolicies.stdOut());

        logger.info("The example on adding erasure code policies with provided template: " + xmlPath);
        CommandResults addPolicies = runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hdfs ec -addPolicies -policyFile " + xmlPath );
        logger.warn(addPolicies.command() + " : " + addPolicies.stdOut());

    }
}