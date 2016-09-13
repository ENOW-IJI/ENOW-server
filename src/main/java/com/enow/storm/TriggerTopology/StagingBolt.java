package com.enow.storm.TriggerTopology;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.bson.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.enow.storm.Connect;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class StagingBolt extends BaseRichBolt {
	protected static final Logger LOG = LogManager.getLogger(StagingBolt2.class);
	private OutputCollector collector;

	@Override

	public void prepare(Map MongoConf, TopologyContext context, OutputCollector collector) {
		this.collector = collector;
	}

	@Override
	public void execute(Tuple input) {
		JSONObject _jsonObject = null;
		boolean mapIdCheck = false;
		FindIterable<Document> iterable;
		JSONParser jsonParser = new JSONParser();
		JSONObject roadMapId;
		JSONObject mapIds;
		JSONObject mapId;
		JSONObject phaseId;
		JSONObject devices;
		JSONObject outingNode;
		JSONObject incomingNode;
		JSONObject subsequentInitPeer;
		JSONArray deviceId;
		JSONArray incomingNodeArray;
		JSONArray subsequentInitPeerPhase;
		JSONArray outingNodeArray;
		JSONArray incomingPeerArray;
		JSONArray initNode;
		JSONParser parser = new JSONParser();

		ArrayList<JSONObject> _jsonArray = new ArrayList<JSONObject>();
		ConcurrentHashMap<String, JSONObject> ackSchdueling = new ConcurrentHashMap<>();
		MongoClient mongoClient = new MongoClient("127.0.0.1", 27017);
		mongoClient.setWriteConcern(WriteConcern.ACKNOWLEDGED);

		_jsonObject = (JSONObject) input.getValueByField("jsonObject");

		_jsonObject.put("topic",
				_jsonObject.get("serverId") + "/" + _jsonObject.get("brokerId") + "/" + _jsonObject.get("deviceId"));

		_jsonObject.remove("serverId");
		_jsonObject.remove("brokerId");
		_jsonObject.remove("deviceId");

		MongoDatabase dbWrite = mongoClient.getDatabase("enow");
		MongoCollection<Document> roadMapCollection = dbWrite.getCollection("roadMap");

		iterable = roadMapCollection.find(new Document("roadMapId", (String) _jsonObject.get("roadMapId")));

		if ((boolean) _jsonObject.get("init")) {
			try {
				roadMapId = (JSONObject) jsonParser.parse(iterable.first().toJson());

				mapIds = (JSONObject) roadMapId.get("mapIds");
				initNode = (JSONArray) roadMapId.get("initNode");
				incomingNode = (JSONObject) roadMapId.get("incomingNode");
				outingNode = (JSONObject) roadMapId.get("outingNode");

				String jsonString = _jsonObject.toJSONString();

				for (int i = 0; i < initNode.size(); i++) {
					String InitMapId = (String) initNode.get(i);

					mapId = (JSONObject) mapIds.get(InitMapId);

					JSONObject tmpJsonObject = new JSONObject();

					tmpJsonObject = (JSONObject) parser.parse(jsonString);
					tmpJsonObject.put("payload", null);
					tmpJsonObject.put("previousData", null);
					tmpJsonObject.put("proceed", false);

					if (outingNode.containsKey(InitMapId)) {
						outingNodeArray = (JSONArray) outingNode.get(InitMapId);

						tmpJsonObject.put("outingNode", outingNodeArray);
					} else {
						tmpJsonObject.put("outingNode", null);
					}

					if (incomingNode.containsKey(InitMapId)) {
						incomingNodeArray = (JSONArray) incomingNode.get(InitMapId);

						tmpJsonObject.put("incomingNode", incomingNodeArray);
					} else {
						tmpJsonObject.put("incomingNode", null);
					}

					tmpJsonObject.put("init", false);
					_jsonArray.add(tmpJsonObject);
				}
			} catch (ParseException e) {
				// iterable.first().toJson() 이 json형식의 string이 아닌 경우
				// 발생 하지만 tojson이기에 그럴 일이 발생하지 않을 것이라 가정
				e.printStackTrace();
				return;

			}
		} else {
			try {
				roadMapId = (JSONObject) jsonParser.parse(iterable.first().toJson());

				mapIds = (JSONObject) roadMapId.get("mapIds");
				initNode = (JSONArray) roadMapId.get("initNode");
				incomingNode = (JSONObject) roadMapId.get("incomingNode");
				outingNode = (JSONObject) roadMapId.get("outingNode");

				String jsonString = _jsonObject.toJSONString();

				mapId = (JSONObject) _jsonObject.get("mapId");

				_jsonObject.put("proceed", false);

				if (outingNode.containsKey(_jsonObject.get("mapId"))) {
					outingNodeArray = (JSONArray) outingNode.get(_jsonObject.get("mapId"));

					_jsonObject.put("outingNode", outingNodeArray);
				} else {
					_jsonObject.put("outingNode", null);
				}

				if (incomingNode.containsKey(_jsonObject.get("mapId"))) {
					incomingNodeArray = (JSONArray) incomingNode.get(_jsonObject.get("mapId"));

					_jsonObject.put("incomingNode", incomingNodeArray);
				} else {
					_jsonObject.put("incomingNode", null);
				}
				
				_jsonArray.add(_jsonObject);
			} catch (ParseException e) {
				// iterable.first().toJson() 이 json형식의 string이 아닌 경우
				// 발생 하지만 tojson이기에 그럴 일이 발생하지 않을 것이라 가정
				e.printStackTrace();
				return;

			}
		}

		collector.emit(new Values(_jsonArray));

		try {
			LOG.debug("input = [" + input + "]");
			collector.ack(input);
		} catch (Exception e) {
			collector.fail(input);
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("jsonArray"));
	}
}
