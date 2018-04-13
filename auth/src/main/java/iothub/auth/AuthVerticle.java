package iothub.auth;

import com.google.protobuf.InvalidProtocolBufferException;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import iothub.rpc.Authentication;
import iothub.rpc.AuthenticationResp;

public class AuthVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        super.start();

        vertx.eventBus().<Buffer>consumer(
                "iothub.auth",
                msg -> {
                    try {
                        Authentication(msg);
                    } catch (Exception e) {
                        e.printStackTrace();
                        msg.reply(
                                Buffer.buffer(
                                        AuthenticationResp.newBuilder()
                                                .setErr(1)
                                                .setStr(e.toString())
                                                .build()
                                                .toByteArray()
                                )
                        );
                    }
                }
        );
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    private void Authentication(Message<Buffer> message) throws InvalidProtocolBufferException {
        Authentication req = Authentication.parseFrom(message.body().getBytes());
        /* TODO */

        /* OK */
        message.reply(
                Buffer.buffer(
                        AuthenticationResp.newBuilder()
                                .setErr(0)
                                .build()
                                .toByteArray()
                )
        );
    }
}
