package iothub.broker;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.mqtt.MqttEndpoint;
import io.vertx.mqtt.MqttTopicSubscription;
import iothub.rpc.*;

import java.util.ArrayList;
import java.util.List;

public class Client {

    private Vertx vertx;
    private MqttEndpoint endpoint;
    private String hubId;
    private String endpointId;
    private String groupId;

    public Client(Vertx vertx, MqttEndpoint endpoint) {
        this.vertx = vertx;
        this.endpoint = endpoint;
    }

    public String getHubId() {
        return hubId;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void Authentication() {
        String[] usernames = endpoint.auth().userName().split("&");
        String[] passwords = endpoint.auth().password().split("&");

        if (usernames.length != 2 || passwords.length == 0) {
            endpoint.close();
        } else {
            hubId = usernames[0];
            endpointId = usernames[1];
            if ( passwords.length > 0) {
                groupId = passwords[1];
            }

            vertx.eventBus().<Buffer>send(
                    "iothub.auth",
                    Buffer.buffer(
                            Authentication.newBuilder()
                                    .setHub(usernames[0])
                                    .setEndpoint(usernames[1])
                                    .setToken(passwords[0])
                                    .build()
                                    .toByteArray()
                    ),
                    ar -> {
                        try {
                            if (ar.succeeded()) {
                                AuthenticationResp(ar.result());
                            } else {
                                ar.cause();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            endpoint.close();
                        }
                    }
            );
        }
    }

    public void AuthenticationResp(Message<Buffer> message) throws InvalidProtocolBufferException {
        AuthenticationResp resp = AuthenticationResp.parseFrom(message.body().getBytes());
        if (resp.getErr() > 0) {
            endpoint.close();
        } else {
            endpoint.accept(false);
            endpoint.publishAutoAck(true);
            endpoint.publishHandler(
                    msg -> Publish(
                            msg.topicName(),
                            msg.payload(),
                            msg.qosLevel()
                    )
            );
            endpoint.subscribeHandler(
                    msg -> Subscribe(
                            msg.messageId(),
                            msg.topicSubscriptions()
                    )
            );
        }
    }

    public void Publish(String topic,
                        Buffer payload,
                        MqttQoS qos) {
        vertx.eventBus().<Buffer>send(
                "iothub.pubsub.pub",
                Buffer.buffer(
                        Publish.newBuilder()
                                .setHub(hubId)
                                .setEndpoint(endpointId)
                                .setTopic(topic)
                                .setPayload(ByteString.copyFrom(payload.getBytes()))
                                .setQos(qos.value())
                                .build()
                                .toByteArray()
                )
        );
    }


    public void Subscribe(int messageId,
                          List<MqttTopicSubscription> topics) {
        Subscribe.Builder builder = Subscribe.newBuilder();

        builder.setMsgid(messageId)
                .setHub(hubId)
                .setEndpoint(endpointId);
        for (MqttTopicSubscription topic : topics) {
            builder.addTopics(
                    SubscribeTopic.newBuilder()
                            .setTopic(topic.topicName())
                            .setQos(topic.qualityOfService().value())
                            .build()
            );
        }

        vertx.eventBus().<Buffer>send(
                "iothub.pubsub.sub",
                Buffer.buffer(builder.build().toByteArray()),
                ar -> {
                    try {
                        if (ar.succeeded()) {
                            SubscribeResp(ar.result());
                        } else {
                            ar.cause();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (messageId > 0) {
                            List<MqttQoS> qoses = new ArrayList<>();
                            for (MqttTopicSubscription topic : topics) {
                                qoses.add(MqttQoS.FAILURE);
                            }
                            endpoint.subscribeAcknowledge(messageId, qoses);
                        }
                        endpoint.close();
                    }
                }
        );
    }

    public void SubscribeResp(Message<Buffer> message) throws InvalidProtocolBufferException {
        SubscribeResp resp = SubscribeResp.parseFrom(message.body().getBytes());
        if (resp.getMsgid() > 0) {
            List<MqttQoS> qoses = new ArrayList<>();
            for (Integer qos : resp.getQosesList()) {
                qoses.add(MqttQoS.valueOf(qos.intValue()));
            }
            endpoint.subscribeAcknowledge(resp.getMsgid(), qoses);
        }
    }

    public void SendMessage(String topic,
                            Buffer payload,
                            MqttQoS qos) {
        endpoint.publish(topic, payload, qos, false, false);
    }
}
