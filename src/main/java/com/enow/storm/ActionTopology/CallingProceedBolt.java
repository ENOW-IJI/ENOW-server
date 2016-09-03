package com.enow.storm.ActionTopology;
import org.apache.kafka.clients.producer.*;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import com.enow.dto.TopicStructure;

public class CallingProceedBolt extends BaseRichBolt {
    protected static final Logger LOG = LoggerFactory.getLogger(CallingFeedBolt.class);
    private OutputCollector collector;
    private Properties props;
    private Producer<String, String> producer;
    private TopicStructure topicStructure;
    @Override

    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        this.collector = collector;
        props = new Properties();
        props.put("producer.type", "sync");
        props.put("batch.size", "1");
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producer =  new KafkaProducer<String, String>(props);
        topicStructure = new TopicStructure();
    }

    @Override
    public void execute(Tuple input) {	
    	topicStructure = (TopicStructure) input.getValueByField("topicStructure");
		if (null == topicStructure) {
			return;
		}
    
		
		collector.emit(new Values(topicStructure));
		try {
			LOG.debug("input = [" + input + "]");
			collector.ack(input);
		} catch (Exception e) {
			collector.fail(input);
		}
    	/*
    	if(null == input.getValueByField("topic"))
 	    {
 	        return;
 	    }else if((null == input.getStringByField("msg") || input.getStringByField("msg").length() == 0))
 	    {
 	        return;
 	    }
 	    */
    	
    	
    	//ts = (TopicStructure)input.getValueByField("topic");
    	//final String msg = input.getStringByField("msg");
    	
		//ProducerRecord<String, String> data = new ProducerRecord<String, String>("feed", "ServerID: " + ts.getServerId() +  " msg : " + msg);
		ProducerRecord<String, String> data = new ProducerRecord<String, String>("proceed","proceed "  +topicStructure.output());
		
		producer.send(data);

		try {
			LOG.debug("input = [" + input + "]");
			collector.ack(input);
		} catch (Exception e) {
			collector.fail(input);
		}
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
    }
}