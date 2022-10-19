.. Redit documentation master file, created by
   sphinx-quickstart on Mon Nov 8 11:07:10 2021.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

.. Replacements Definition


=======
Redit
=======

Introduction
============

|projectName| is a test framework for end-to-end testing of distributed systems. It can be used to deterministically inject
failures during a normal test case execution. Currently, node failure, network partition, network delay, network packet loss, and clock drift is supported. For a few supported languages, it is possible to enforce a specific order between
nodes in order to reproduce a specific time-sensitive scenario and inject failures before or after a specific method is
called when a specific stack trace is present.

Bugs Reproduced by Redit
============
We applied Redit to 19 versions of 8 widely-used cloud systems including Cassandra-3.11.6, HDFS-3.3.0, HDFS-3.1.2, ZK-3.7.0,
and HBASE-2.4.12. The following table shows the bugs reproduced by Redit in these systems.

====================== ========================================================
Bug ID                 Bug Description
====================== ========================================================
`HBASE-26114`_         HMaster cannot start normally
`HBASE-23682`_         HMaster cannot become active master
`HBASE-21487`_         Concurrent modify table ops lead to unexpected results
`CASSANDRA-16836`_     Materialized views incorrect quoting of UDF
`CASSANDRA-14365`_     Commit log replay failure
`CASSANDRA-13669`_     Error when starting cassandra
=============== =======================================================

.. _HBASE-26114:https://issues.apache.org/jira/browse/HBASE-26114
.. _HBASE-23682:https://issues.apache.org/jira/browse/HBASE-23682
.. _HBASE-21487:https://issues.apache.org/jira/browse/HBASE-21487
.. _CASSANDRA-16836:https://issues.apache.org/jira/browse/CASSANDRA-16836
.. _CASSANDRA-14365:https://issues.apache.org/jira/browse/CASSANDRA-14365
.. _CASSANDRA-13669:https://issues.apache.org/jira/browse/CASSANDRA-13669

.. toctree::
    :caption: Table of Contents
    :maxdepth: 1
    :glob:

    pages/prereq
    pages/quickstart
    pages/deterministic
    pages/runseq
    pages/newnode
    pages/jvmservice
    pages/docker
    pages/changelog