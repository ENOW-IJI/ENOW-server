package com.enow.storm.ActionTopology;

import com.enow.dto.TopicStructure;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

public class SchedulingBolt extends BaseRichBolt {
    protected static final Logger _LOG = LoggerFactory.getLogger(CallingFeedBolt.class);
    ConcurrentHashMap<String, TopicStructure> _executedNode = new ConcurrentHashMap<String, TopicStructure>();
    private OutputCollector _collector;
    private TopicStructure _topicStructure;

    @Override
    public void prepare(Map MongoConf, TopologyContext context, OutputCollector collector) {
        _collector = collector;
        _topicStructure = new TopicStructure();
    }

    @Override
    public void execute(Tuple input) {
        if ((null == input.toString()) || (input.toString().length() == 0)) {
            return;
        }

        String temp = input.getValues().toString().substring(1, input.getValues().toString().length() - 1);
        System.out.println(temp);

        if ((null == temp) || (temp.length() == 0)) {
            _LOG.warn("input value or length of input is empty : [" + input + "]\n");
            return;
        }

        String[] elements = new String[3];
        String[] topics = new String[7];
        StringTokenizer tokenizer;
        tokenizer = new StringTokenizer(temp, ",");
        for (int index = 0; tokenizer.hasMoreTokens(); index++) {
            elements[index] = tokenizer.nextToken().toString();
            System.out.println("elements[" + index + "]: " + elements[index]);
        }
        tokenizer = new StringTokenizer(elements[1], "/");
        for (int index = 0; tokenizer.hasMoreTokens(); index++) {
            topics[index] = tokenizer.nextToken().toString();
            System.out.println("topics[" + index + "]: " + topics[index]);
        }
        String currentMapId = topics[6];
        _topicStructure.setCorporationName(topics[0]);
        _topicStructure.setServerId(topics[1]);
        _topicStructure.setBrokerId(topics[2]);
        _topicStructure.setDeviceId(topics[3]);
        _topicStructure.setPhaseRoadMapId(topics[4]);
        _topicStructure.setPhaseId(topics[5]);
        _topicStructure.setCurrentMapId(currentMapId);
        _topicStructure.setCurrentMsg(elements[2]);
        String _msgId = currentMapId;
        boolean check = false;
        if (elements[0].equals("trigger")) {
            if (!this._executedNode.containsKey(_msgId)) {
                this._executedNode.put(_msgId, _topicStructure);
                check = true;
            }
            // Data handling part
            _collector.emit(new Values(elements[0], _topicStructure, check));
            try {
                _collector.ack(input);
                _LOG.info("Try to insert input to Hashmap = [" + temp + "]\n");
                System.out.println("Succeed in storing " + temp + " to ConcurrentHashMap");
            } catch (Exception e) {
                _collector.fail(input);
                _LOG.warn("Fail in inserting input to Hashmap = [" + temp + "]\n");
                System.out.println("Fail in storing " + temp + " to ConcurrentHashMap");
            }
        } else if (elements[0].equals("status")) {
            if (this._executedNode.containsKey(_msgId)) {
                check = true;
            }
            // Data handling part

            _collector.emit(new Values(elements[0], _topicStructure, check));
            try {
                this._executedNode.remove(_msgId);
                _collector.ack(input);
                _LOG.info("Try to send input to ProvisioningBolt = [" + temp + "]\n");
                System.out.println("Succeed in sending and deleting " + temp + " in ConcurrentHashMap");
            } catch (Exception e) {
                _collector.fail(input);
                _LOG.warn("Fail in sending input to ProvisioningBolt = [" + temp + "]\n");
                System.out.println("Fail in sending and deleting " + temp + " to ConcurrentHashMap");
            }
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("spoutSource", "topicStructure", "check"));
    }
}
