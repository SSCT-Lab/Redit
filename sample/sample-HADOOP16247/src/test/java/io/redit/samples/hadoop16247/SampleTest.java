package io.redit.samples.hadoop16247;
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
    private static final String RELATIVE_PATH = "wc.txt";
    private static final String RELATIVE_PATH_W_SPACE = "wc\\ 2.txt";
    private static String data = "hello hdfs hello hadoop";

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
    public void sampleTest() throws RuntimeEngineException, InterruptedException {
        logger.info("start mapreduce job");
        runner.runtime().runCommandInNode("nn1", "touch " + RELATIVE_PATH + " && echo \"" + data +  "\" >> " + RELATIVE_PATH + " && "
                + ReditHelper.getHadoopHomeDir() + "/bin/hadoop fs -mkdir /input && " + ReditHelper.getHadoopHomeDir() + "/bin/hadoop fs -put " + RELATIVE_PATH + " /input");

        runner.runtime().runCommandInNode("nn1", "touch " + RELATIVE_PATH_W_SPACE + " && echo \"" + data +  "\" >> " + RELATIVE_PATH_W_SPACE);
        CommandResults catInput = runner.runtime().runCommandInNode("nn1", "cat " + RELATIVE_PATH_W_SPACE);
        logger.info(catInput.command() + " : " + catInput.stdOut());

        CommandResults checkInput = runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hadoop fs -put " + RELATIVE_PATH_W_SPACE + " /input");
        if (checkInput.exitCode()!= 0){
            logger.warn(checkInput.command() + " : " + checkInput.stdErr());
        }

        mapreduce();
        logger.info("wait for mapreduce ...");
        Thread.sleep(10000);
        CommandResults res = runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hadoop fs -cat /output/*");
        if (res.exitCode() != 0) {
            logger.info("error: " + res.stdErr());
        }else {
            logger.info("mapreduce result: " + res.stdOut());
        }
    }

    public static void mapreduce() {
        new Thread(() -> {
            try {
                runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hadoop jar "
                        + ReditHelper.getHadoopHomeDir() + "/share/hadoop/mapreduce/hadoop-mapreduce-examples-3.1.2.jar wordcount /input /output");
            } catch (RuntimeEngineException e) {
                e.printStackTrace();
            }
        }).start();
    }

}