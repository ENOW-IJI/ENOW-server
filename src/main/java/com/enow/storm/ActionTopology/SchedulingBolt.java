package com.enow.storm.ActionTopology;

import com.enow.dto.TopicStructure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

public class SchedulingBolt extends BaseRichBolt {
    protected static final Logger _LOG = LogManager.getLogger(SchedulingBolt.class);
    ConcurrentHashMap<String[], Boolean> _peerNode = new ConcurrentHashMap<>();
    ConcurrentHashMap<String, JSONObject> _visitedNode = new ConcurrentHashMap<>();
    private OutputCollector _collector;
    private JSONParser _parser;

    @Override
    public void prepare(Map MongoConf, TopologyContext context, OutputCollector collector) {
        _collector = collector;
        _parser = new JSONParser();
    }

    @Override
    public void execute(Tuple input) {

        JSONObject _jsonObject;

        if ((null == input.toString()) || (input.toString().length() == 0)) {
            return;
        }

        String msg = input.getValues().toString().substring(1, input.getValues().toString().length() - 1);

        try {
            _jsonObject = (JSONObject) _parser.parse(msg);
            _LOG.warn("Succeed in inserting messages to JSONObject : \n" + _jsonObject.toJSONString());
        } catch (ParseException e1) {
            e1.printStackTrace();
            _LOG.warn("Fail in inserting messages to JSONObject");
            _collector.fail(input);
            return;
        }

        System.out.println(_jsonObject.toJSONString());

        if ((Boolean) _jsonObject.get("ack")) {
            // Acknowledge ack = true
            String[] waitingPeers = (String[]) _jsonObject.get("waitingPeer");
            if (waitingPeers[0] != "0")
                if(_peerNode.containsKey(waitingPeers))
                    for (String peer : waitingPeers)
                        if(!_visitedNode.containsKey(peer))
                            _jsonObject.put("proceed", true);
            else _jsonObject.put("proceed", true);
        } else {
            // Execution Cycle : ack = false
            // 새로 들어온 `mapId`인지 확인
            String currentMapId = (String) _jsonObject.get("mapId");
            if (_visitedNode.containsKey(currentMapId)) {
                // 이미 방문했던 `mapId`
                _LOG.error("This node already visited before!"
                        + " Check the ConcurrentHashMap you created");
                return;
            } else {
                // 새로 방문한 `mapId`
                _visitedNode.put(currentMapId, _jsonObject);
                // 새로 들어온 `mapId`면 `ConcurrentHashMap`에 `peer`들과 함께 저장
                String[] waitingPeers = (String[]) _jsonObject.get("waitingPeer");
                String[] incomingPeers = (String[]) _jsonObject.get("incomingPeer");
                if (waitingPeers[0] != "0") { // waitingPeers exist
                    if (_peerNode.containsKey(waitingPeers));  // 이미 다른 peer로 부터 자신의 currentId가 저장됨
                    else {
                        _peerNode.put(waitingPeers, false);
                    }
                } else {
                    if (incomingPeers[0] != "0") { // incomingPeers exist
                        JSONArray _jsonArray = new JSONArray();
                        int i = 0;
                        for (String peer : incomingPeers)
                            _jsonArray.add(i++, _visitedNode.get(peer).get("message"));
                        _jsonObject.put("previousData", _jsonArray);
                    }
                }
            }
        }

        _collector.emit(new Values(_jsonObject));
        try {
            _LOG.debug("input = [" + input + "]");
            _collector.ack(input);
        } catch (Exception e) {
            _collector.fail(input);
        }
        /*
        // The elements variable
        // elements[0] = spoutSource
        // elements[1] = topics
        // e.g corporationName/serverId/brokerId/deviceId2/phaseRoadMapId/currentMapId/previousMapId
        // elements[2] = messages
        // e.g current/previous
        String[] elements = new String[3];
        String[] topics = new String[7];
        String executedNodeId = elements[1];
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

        _topicStructure.setCorporationName(topics[0]);
        _topicStructure.setServerId(topics[1]);
        _topicStructure.setBrokerId(topics[2]);
        _topicStructure.setDeviceId(topics[3]);
        _topicStructure.setPhaseRoadMapId(topics[4]);
        _topicStructure.setPhaseId(topics[5]);
        _topicStructure.setCurrentMapId(topics[6]);
        // Msg will be handled by conditional contexts with ConcurrentHashMap later
        // Msg 는 나중에 조건문으로 헨들링됨.
        // _topicStructure.setCurrentMsg(elements[2]);
        boolean check = false;

        // Execution Cycle 1 : ack == false

        if (elements[0].equals("status")) {
            // Is this init node?
            if (topics.length < 7) {
                // Is this have been stored to ConcurrentHashMap?
                if (!this._executedNode.containsKey(executedNodeId)) {
                    // Init node has single message; current one,
                    // so don't have to tokenize
                    // 최초 노드는 현제 메시지만 가짐. 토크나이저 불필요
                    _topicStructure.setCurrentMsg(elements[2]);
                    this._executedNode.put(executedNodeId, _topicStructure);
                    System.out.println("Succeed in storing " + temp + " to ConcurrentHashMap");

                } else {
                    this._executedNode.remove(executedNodeId, _topicStructure);
                    // This can activate provisioningBolt to execute subsequent node.
                    // check는 provisioningBolt의 peerOut를 활성화 시킴(ack값을 받았다는 뜻)
                    check = true;

                    String messages[] = new String[2];


                    // Confirm ack value for next execution
                    // ack 확인 코드

                    // --- Code needed ---

                    // Add PreviousMapId to topicStructure
                    _topicStructure.setPreviousMapId(topics[7]);

                    // Tokenize elements[2] for current and previous messages
                    tokenizer = new StringTokenizer(elements[2], "/");
                    for (int index = 0; tokenizer.hasMoreTokens(); index++) {
                        messages[index] = tokenizer.nextToken().toString();
                        System.out.println("messages[" + index + "]: " + messages[index]);
                    }
                    // Add Messages to topicStructure
                    _topicStructure.setCurrentMsg(messages[0]);
                    _topicStructure.setPreviousMsg(messages[1]);
                }
            } else {
                // If the node isn't init node
                // Both ack and exec nodes have previous infomation
                // 최초 노드가 아니면 ack, exec 노드 모두 이전 값을 가지므로 토크나이저가 필요
                String messages[] = new String[2];
                tokenizer = new StringTokenizer(elements[2], "/");
                for (int index = 0; tokenizer.hasMoreTokens(); index++) {
                    messages[index] = tokenizer.nextToken().toString();
                    System.out.println("messages[" + index + "]: " + messages[index]);
                }
                // Add PreviousMapId to topicStructure
                _topicStructure.setPreviousMapId(topics[7]);
                // Add Messages to topicStructure
                _topicStructure.setCurrentMsg(messages[0]);
                _topicStructure.setPreviousMsg(messages[1]);
                if (!this._executedNode.containsKey(executedNodeId)) {
                    this._executedNode.put(executedNodeId, _topicStructure);
                } else {
                    this._executedNode.remove(executedNodeId, _topicStructure);
                    // This can activate provisioningBolt to execute subsequent node.
                    // check는 provisioningBolt의 peerOut을 활성화 시킴(ack값을 받았다는 뜻)
                    check = true;
                    // Add peerOut for heading to next node

                }
            }
        }
        */
        // Case 2 : spoutSource == status
        // Data handling part
        /*
        if (elements[0].equals("status")) {
            // spoutSource == status
            if (this._executedNode.containsKey(executedNodeId)) {
                check = true;
            }
            // Data handling part
        }
        */
        // Don't block data flow of Apache Storm!
        // 무조건 emit을 함으로써 storm의 데이터 흐름을 막지 않아야!!!
//        _collector.emit(new Values(_jsonObject));
//        try {
//            _LOG.info("Try to send input to ExecutingBolt = [\n" + _jsonObject.toJSONString() + "\n]\n");
//            // _jsonObject = (JSONObject)_parser.parse(_jsonObject);
//        } catch (ParseException e1) {
//            // TODO Auto-generated catch block
//            _LOG.warn("Fail in send input to ExecutingBolt = [\n" + _jsonObject.toJSONString() + "\n]\n");
//            e1.printStackTrace();
//        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("jsonObject"));
    }
}
