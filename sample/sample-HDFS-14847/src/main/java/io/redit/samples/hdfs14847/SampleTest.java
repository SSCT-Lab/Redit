package io.redit.samples.hdfs14847;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import io.redit.ReditRunner;
import io.redit.exceptions.RuntimeEngineException;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.client.HdfsDataInputStream;
import org.apache.hadoop.hdfs.protocol.*;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockInfoStriped;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockManager;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeDescriptor;
import org.apache.hadoop.hdfs.server.blockmanagement.DatanodeStorageInfo;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.util.Time;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

public class SampleTest {
    private static final Logger logger = LoggerFactory.getLogger(SampleTest.class);
    protected static ReditRunner runner;
    private static final String policy = "XOR-2-1-1024k";
    private static final String ecFilePath = "/test_ec";
    private static final String testFile = "aa.txt";
    private static final String data = "hello hadoop hello hdfs";
    private static DistributedFileSystem dfs = null;
    private static NameNode nn1 = null;
    private BlockQueue<BlockTargetPair> replicateBlocks;

    @BeforeClass
    public static void before() throws RuntimeEngineException, ParserConfigurationException, IOException, SAXException, TransformerException, InterruptedException {

        runner = ReditRunner.run(ReditHelper.getDeployment());
        ReditHelper.startNodesInOrder(runner);
        ReditHelper.waitActive();
        logger.info("The cluster is UP!");

        ReditHelper.transitionToActive(1, runner);
        ReditHelper.checkNNs(runner);
        dfs = ReditHelper.getDFS(runner);
        nn1 = ReditHelper.getNN1(runner);
    }

    @AfterClass
    public static void after() {
        if (runner != null) {
            runner.stop();
        }
    }


    @Test
    public void sampleTest() throws IOException, RuntimeEngineException, TimeoutException, InterruptedException {

        dfs.enableErasureCodingPolicy(policy);
        dfs.mkdirs(new Path(ecFilePath));
        Path ecPath = new Path(ecFilePath);
        dfs.setErasureCodingPolicy(ecPath, policy);

        int cellSize = dfs.getErasureCodingPolicy(ecPath).getCellSize();
        final BlockManager bm = nn1.getNamesystem().getBlockManager();

        // 写ec文件.
        runner.runtime().runCommandInNode("nn1", "touch " + testFile + " && echo \"" + data + "\" >> " + testFile);
        runner.runtime().runCommandInNode("nn1", ReditHelper.getHadoopHomeDir() + "/bin/hdfs dfs -put " + testFile + " " + ecFilePath);

        // 获取ec block的2个节点，设置为decommission. 这2个节点不在DatanodeAdminManager的pendingNodes中.
        Path ecFile = new Path(ecFilePath, testFile);
        List<LocatedBlock> lbs = ((HdfsDataInputStream) dfs.open(ecFile)).getAllBlocks();
        LocatedStripedBlock blk = (LocatedStripedBlock) lbs.get(0);
        DatanodeInfo[] dnList = blk.getLocations();
        DatanodeDescriptor dn0 = bm.getDatanodeManager().getDatanode(dnList[0].getDatanodeUuid());
        dn0.startDecommission();
        DatanodeDescriptor dn1 = bm.getDatanodeManager().getDatanode(dnList[1].getDatanodeUuid());
        dn1.startDecommission();
        assertEquals(0, bm.getDatanodeManager().getDatanodeAdminManager().getNumPendingNodes());

//        // 将 dn0 块复制到另一个 dn 模拟 dn0 复制成功， dn1 复制失败.
//        final byte blockIndex = blk.getBlockIndices()[0];
//        final Block targetBlk = new Block(blk.getBlock().getBlockId() + blockIndex, cellSize, blk.getBlock().getGenerationStamp());
//        DatanodeInfo extraDn = getDatanodeOutOfTheBlock(blk);
//        DatanodeDescriptor target = bm.getDatanodeManager().getDatanode(extraDn.getDatanodeUuid());
//        dn0.addBlockToBeReplicated(targetBlk, new DatanodeStorageInfo[]{target.getStorageInfos()[0]});
//
//        // dn0 复制成功
//        waitFor(new Supplier<Boolean>() {
//            @Override
//            public Boolean get() {
//                return dn0.getNumberOfReplicateBlocks() == 0;
//            }
//        }, 100, 60000);
//
//        waitFor(new Supplier<Boolean>() {
//            @Override
//            public Boolean get() {
//                Iterator<DatanodeStorageInfo> it = bm.getStoredBlock(targetBlk).getStorageInfos();
//                while (it.hasNext()) {
//                    if (it.next().getDatanodeDescriptor().equals(target)) {
//                        return true;
//                    }
//                }
//                return false;
//            }
//        }, 100, 60000);
//
//        // 有 8 个实时副本
//        BlockInfoStriped blockInfo = (BlockInfoStriped)bm.getStoredBlock(new Block(blk.getBlock().getBlockId()));
//        assertEquals(8, bm.countNodes(blockInfo).liveReplicas());
//
//        // 添加2个节点到DatanodeAdminManager的pendingNodes
//        bm.getDatanodeManager().getDatanodeAdminManager().getPendingNodes().add(dn0);
//        bm.getDatanodeManager().getDatanodeAdminManager().getPendingNodes().add(dn1);
//        waitNodeState(dn0, DatanodeInfo.AdminStates.DECOMMISSIONED);
//        waitNodeState(dn1, DatanodeInfo.AdminStates.DECOMMISSIONED);
//
//        // 有 9 个实时副本
//        assertEquals(9, bm.countNodes(blockInfo).liveReplicas());
//
//        // dn0 & dn1退役后，所有内部Blocks(0~8)都在那里.
//        Iterator<DatanodeStorageInfo> it = blockInfo.getStorageInfos();
//        BitSet indexBitSet = new BitSet(9);
//        while(it.hasNext()) {
//            DatanodeStorageInfo storageInfo = it.next();
//            if(storageInfo.getDatanodeDescriptor().equals(dn0) || storageInfo.getDatanodeDescriptor().equals(dn1)) {
//                // 跳过退役节点
//                continue;
//            }
//            byte index = blockInfo.getStorageBlockIndex(storageInfo);
//            indexBitSet.set(index);
//        }
//        for (int i = 0; i < 9; ++i) {
//            assertEquals(true, indexBitSet.get(i));
//        }
    }

    /**
     * Get a Datanode which does not contain the block.
     */
    private DatanodeInfo getDatanodeOutOfTheBlock(LocatedStripedBlock blk) throws IOException {

        DatanodeInfo[] allDnInfos = dfs.getClient().datanodeReport(HdfsConstants.DatanodeReportType.LIVE);
        DatanodeInfo[] blkDnInos= blk.getLocations();
        for (DatanodeInfo dnInfo : allDnInfos) {
            boolean in = false;
            for (DatanodeInfo blkDnInfo : blkDnInos) {
                if (blkDnInfo.equals(dnInfo)) {
                    in = true;
                }
            }
            if(!in) {
                return dnInfo;
            }
        }
        return null;
    }

    public static void waitFor(Supplier<Boolean> check, int checkEveryMillis, int waitForMillis) throws TimeoutException, InterruptedException {
        Preconditions.checkNotNull(check, "Input supplier interface should be initailized");
        Preconditions.checkArgument(waitForMillis >= checkEveryMillis, "Total wait time should be greater than check interval time");
        long st = Time.monotonicNow();

        boolean result;
        for(result = (Boolean)check.get(); !result && Time.monotonicNow() - st < (long)waitForMillis; result = (Boolean)check.get()) {
            Thread.sleep((long)checkEveryMillis);
        }

        if (!result) {
//            throw new TimeoutException("Timed out waiting for condition. Thread diagnostics:\n" + TimedOutTestsListener.buildThreadDiagnosticString());
            throw new TimeoutException("Timed out waiting for condition.");
        }
    }

    public void addBlockToBeReplicated(Block block, DatanodeStorageInfo[] targets) {
        assert block != null && targets != null && targets.length > 0;

        this.replicateBlocks.offer(new BlockTargetPair(block, targets));
    }

    private static class BlockQueue<E> {
        private final Queue<E> blockq;

        private BlockQueue() {
            this.blockq = new LinkedList();
        }

        synchronized int size() {
            return this.blockq.size();
        }

        synchronized boolean offer(E e) {
            return this.blockq.offer(e);
        }

        synchronized List<E> poll(int numBlocks) {
            if (numBlocks > 0 && !this.blockq.isEmpty()) {
                ArrayList results;
                for(results = new ArrayList(); !this.blockq.isEmpty() && numBlocks > 0; --numBlocks) {
                    results.add(this.blockq.poll());
                }

                return results;
            } else {
                return null;
            }
        }

        synchronized boolean contains(E e) {
            return this.blockq.contains(e);
        }

        synchronized void clear() {
            this.blockq.clear();
        }
    }

    public static class BlockTargetPair {
        public final Block block;
        public final DatanodeStorageInfo[] targets;

        BlockTargetPair(Block block, DatanodeStorageInfo[] targets) {
            this.block = block;
            this.targets = targets;
        }
    }

    private void waitNodeState(DatanodeInfo node, DatanodeInfo.AdminStates state) {
        for(boolean done = state == node.getAdminState(); !done; done = state == node.getAdminState()) {
            logger.info("Waiting for node " + node + " to change state to " + state + " current state: " + node.getAdminState());

            try {
                Thread.sleep(500L);
            } catch (InterruptedException var5) {
            }
        }

        logger.info("node " + node + " reached the state " + state);
    }

}