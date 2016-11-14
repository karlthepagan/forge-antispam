package karl.codes.chatbot;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by karl on 9/1/16.
 */
public class BotClientHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger log = LoggerFactory.getLogger(BotClientHandler.class);

    private static final Pattern PRIVMSG = Pattern.compile(":([^!]+)\\S+ PRIVMSG #\\S+ :");
    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;

    public BotClientHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("WebSocket Client disconnected!");
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            System.out.println("WebSocket Client connected!");
            handshakeFuture.setSuccess();
            return;
        }

        if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) msg;
            ByteBuf buf = textFrame.content();

            CharSequence charbuf = buf.getCharSequence(0,buf.readableBytes(),CharsetUtil.UTF_8);
            Matcher m = PRIVMSG.matcher(charbuf);
            if (!m.find()) {
                log.info(textFrame.text());
                return;
            }

            CharSequence payload = charbuf.subSequence(m.end(), charbuf.length());
            System.out.print(m.group(1));
            System.out.print(": ");
            System.out.print(payload);
        } else if (msg instanceof PongWebSocketFrame) {
            System.out.println("WebSocket Client received pong");
        } else if (msg instanceof CloseWebSocketFrame) {
            System.out.println("WebSocket Client received closing");
            ch.close();
        } else if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.status() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        } else {
            System.out.println("other message: " + msg.getClass());
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }

    /*
    twitch.tv/commands enables
    USERSTATE, GLOBALUSERSTATE, HOSTTARGET, NOTICE and CLEARCHAT

    twitch.tv/tags
     */
}
