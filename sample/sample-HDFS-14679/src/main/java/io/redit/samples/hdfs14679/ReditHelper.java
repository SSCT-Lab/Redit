package io.redit.samples.hdfs14679;

import io.redit.ReditRunner;
import io.redit.dsl.entities.Deployment;
import io.redit.dsl.entities.PathAttr;
import io.redit.dsl.entities.ServiceType;
import io.redit.exceptions.RuntimeEngineException;
import io.redit.execution.CommandResults;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.StringJoiner;

public class ReditHelper {
    public static final Logger logger = LoggerFactory.getLogger(ReditHelper.class);
    private static final int NN_HTTP_PORT = 50070;
    private static final int NN_RPC_PORT = 8020;
    private static final int numOfNNs = 3;
    private static final int numOfDNs = 3;
    private static final int numOfJNs = 3;

    public static String getHadoopHomeDir() {
        String version = "3.1.2"; // this can be dynamically generated from maven metadata
        String dir = "hadoop-" + version;
        return "/hadoop/" + dir;
    }

    public static Deployment getDeployment() throws ParserConfigurationException, TransformerException, SAXException, IOException {

        String version = "3.1.2"; // this can be dynamically generated from maven metadata
        String dir = "hadoop-" + version;
        String fsAddress = "mycluster";
        String hdfsSiteFileName = "hdfs-site-ha.xml";
        Deployment.Builder builder = Deployment.builder("example-hdfs-hadoop-14679")
                .withService("hadoop-base")
                .applicationPath("../../hadoop-3.1.2-build/hadoop-dist/target/" + dir + ".tar.gz", "/hadoop",  PathAttr.COMPRESSED)
                .applicationPath("etc", getHadoopHomeDir() + "/etc").workDir(getHadoopHomeDir())
                .addSettingsToXml("etc/" + hdfsSiteFileName, getHadoopHomeDir() + "/etc/hadoop/hdfs-site.xml",
                        new HashMap<String, String>() {{
                            put("NN_STRING", getNNString());
                            put("NN_ADDRESSES", getNNAddresses());
                        }})
                .addSettingsToXml("etc/hadoop/core-site.xml", getHadoopHomeDir() + "/etc/hadoop/core-site.xml",
                        new HashMap<String, String>() {{ put("CLUSTER_ADDRESS", fsAddress); }})
                .environmentVariable("HADOOP_HOME", getHadoopHomeDir()).environmentVariable("HADOOP_HEAPSIZE_MAX", "1g")
                .dockerImageName("redit/hadoop:1.0").dockerFileAddress("docker/Dockerfile", true)
                .libraryPath(getHadoopHomeDir() + "/share/hadoop/**/*.jar")
                .libraryPath(getHadoopHomeDir() + "/share/hadoop/**/*.java")
                .logDirectory(getHadoopHomeDir() + "/logs").serviceType(ServiceType.JAVA).and();

        addRuntimeLibsToDeployment(builder, getHadoopHomeDir());

        builder.withService("nn", "hadoop-base").tcpPort(NN_HTTP_PORT, NN_RPC_PORT)
                .initCommand(getHadoopHomeDir() + "/bin/hdfs namenode -bootstrapStandby")
                .startCommand(getHadoopHomeDir() + "/bin/hdfs --daemon start namenode")
                .stopCommand(getHadoopHomeDir() + "/bin/hdfs --daemon stop namenode").and()
                .nodeInstances(numOfNNs, "nn", "nn", true)
                .withService("dn", "hadoop-base")
                .startCommand(getHadoopHomeDir() + "/bin/hdfs --daemon start datanode")
                .stopCommand(getHadoopHomeDir() + "/bin/hdfs --daemon stop datanode").and()
                .nodeInstances(numOfDNs, "dn", "dn", true)
                .withService("jn", "hadoop-base")
                .startCommand(getHadoopHomeDir() + "/bin/hdfs --daemon start journalnode")
                .stopCommand(getHadoopHomeDir() + "/bin/hdfs --daemon stop journalnode").and()
                .nodeInstances(numOfJNs, "jn", "jn", false);

        builder.node("nn1").initCommand(getHadoopHomeDir() + "/bin/hdfs namenode -format").and();

        addInstrumentablePath(builder, "/share/hadoop/hdfs/hadoop-hdfs-3.1.2.jar");

        return builder.build();
    }

    //Add the runtime library to the deployment
    private static void addRuntimeLibsToDeployment(Deployment.Builder builder, String hadoopHome) {
        for (String cpItem: System.getProperty("java.class.path").split(":")) {
            if (cpItem.contains("aspectjrt") || cpItem.contains("reditrt")) {
                String fileName = new File(cpItem).getName();
                logger.info("addRuntimeLibsToDeployment: " + hadoopHome + "/share/hadoop/common/" + fileName);
                builder.service("hadoop-base")
                        .applicationPath(cpItem, hadoopHome + "/share/hadoop/common/" + fileName, PathAttr.LIBRARY).and();
            }
        }
    }

    public static void addInstrumentablePath(Deployment.Builder builder, String path) {
        String[] services = {"hadoop-base", "nn", "dn", "jn"};
        for (String service: services) {
            builder.service(service).instrumentablePath(getHadoopHomeDir() + path).and();
        }
    }

    public static void startNodesInOrder(ReditRunner runner) throws RuntimeEngineException {
        try {

            if (numOfNNs > 1) {
                // wait for journal nodes to come up
                Thread.sleep(10000);
            }

            runner.runtime().startNode("nn1");
            Thread.sleep(10000);

            if (numOfNNs > 1) {
                for (int nnIndex=2; nnIndex<=numOfNNs; nnIndex++) {
                    runner.runtime().startNode("nn" + nnIndex);
                }
            }

            for (String node : runner.runtime().nodeNames())
                if (node.startsWith("dn")) runner.runtime().startNode(node);
        } catch (InterruptedException e) {
            logger.warn("startNodesInOrder sleep got interrupted");
        }

    }

    private static String getNNString() {
        StringJoiner stringJoiner = new StringJoiner(",");
        for (int i=1; i<=numOfNNs; i++) {
            stringJoiner.add("nn" + i);
        }
        return stringJoiner.toString();
    }

    private static String getNNAddresses() {
        String addrTemplate =
                "    <property>\n" +
                        "        <name>dfs.namenode.rpc-address.mycluster.{{NAME}}</name>\n" +
                        "        <value>{{NAME}}:" + NN_RPC_PORT + "</value>\n" +
                        "    </property>\n" +
                        "    <property>\n" +
                        "        <name>dfs.namenode.http-address.mycluster.{{NAME}}</name>\n" +
                        "        <value>{{NAME}}:" + NN_HTTP_PORT + "</value>\n" +
                        "    </property>";

        String retStr = "";
        for (int i=1; i<=numOfNNs; i++) {
            retStr += addrTemplate.replace("{{NAME}}", "nn" + i);
        }
        return retStr;
    }

    public static void waitActive() throws RuntimeEngineException {

        for (int index=1; index<= numOfNNs; index++) {
            for (int retry=3; retry>0; retry--){
                logger.info("Checking if NN nn{} is UP (retries left {})", index, retry-1);
                if (assertNNisUpAndReceivingReport(index, numOfDNs))
                    break;

                if (retry > 1) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        logger.warn("waitActive sleep got interrupted");
                    }
                } else {
                    throw new RuntimeException("NN nn" + index + " is not active or not receiving reports from DNs");
                }
            }
        }
        logger.info("The cluster is ACTIVE");
    }

    public static boolean assertNNisUpAndReceivingReport(int index, int numOfDNs) throws RuntimeEngineException {
        if (!isNNUp(index))
            return false;

        String res = getNNJmxHaInfo(index);
        if (res == null) {
            logger.warn("Error while trying to get the status of name node");
            return false;
        }

        logger.info("NN {} is up. Checking datanode connections", "nn" + index);
        return res.contains("\"NumLiveDataNodes\" : " + numOfDNs);
    }

    public static boolean isNNUp(int index) throws RuntimeEngineException {
        String res = getNNJmxHaInfo(index);
        if (res == null) {
            logger.warn("Error while trying to get the status of name node");
            return false;
        }

        return res.contains("\"tag.HAState\" : \"active\"") || res.contains("\"tag.HAState\" : \"standby\"");
    }

    private static String getNNJmxHaInfo(int index) {
        OkHttpClient client = new OkHttpClient();
        try {
            return client.newCall(new Request.Builder()
                    .url("http://" + SampleTest.runner.runtime().ip("nn" + index) + ":" + NN_HTTP_PORT +
                            "/jmx?qry=Hadoop:service=NameNode,name=FSNamesystem")
                    .build()).execute().body().string();
        } catch (IOException e) {
            logger.warn("Error while trying to get the status of name node");
            return null;
        }
    }

    public static void transitionToActive(int nnNum, ReditRunner runner) throws RuntimeEngineException {
        logger.info("Transitioning nn{} to ACTIVE", nnNum);
        CommandResults res = runner.runtime().runCommandInNode("nn" + nnNum, getHadoopHomeDir() + "/bin/hdfs haadmin -transitionToActive nn" + nnNum);
        if (res.exitCode() != 0) {
            throw new RuntimeException("Error while transitioning nn" + nnNum + " to ACTIVE.\n" + res.stdErr());
        }
    }

    public static void checkNNs(ReditRunner runner) throws RuntimeEngineException {
        logger.info("start check NNs !!!");
        for(int nnNum = 1; nnNum <= numOfNNs; nnNum++){
            CommandResults res = runner.runtime().runCommandInNode("nn" + nnNum, getHadoopHomeDir() + "/bin/hdfs haadmin -getServiceState nn" + nnNum);
            logger.info("nn" + nnNum + " status: " + res.stdOut());
        }
    }

}


