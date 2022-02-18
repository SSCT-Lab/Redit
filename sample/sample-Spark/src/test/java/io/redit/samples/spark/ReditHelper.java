package io.redit.samples.spark;

import io.redit.ReditRunner;
import io.redit.dsl.entities.Deployment;
import io.redit.dsl.entities.PathAttr;
import io.redit.dsl.entities.PortType;
import io.redit.dsl.entities.ServiceType;
import io.redit.exceptions.RuntimeEngineException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringJoiner;

public class ReditHelper {
    public static final Logger logger = LoggerFactory.getLogger(ReditHelper.class);

    public static Deployment getDeployment(int numOfMasters, int numOfSlaves) {

        String version = "2.4.3"; // this can be dynamically generated from maven metadata
        String dir = "spark-" + version + "-bin-custom-spark";
        String tmpdir="/spark/spark-2.4.3-bin-custom-spark/";
        return Deployment.builder("example-spark")
                .withService("zk").dockerImageName("redit/zk:3.4.14").dockerFileAddress("docker/zk", true).disableClockDrift().and()
                .withService("spark-base")
                .applicationPath("../../spark-2.4.3-build/" + dir + ".tar.gz", "/spark", PathAttr.COMPRESSED)
                .dockerImageName("redit/spark:1.0").dockerFileAddress("docker/Dockerfile", true)
                .workDir("/spark/" + dir).logDirectory("/spark/" + dir + "/logs").serviceType(ServiceType.SCALA).and()

                .withService("spark-master", "spark-base")
                .startCommand(tmpdir+"sbin/start-master.sh").tcpPort(7077)
                .environmentVariable("SPARK_DAEMON_JAVA_OPTS", "-Dspark.deploy.recoveryMode=ZOOKEEPER -Dspark.deploy.zookeeper.url=zk1:2181 "
                        + "-Dzookeeper.sasl.client=false").and().nodeInstances(numOfMasters, "master", "spark-master", true)

                .withService("spark-slave", "spark-base")
                .startCommand(tmpdir+"sbin/start-slave.sh -c 1 -m 1G " + getMasterString(numOfMasters)).and()
                .nodeInstances(numOfSlaves, "slave", "spark-slave", true).withNode("zk1", "zk").and().build();
    }


    private static String getMasterString(int numOfMasters) {
        StringJoiner joiner = new StringJoiner(",");
        for (int i=1; i<=numOfMasters; i++) joiner.add("master" + i + ":7077");
        return "spark://" + joiner.toString();
    }

    public static String getClientMasterString(ReditRunner runner, int numOfMasters) {
        StringJoiner joiner = new StringJoiner(",");
        for (int i=1; i<=numOfMasters; i++)
            joiner.add(runner.runtime().ip("master" + i) + ":" +
                    runner.runtime().portMapping("master" + i, 7077, PortType.TCP));
        return "spark://" + joiner.toString();
    }

    public static void startNodesInOrder(ReditRunner runner) throws InterruptedException, RuntimeEngineException {
        Thread.sleep(10000);
        for (String node: runner.runtime().nodeNames()) {
            if (node.startsWith("master"))
                runner.runtime().startNode(node);
        }
        Thread.sleep(10000);
        for (String node: runner.runtime().nodeNames()) {
            if (node.startsWith("slave"))
                runner.runtime().startNode(node);
        }
        Thread.sleep(10000);
    }
}
