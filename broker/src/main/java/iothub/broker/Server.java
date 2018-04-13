package iothub.broker;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.mqtt.MqttServer;
import iothub.rpc.TopicMessage;

import java.util.HashMap;
import java.util.Map;

public class Server {

    private MqttServer server;
    private Map<String, Client> clientMap;

    public void start(Vertx vertx) {
        clientMap = new HashMap<>();
        server = MqttServer.create(vertx);
        server.endpointHandler(endpoint -> {
            endpoint.closeHandler(ret ->
                    clientMap.remove(endpoint.auth().userName())
            );

            Client client = new Client(vertx, endpoint);
            clientMap.put(endpoint.auth().userName(), client);
            client.Authentication();
        }).listen();
        vertx.eventBus().<Buffer>consumer(
                "iothub.broker.message",
                msg -> {
                    try {
                        ReceivedTopicMessage(msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    public void stop() {
        server.close();
    }

    public void ReceivedTopicMessage(Message<Buffer> message) throws Exception {
        TopicMessage req = TopicMessage.parseFrom(message.body().getBytes());
        String userName = req.getHub() + "&" + req.getEndpoint();
        Client client = clientMap.get(userName);
        if (client != null) {
            client.SendMessage(req.getTopic(),
                               Buffer.buffer(
                                       req.getPayload().toByteArray()
                               ),
                               MqttQoS.valueOf(req.getQos()));
        }
    }
}
