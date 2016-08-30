package com.enow.storm.TriggerTopology;

import java.util.Map;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import java.util.List;

public class StagingBolt extends BaseRichBolt {
    protected static final Logger LOG = LoggerFactory.getLogger(CallingKafkaBolt.class);
    private OutputCollector collector;
    private TopicStructure ts;
    boolean machineIdCheck = false;
    boolean phaseRoadMapIdCheck = false;

    @Override

    public void prepare(Map MongoConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        ts = new TopicStructure();
    }

    @Override
    public void execute(Tuple input) {

        if (null == input.getValueByField("topicStucture")) {
            return;
        } else if ((null == input.getStringByField("msg") || input.getStringByField("msg").length() == 0)) {
            return;
        }

        ts = (TopicStructure) input.getValueByField("topicStucture");
        final String msg = input.getStringByField("msg");

        MongoClient mongoClient = new MongoClient("52.193.56.228", 9092);

        mongoClient.setWriteConcern(WriteConcern.ACKNOWLEDGED);
        MongoDatabase dbWrite = mongoClient.getDatabase("enow");
        MongoCollection<Document> deviceListCollection = dbWrite.getCollection("device");

        if (deviceListCollection.count(new Document("deviceId", ts.getDeviceId())) == 0) {
            machineIdCheck = false;
        } else if (deviceListCollection.count(new Document("deviceId", ts.getDeviceId())) == 1) {
            machineIdCheck = true;
        } else {
//			machineIdCheck = "device id : now we have a problem";
            LOG.debug("There are more than two machine ID on MongoDB");
        }

        MongoCollection<Document> phaseRoadMapCollection = dbWrite.getCollection("phaseRoadMap");

        if (phaseRoadMapCollection.count(new Document("phaseRoadMapId", ts.getPhaseRoadMapId())) == 0) {
            phaseRoadMapIdCheck = false;
        } else if (phaseRoadMapCollection.count(new Document("phaseRoadMapId", ts.getPhaseRoadMapId())) == 1) {
            phaseRoadMapIdCheck = true;
        } else {
//			phaseRoadMapIdCheck = "phase road map id : now we have a problem";
            LOG.debug("There are more than two Phase Roadmap Id on MongoDB");
        }
        collector.emit(new Values(ts, msg, machineIdCheck, phaseRoadMapIdCheck));

        try {
            LOG.debug("input = [" + input + "]");
            collector.ack(input);
        } catch (Exception e) {
            collector.fail(input);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("topicStucture", "msg", "machineIdCheck", "phaseRoadMapIdCheck"));
    }
}