package com.enow.storm.ActionTopology;

import com.enow.persistence.dto.NodeDTO;
import com.enow.persistence.redis.IRedisDB;
import com.enow.persistence.redis.RedisDB;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;

public class ProvisioningBolt extends BaseRichBolt {
    protected static final Logger _LOG = LoggerFactory.getLogger(ProvisioningBolt.class);
    private IRedisDB _redis;
    private OutputCollector _collector;

    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        String redisIp = (String) conf.get("redis.ip");
        Long lredisPort = (Long) conf.get("redis.port");
        int  redisPort = lredisPort.intValue();
        _redis = RedisDB.getInstance(redisIp, redisPort);
        _collector = collector;
    }

    @Override
    public void execute(Tuple input) {
        org.apache.storm.shade.org.json.simple.JSONObject _jsonObject_shade = new org.apache.storm.shade.org.json.simple.JSONObject();
        _jsonObject_shade = (org.apache.storm.shade.org.json.simple.JSONObject) input.getValue(0);
        String _jsonObject_shade_string = (String) _jsonObject_shade.toJSONString();
        JSONParser parser = new JSONParser();
        JSONObject _jsonObject = new JSONObject();
		try {
			_jsonObject = (JSONObject) parser.parse(_jsonObject_shade_string);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        String _roadMapId = (String)input.getValue(1);
        Boolean verified = (Boolean) _jsonObject.get("verified");
        Boolean lastNode = (Boolean) _jsonObject.get("lastNode");
        // Confirm verification
        if (verified) {
            // Store this node for subsequent node
            String result = "nothing";
            try {
                NodeDTO dto = _redis.jsonObjectToNode(_jsonObject);
                result = _redis.addNode(dto);
                _LOG.debug("Succeed in inserting current node to Redis : " + result);
            } catch (Exception e) {
                e.printStackTrace();
                _LOG.debug("Fail in inserting current node to Redis : " + result);
            }
            // Check this node is the last node
            if(lastNode) {
                // When this node is the last node, delete the node that will not be used
                _redis.deleteLastNode(_redis.jsonObjectToNode(_jsonObject));
            }
        }
        // Go to next bolt
        _collector.emit(new Values(_jsonObject, _roadMapId));
        try {
            _collector.ack(input);
        } catch (Exception e) {
            _LOG.warn("ack failed");
            _collector.fail(input);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("jsonObject", "roadMapId"));
    }
}