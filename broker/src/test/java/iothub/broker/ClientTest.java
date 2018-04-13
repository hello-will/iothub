package iothub.broker;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

public class ClientTest {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        MqttClient client = MqttClient.create(vertx,
                new MqttClientOptions()
                        .setClientId("test")
                        .setUsername("1234&test")
                        .setPassword("token&test"));
        client.connect(1883, "127.0.0.1", ack -> {
            client.subscribe("test",
                    MqttQoS.AT_MOST_ONCE.value(),
                    ret->{
                        client.publish("test", Buffer.buffer("test"), MqttQoS.AT_MOST_ONCE, false, false);
                        client.publishHandler(msg -> {
                            System.out.println("Received a publish Message, topic = " + msg.topicName() + ", payload = " + msg.payload().toString());
                            client.disconnect(res -> {
                                vertx.close();
                            });
                        });
                    });
        });
    }

}
