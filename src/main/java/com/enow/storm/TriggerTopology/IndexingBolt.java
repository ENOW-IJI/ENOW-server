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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.enow.storm.Connect;

public class IndexingBolt extends BaseRichBolt {
	protected static final Logger LOG = LogManager.getLogger(IndexingBolt.class);
	private OutputCollector collector;

	@Override

	public void prepare(Map MongoConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void execute(Tuple input) {
		JSONParser parser = new JSONParser();
		JSONObject _jsonObject;

		if (input.toString().length() == 0) {
			return;
		}

		String msg = input.getValues().toString().substring(1, input.getValues().toString().length() - 1);

		try {
			_jsonObject = (JSONObject) parser.parse(msg);

			if (_jsonObject.containsKey("init")) {

				if (_jsonObject.containsKey("ack") && _jsonObject.containsKey("proceed")
						&& _jsonObject.containsKey("corporationName") && _jsonObject.containsKey("serverId")
						&& _jsonObject.containsKey("brokerId") && _jsonObject.containsKey("deviceId")
						&& _jsonObject.containsKey("phaseRoadMapId")) {
				} else {
					// init = true 일 경우 필요한 값이 다 안 들어 왔다.
					return;
				}
				
				/*
				Event
				{
				    "corporationName":"enow",
				    "serverId":"serverId1",
				    "brokerId":"brokerId1",
				    "deviceId":"deviceId1",
				    "phaseRoadMapId":"1",
				    "phaseId":"phaseId1",
				    "mapId":1,
				    "procced":false,
				    "waitingPeer":["1", "2"],
				    "incomingPeer":null,
				    "outingPeer":["11", "13"],
				    "subsequentInitPeer":["15"],
				    "previousData":[{},{},{}],
				    "payload":[]
				}
				*/
				
			} else {
				if (_jsonObject.containsKey("ack") && _jsonObject.containsKey("proceed")
						&& _jsonObject.containsKey("corporationName") && _jsonObject.containsKey("serverId")
						&& _jsonObject.containsKey("brokerId") && _jsonObject.containsKey("deviceId")
						&& _jsonObject.containsKey("phaseRoadMapId") && _jsonObject.containsKey("phaseId")
						&& _jsonObject.containsKey("mapId") && _jsonObject.containsKey("message")
						&& _jsonObject.containsKey("waitingPeer") && _jsonObject.containsKey("outingPeer")
						&& _jsonObject.containsKey("subsequentInitPeer") && _jsonObject.containsKey("incomingPeer")
						&& _jsonObject.containsKey("previousData")) {
				} else {
					// init = false 일 경우 필요한 값이 다 안 들어 왔다.
					return;
				}
			}
		} catch (ParseException e1) {
			// JSONParseException 발
			e1.printStackTrace();
			return;
		}

		collector.emit(new Values(_jsonObject));
		try {
			LOG.debug("input = [" + input + "]");
			collector.ack(input);
		} catch (Exception e) {
			collector.fail(input);
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("jsonObject"));
	}
}