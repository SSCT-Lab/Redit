package io.redit.samples.hdfs14852;

import org.apache.hadoop.hdfs.server.blockmanagement.BlockInfo;
import org.apache.hadoop.hdfs.server.blockmanagement.BlockInfoStriped;
import org.apache.hadoop.hdfs.server.namenode.NameNode;
import org.apache.hadoop.hdfs.util.LightWeightLinkedSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;

public class LowRedundancyBlocks implements Iterable<BlockInfo> {
    static final int LEVEL = 5;
    static final int QUEUE_HIGHEST_PRIORITY = 0;
    static final int QUEUE_VERY_LOW_REDUNDANCY = 1;
    static final int QUEUE_LOW_REDUNDANCY = 2;
    static final int QUEUE_REPLICAS_BADLY_DISTRIBUTED = 3;
    static final int QUEUE_WITH_CORRUPT_BLOCKS = 4;
    private final List<LightWeightLinkedSet<BlockInfo>> priorityQueues = new ArrayList(5);
    private final LongAdder lowRedundancyBlocks = new LongAdder();
    private final LongAdder corruptBlocks = new LongAdder();
    private final LongAdder corruptReplicationOneBlocks = new LongAdder();
    private final LongAdder lowRedundancyECBlockGroups = new LongAdder();
    private final LongAdder corruptECBlockGroups = new LongAdder();
    private final LongAdder highestPriorityLowRedundancyReplicatedBlocks = new LongAdder();
    private final LongAdder highestPriorityLowRedundancyECBlocks = new LongAdder();

    @Override
    public synchronized Iterator<BlockInfo> iterator() {
        final Iterator<LightWeightLinkedSet<BlockInfo>> q = this.priorityQueues.iterator();
        return new Iterator<BlockInfo>() {
            private Iterator<BlockInfo> b = ((LightWeightLinkedSet)q.next()).iterator();

            public BlockInfo next() {
                this.hasNext();
                return (BlockInfo)this.b.next();
            }

            public boolean hasNext() {
                while(!this.b.hasNext() && q.hasNext()) {
                    this.b = ((LightWeightLinkedSet)q.next()).iterator();
                }

                return this.b.hasNext();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    synchronized boolean add(BlockInfo block, int curReplicas, int readOnlyReplicas, int outOfServiceReplicas, int expectedReplicas) {
        int priLevel = this.getPriority(block, curReplicas, readOnlyReplicas, outOfServiceReplicas, expectedReplicas);
        if (this.add(block, priLevel, expectedReplicas)) {
            NameNode.blockStateChangeLog.debug("BLOCK* NameSystem.LowRedundancyBlock.add: {} has only {} replicas and need {} replicas so is added to neededReconstructions at priority level {}", new Object[]{block, curReplicas, expectedReplicas, priLevel});
            return true;
        } else {
            return false;
        }
    }

    private boolean add(BlockInfo blockInfo, int priLevel, int expectedReplicas) {
        if (((LightWeightLinkedSet)this.priorityQueues.get(priLevel)).add(blockInfo)) {
            this.incrementBlockStat(blockInfo, priLevel, expectedReplicas);
            return true;
        } else {
            return false;
        }
    }

    boolean remove(BlockInfo block, int priLevel) {
        return this.remove(block, priLevel, block.getReplication());
    }

    boolean remove(BlockInfo block, int priLevel, int oldExpectedReplicas) {
        if (priLevel >= 0 && priLevel < 5 && ((LightWeightLinkedSet)this.priorityQueues.get(priLevel)).remove(block)) {
            NameNode.blockStateChangeLog.debug("BLOCK* NameSystem.LowRedundancyBlock.remove: Removing block {} from priority queue {}", block, priLevel);
            this.decrementBlockStat(block, priLevel, oldExpectedReplicas);
            return true;
        } else {
            for(int i = 0; i < 5; ++i) {
                if (i != priLevel && ((LightWeightLinkedSet)this.priorityQueues.get(i)).remove(block)) {
                    NameNode.blockStateChangeLog.debug("BLOCK* NameSystem.LowRedundancyBlock.remove: Removing block {} from priority queue {}", block, i);
                    this.decrementBlockStat(block, i, oldExpectedReplicas);
                    return true;
                }
            }

            return false;
        }
    }

    private void decrementBlockStat(BlockInfo blockInfo, int priLevel, int oldExpectedReplicas) {
        if (blockInfo.isStriped()) {
            this.lowRedundancyECBlockGroups.decrement();
            if (priLevel == 4) {
                this.corruptECBlockGroups.decrement();
            }

            if (priLevel == 0) {
                this.highestPriorityLowRedundancyECBlocks.decrement();
            }
        } else {
            this.lowRedundancyBlocks.decrement();
            if (priLevel == 4) {
                this.corruptBlocks.decrement();
                if (oldExpectedReplicas == 1) {
                    this.corruptReplicationOneBlocks.decrement();

                    assert this.corruptReplicationOneBlocks.longValue() >= 0L : "Number of corrupt blocks with replication factor 1 should be non-negative";
                }
            }

            if (priLevel == 0) {
                this.highestPriorityLowRedundancyReplicatedBlocks.decrement();
            }
        }

    }

    private void incrementBlockStat(BlockInfo blockInfo, int priLevel, int expectedReplicas) {
        if (blockInfo.isStriped()) {
            this.lowRedundancyECBlockGroups.increment();
            if (priLevel == 4) {
                this.corruptECBlockGroups.increment();
            }

            if (priLevel == 0) {
                this.highestPriorityLowRedundancyECBlocks.increment();
            }
        } else {
            this.lowRedundancyBlocks.increment();
            if (priLevel == 4) {
                this.corruptBlocks.increment();
                if (expectedReplicas == 1) {
                    this.corruptReplicationOneBlocks.increment();
                }
            }

            if (priLevel == 0) {
                this.highestPriorityLowRedundancyReplicatedBlocks.increment();
            }
        }

    }

    private int getPriority(BlockInfo block, int curReplicas, int readOnlyReplicas, int outOfServiceReplicas, int expectedReplicas) {
        assert curReplicas >= 0 : "Negative replicas!";

        if (curReplicas >= expectedReplicas) {
            return 3;
        } else if (block.isStriped()) {
            BlockInfoStriped sblk = (BlockInfoStriped)block;
            return this.getPriorityStriped(curReplicas, outOfServiceReplicas, sblk.getRealDataBlockNum(), sblk.getParityBlockNum());
        } else {
            return this.getPriorityContiguous(curReplicas, readOnlyReplicas, outOfServiceReplicas, expectedReplicas);
        }
    }

    private int getPriorityContiguous(int curReplicas, int readOnlyReplicas, int outOfServiceReplicas, int expectedReplicas) {
        if (curReplicas == 0) {
            if (outOfServiceReplicas > 0) {
                return 0;
            } else {
                return readOnlyReplicas > 0 ? 0 : 4;
            }
        } else if (curReplicas == 1) {
            return 0;
        } else {
            return curReplicas * 3 < expectedReplicas ? 1 : 2;
        }
    }

    private int getPriorityStriped(int curReplicas, int outOfServiceReplicas, short dataBlkNum, short parityBlkNum) {
        if (curReplicas < dataBlkNum) {
            return curReplicas + outOfServiceReplicas >= dataBlkNum ? 0 : 4;
        } else if (curReplicas == dataBlkNum) {
            return 0;
        } else {
            return (curReplicas - dataBlkNum) * 3 < parityBlkNum + 1 ? 1 : 2;
        }
    }

    synchronized boolean contains(BlockInfo block) {
        Iterator var2 = this.priorityQueues.iterator();

        LightWeightLinkedSet set;
        do {
            if (!var2.hasNext()) {
                return false;
            }

            set = (LightWeightLinkedSet)var2.next();
        } while(!set.contains(block));

        return true;
    }

}
