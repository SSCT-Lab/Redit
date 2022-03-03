package io.redit.samples.hdfs16325;

import io.redit.ReditRunner;
import io.redit.dsl.entities.PortType;
import io.redit.exceptions.RuntimeEngineException;
import io.redit.execution.CommandResults;
import org.apache.hadoop.test.GenericTestUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.mockito.stubbing.Answer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;

import static org.junit.Assert.fail;

public class SampleTest {
    private static final Logger logger = LoggerFactory.getLogger(SampleTest.class);
    protected static ReditRunner runner;

    @BeforeClass
    public static void before() throws RuntimeEngineException, ParserConfigurationException, IOException, SAXException, TransformerException {
        runner = ReditRunner.run(ReditHelper.getDeployment());
        ReditHelper.startNodesInOrder(runner);
        ReditHelper.waitActive();
        logger.info("The cluster is UP!");
        ReditHelper.transitionToActive(1, runner);
        ReditHelper.transitionToObserver(3,runner);
        ReditHelper.checkNNs(runner);
    }

    @AfterClass
    public static void after() {
        if (runner != null) {
            runner.stop();
        }
    }

    @Test
    public void sampleTest() throws RuntimeEngineException, InterruptedException {
        //curl --negotiate -u ':' 'https://<master>:<port>/webhdfs/v1/...'
        String observerIp = runner.runtime().ip("nn3");
        int observerPort = runner.runtime().portMapping("nn3", ReditHelper.NN_HTTP_PORT, PortType.TCP);
        CommandResults curlUrl = runner.runtime().runCommandInNode("nn1", "curl --negotiate -v -u ':' 'http://" + observerIp + ":" + observerPort + "/webhdfs/v1/...'");
        logger.info(curlUrl.command() + " : " + curlUrl.stdOut());
        logger.error(curlUrl.command() + " : " + curlUrl.stdErr());

        // network delay ???
        GenericTestUtils.DelayAnswer delayer = new GenericTestUtils.DelayAnswer(logger);
        //Mockito.doAnswer(delayer).when()
        delayer.waitForCall();
        delayer.proceed();
        delayer.waitForResult();
        Throwable t = delayer.getThrown();
        if(t == null){
            fail("???");
        }
        GenericTestUtils.assertExceptionContains("???", t);
    }


}