package com.enow.storm.ActionTopology;

import com.enow.persistence.redis.IRedisDB;
import com.enow.persistence.redis.RedisDB;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Created by writtic on 2016. 9. 13..
 */
public class StatusBolt extends BaseRichBolt {
    protected static final Logger _LOG = LoggerFactory.getLogger(StatusBolt.class);
    private OutputCollector _collector;
    private JSONParser _parser;
    private IRedisDB _redis;

    @Override
    public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
        String redisIp = (String) conf.get("redis.ip");
        Long lredisPort = (Long) conf.get("redis.port");
        int  redisPort = lredisPort.intValue();
        _redis = RedisDB.getInstance(redisIp, redisPort);
        _parser = new JSONParser();
        _collector = collector;
    }

    @Override
    public void execute(Tuple input) {

        JSONObject _jsonObject;

        if ((null == input.toString()) || (input.toString().length() == 0)) {
        	_LOG.warn("error:1");
            return;
        }

        String jsonString = input.getValues().toString().substring(1, input.getValues().toString().length() - 1);
        try {
            _jsonObject = (JSONObject) _parser.parse(jsonString);
            _LOG.debug("Succeed in inserting messages to _jsonObject : \n" + _jsonObject.toJSONString());
            // Save the payload came from connected devices.
            _redis.addStatus(_redis.jsonObjectToStatus(_jsonObject));
        } catch (ParseException e1) {
            e1.printStackTrace();
            _LOG.warn("error:2");
            return;
        }

        // Go to next bolt
        _collector.emit(new Values(input));
        try {
            //_LOG.info("topic:" + _jsonObject.get("topic"));
            _collector.ack(input);
        } catch (Exception e) {
        	_LOG.warn("ack failed");
            _collector.fail(input);
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    	
    	
    }
}