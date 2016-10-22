package com.enow.storm.TriggerTopology;

import org.apache.kafka.clients.producer.*;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.enow.storm.Connect;
import com.esotericsoftware.minlog.Log;

import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

public class CallingTriggerBolt extends BaseRichBolt {
	protected static final Logger _LOG = LogManager.getLogger(CallingTriggerBolt.class);
	private OutputCollector collector;
	private Properties props;

	@Override

	public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
		String kafkaProperties = (String) conf.get("kafka.properties");
		this.collector = collector;
		
		//set kafka properties
		props = new Properties();
		props.put("producer.type", "sync");
		props.put("batch.size", "1");
		props.put("bootstrap.servers", kafkaProperties);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
	}

	@Override
	public void execute(Tuple input) {
		JSONObject _jsonError = new JSONObject();
		boolean errorCheck = false;
		
		Producer<String, String> producer = new KafkaProducer<String, String>(props);

		ArrayList<JSONObject> _jsonArray = new ArrayList<JSONObject>();

		_jsonArray = (ArrayList<JSONObject>) input.getValueByField("jsonArray");
		
		if(_jsonArray.size() == 0){
			//if _jsonArray has no value log error : 1
			_LOG.warn("noValueEntered");
			errorCheck = true;
		}else if(_jsonArray.size() == 1 && _jsonArray.get(0).containsKey("error")){
			//if _jsonArray.get(0).containsKey("error") it means indexingBolt or StagingBolt occured an error log error : 2
			errorCheck = true;
		}else{
			//repeat producing json String in _jsonArray
			for (JSONObject tmpJsonObject : _jsonArray) {
				ProducerRecord<String, String> data = new ProducerRecord<String, String>("trigger",
						tmpJsonObject.toJSONString());
				producer.send(data);
				collector.emit(new Values(tmpJsonObject.toJSONString()));
			}
			errorCheck = false;
		}
		
		
		
		try {
			collector.ack(input);
			//log exited roadMapId and nodeId if error has not occured 
			
			if(errorCheck){
				
			}else{
				for(JSONObject tmp : _jsonArray){
					_LOG.info(tmp.get("roadMapId") + "," + tmp.get("nodeId") + "|" + tmp.get("topic") +"|" + tmp.toString());
				}
			}
		} catch (Exception e) {
			collector.fail(input);
		}
		
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("triggerTopologyResult"));
	}
}