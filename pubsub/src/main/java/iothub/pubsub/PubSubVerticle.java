package iothub.pubsub;

import com.google.protobuf.InvalidProtocolBufferException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import iothub.rpc.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PubSubVerticle extends AbstractVerticle{

    private Map<String, SubTree> subTreeMap;

    @Override
    public void start() throws Exception {
        super.start();

        subTreeMap = new HashMap<>();
        vertx.eventBus().<Buffer>consumer(
                "iothub.pubsub.sub",
                msg -> {
                    try {
                        Subscribe( msg );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        vertx.eventBus().<Buffer>consumer(
                "iothub.pubsub.pub",
                msg -> {
                    try {
                        Publish(msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    public void Subscribe(Message<Buffer> message) throws Exception {
        Subscribe req = Subscribe.parseFrom(message.body().getBytes());
        SubTree tree = subTreeMap.get(req.getHub());
        if (tree == null) {
            tree = new SubTree();
            subTreeMap.put(req.getHub(), tree);
        }

        SubscribeResp.Builder builder = SubscribeResp.newBuilder();
        for (SubscribeTopic topic : req.getTopicsList()) {
            tree.Subscribe(topic.getTopic(), req.getEndpoint());
            builder.addQoses(topic.getQos());
        }

        message.reply(
                Buffer.buffer(
                        builder.setMsgid(req.getMsgid())
                               .build()
                               .toByteArray()
                )
        );
    }

    public void Publish(Message<Buffer> message) throws Exception {
        Publish req = Publish.parseFrom(message.body().getBytes());
        SubTree tree = subTreeMap.get(req.getHub());
        if (tree != null) {
            for (String endpoint : tree.GetEndpoints(req.getTopic())) {
                vertx.eventBus().send(
                        "iothub.broker.message",
                        Buffer.buffer(
                                TopicMessage.newBuilder()
                                    .setHub(req.getHub())
                                    .setEndpoint(endpoint)
                                    .setTopic(req.getTopic())
                                    .setPayload(req.getPayload())
                                    .setQos(req.getQos())
                                    .build()
                                    .toByteArray()
                        )
                );
            }
        }
    }
}
