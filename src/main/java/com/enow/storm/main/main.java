package com.enow.storm.main;

/**
 * Created by writtic on 2016. 8. 30..
 */

import com.enow.storm.ActionTopology.*;
import com.enow.storm.TriggerTopology.*;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.kafka.*;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.topology.TopologyBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.UUID;

public class main {
    private static final String[] TOPICS = new String[]{"event", "proceed", "order", "trigger", "status"};
    private static final String zkhost = "192.168.99.100:2181";
    private LocalCluster cluster = new LocalCluster();
    public static void main(String[] args) throws Exception {
        new main().runMain(args);
    }

    protected void runMain(String[] args) throws Exception {


        if (args.length == 0) {
            submitTopologyLocalCluster("action", getActionTopology(), getConfig());
            submitTopologyLocalCluster("trigger", getTriggerTopology(), getConfig());
        } else {
            submitTopologyRemoteCluster(args[0], getTriggerTopology(), getConfig());
            submitTopologyRemoteCluster(args[1], getActionTopology(), getConfig());
        }

    }

    protected void submitTopologyLocalCluster(String name, StormTopology topology, Config config) throws InterruptedException {
        cluster.submitTopology(name, config, topology);
        stopWaitingForInput();
    }

    protected void submitTopologyRemoteCluster(String arg, StormTopology topology, Config config) throws Exception {
        StormSubmitter.submitTopology(arg, config, topology);
    }

    protected void stopWaitingForInput() {
        try {
            System.out.println("PRESS ENTER TO STOP");
            new BufferedReader(new InputStreamReader(System.in)).readLine();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected Config getConfig() {
        Config config = new Config();
        config.setDebug(true);
        config.setNumWorkers(2);
        return config;
    }

    protected StormTopology getTriggerTopology() {
        BrokerHosts hosts = new ZkHosts(zkhost);
        TopologyBuilder builder = new TopologyBuilder();
        // event spouts setting
        SpoutConfig eventConfig = new SpoutConfig(hosts, TOPICS[0], "/" + TOPICS[0], UUID.randomUUID().toString());
        eventConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
        eventConfig.startOffsetTime = kafka.api.OffsetRequest.LatestTime();
        eventConfig.ignoreZkOffsets = true;
        // proceed spouts setting
        SpoutConfig proceedConfig = new SpoutConfig(hosts, TOPICS[1], "/" + TOPICS[1], UUID.randomUUID().toString());
        proceedConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
        proceedConfig.startOffsetTime = kafka.api.OffsetRequest.LatestTime();
        proceedConfig.ignoreZkOffsets = true;
        // order spouts setting
        SpoutConfig orderConfig = new SpoutConfig(hosts, TOPICS[2], "/" + TOPICS[2], UUID.randomUUID().toString());
        orderConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
        orderConfig.startOffsetTime = kafka.api.OffsetRequest.LatestTime();
        orderConfig.ignoreZkOffsets = true;
//         kafka.api.OffsetRequest.LatestTime()
        // Set spouts
        builder.setSpout("event-spout", new KafkaSpout(eventConfig));
        builder.setSpout("proceed-spout", new KafkaSpout(proceedConfig));
        builder.setSpout("order-spout", new KafkaSpout(orderConfig));
        // Set bolts
        builder.setBolt("indexing-bolt", new IndexingBolt()).shuffleGrouping("event-spout")
                .shuffleGrouping("proceed-spout")
                .shuffleGrouping("order-spout");
        builder.setBolt("staging-bolt", new StagingBolt()).shuffleGrouping("indexing-bolt");
        builder.setBolt("calling-trigger-bolt", new CallingTriggerBolt()).shuffleGrouping("staging-bolt");
        return builder.createTopology();
    }
    protected StormTopology getActionTopology() {
        BrokerHosts hosts = new ZkHosts(zkhost);
        TopologyBuilder builder = new TopologyBuilder();
        // trigger spouts setting
        SpoutConfig triggerConfig = new SpoutConfig(hosts, TOPICS[3], "/" + TOPICS[3], UUID.randomUUID().toString());
        triggerConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
        triggerConfig.startOffsetTime = kafka.api.OffsetRequest.LatestTime();
        triggerConfig.ignoreZkOffsets = true;
        // status spouts setting
        SpoutConfig statusConfig = new SpoutConfig(hosts, TOPICS[4], "/" + TOPICS[4], UUID.randomUUID().toString());
        statusConfig.scheme = new SchemeAsMultiScheme(new StringScheme());
        statusConfig.startOffsetTime = kafka.api.OffsetRequest.LatestTime();
        statusConfig.ignoreZkOffsets = true;
        // Set spouts
        builder.setSpout("trigger-spout", new KafkaSpout(triggerConfig));
        builder.setSpout("status-spout", new KafkaSpout(statusConfig));
        // Set bolts
        builder.setBolt("scheduling-bolt", new SchedulingBolt())
                .shuffleGrouping("trigger-spout");
        builder.setBolt("status-bolt", new StatusBolt(), 4)
                .shuffleGrouping("status-spout");
        builder.setBolt("execute-code-bolt", new ExecutingBolt()).shuffleGrouping("scheduling-bolt");
        builder.setBolt("provisioning-bolt", new ProvisioningBolt()).shuffleGrouping("execute-code-bolt");
        builder.setBolt("calling-feed-bolt", new CallingFeedBolt()).shuffleGrouping("provisioning-bolt");
        return builder.createTopology();
    }
}
