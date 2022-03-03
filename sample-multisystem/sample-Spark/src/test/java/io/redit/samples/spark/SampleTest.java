package io.redit.samples.spark;

import io.redit.ReditRunner;
import io.redit.exceptions.RuntimeEngineException;
import io.redit.execution.CommandResults;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.Dataset;
public class SampleTest {
    private static final Logger logger = LoggerFactory.getLogger(SampleTest.class);

    protected static ReditRunner runner;

    protected static final int NUM_OF_SLAVES = 3;
    protected static final int NUM_OF_MASTERS = 3;
    @BeforeClass
    public static void before() throws RuntimeEngineException, InterruptedException {
        runner = ReditRunner.run(ReditHelper.getDeployment(NUM_OF_MASTERS,NUM_OF_SLAVES));
        ReditHelper.startNodesInOrder(runner);


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
//        String logFile = "pom.xml"; // Should be some file on your system
//        // due to the data, now exist a lambda problem
//        SparkConf conf = new SparkConf();
//        conf.setMaster(ReditHelper.getClientMasterString(runner, NUM_OF_MASTERS));
//        SparkSession spark = SparkSession.builder().config(conf).appName("test").getOrCreate();
//
//        Dataset<String> logData = spark.read().textFile(logFile).cache();
//
//        long numAs = logData.filter((FilterFunction<String>) s -> s.contains("a")).count();
//        long numBs = logData.filter((FilterFunction<String>) s -> s.contains("b")).count();
//
//        System.out.println("Lines with a: " + numAs + ", lines with b: " + numBs);
//
//        spark.stop();

        //TODO to run original code, we should fix the problem of 
        logger.info("Start load test case...");
        for(int i=1;i<=3;i++){
            CommandResults listPolicies = runner.runtime().runCommandInNode("master"+i, "jps");
            logger.info("\nmaster"+i+"\n"+listPolicies.command() + " : " + listPolicies.stdOut());
        }
        for(int i=1;i<=3;i++){
            CommandResults listPolicies = runner.runtime().runCommandInNode("slave"+i, "jps");
            logger.info("\nslave"+i+"\n"+listPolicies.command() + " : " + listPolicies.stdOut());
        }


    }
}