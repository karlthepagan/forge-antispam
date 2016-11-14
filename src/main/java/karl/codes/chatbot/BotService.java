package karl.codes.chatbot;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Future;

/**
 * @author karl
 */
@Service
public class BotService {
    private static final Logger log = LoggerFactory.getLogger(BotService.class);

    final EventLoopGroup group = new NioEventLoopGroup();

    final ByteBuf CR = Unpooled.copiedBuffer("\n",CharsetUtil.UTF_8);
    final ByteBuf PASS_MSG = Unpooled.copiedBuffer("PASS oauth:",CharsetUtil.UTF_8);
    final ByteBuf NICK_MSG = Unpooled.copiedBuffer("NICK ",CharsetUtil.UTF_8);

    final ByteBuf CAP_MEMBER = Unpooled.copiedBuffer("CAP REQ :twitch.tv/membership\n",CharsetUtil.UTF_8);
    final ByteBuf CAP_COMMANDS = Unpooled.copiedBuffer("CAP REQ :twitch.tv/commands\n",CharsetUtil.UTF_8);
    final ByteBuf CAP_TAGS = Unpooled.copiedBuffer("CAP REQ :twitch.tv/tags\n",CharsetUtil.UTF_8);

    Channel ch = null;

    @Autowired
    BotProperties properties;

    @PostConstruct
    public void init() throws URISyntaxException, SSLException, InterruptedException {
        validate(properties);

        if(properties.isConnectOnStartup()) {
            this.connect(true);
        }
    }

    protected void validate(BotProperties properties) throws IllegalArgumentException {
        if(StringUtils.isEmpty(properties.getChatToken())) {
            throw new IllegalArgumentException("chatToken is empty");
        }

        if(StringUtils.isEmpty(properties.getName())) {
            throw new IllegalArgumentException("name is empty");
        }
    }

    @Async
    public Future<ChannelFuture> connect(boolean auth) throws URISyntaxException, SSLException, InterruptedException {
        URI uri = new URI(properties.getUrl());
        String scheme = uri.getScheme() == null? "ws" : uri.getScheme();
        final String host = uri.getHost() == null? "127.0.0.1" : uri.getHost();
        final int port;
        if (uri.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only WS(S) is supported. (" + scheme + ")");
        }

        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        boolean crashed = true;
        try {
            // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
            // If you change it to V00, ping is not supported and remember to change
            // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
            final BotClientHandler handler =
                    new BotClientHandler(
                            WebSocketClientHandshakerFactory.newHandshaker(
                                    uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()));

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                            }
                            p.addLast(
                                    new HttpClientCodec(),
                                    new HttpObjectAggregator(8192),
                                    WebSocketClientCompressionHandler.INSTANCE,
                                    handler);
                        }
                    });

            ChannelFuture connect = b.connect(uri.getHost(), port);
            ch = connect.sync().channel();
            handler.handshakeFuture().sync();

            if(auth) {
                text("PASS oauth:" + properties.getChatToken() + "\n").sync();
                log.info("Authenticated");

                text("NICK " + properties.getName() + "\n").sync();
                log.info("Nickname");

//                ch.write(buf(CAP_MEMBER));
//                ch.write(buf(CAP_COMMANDS));
//                ch.write(buf(CAP_TAGS)).sync();
//                log.info("Requested capabilities");

//                ch.write(buf(PASS_MSG, Unpooled.copiedBuffer(properties.getChatToken(),CharsetUtil.UTF_8), CR));
//
//                ch.write(buf(NICK_MSG, Unpooled.copiedBuffer(properties.getName(),CharsetUtil.UTF_8), CR));
            }

//            ch.write(CAP_MEMBER);
//            ch.write(CAP_COMMANDS);
//            ch.writeAndFlush(CAP_TAGS).sync();
/*
fail to connect is:
:tmi.twitch.tv NOTICE * :Login authentication failed

bad syntax:
just disconnects

welcome message is:
 Client received message: :tmi.twitch.tv 001 karlthepagan :Welcome, GLHF!
:tmi.twitch.tv 002 karlthepagan :Your host is tmi.twitch.tv
:tmi.twitch.tv 003 karlthepagan :This server is rather new
:tmi.twitch.tv 004 karlthepagan :-
:tmi.twitch.tv 375 karlthepagan :-
:tmi.twitch.tv 372 karlthepagan :You are in a maze of twisty passages, all alike.
:tmi.twitch.tv 376 karlthepagan :>
 */
            crashed = false;
            return new AsyncResult<>(connect);
        } finally {
            if(crashed) {
                group.shutdownGracefully();
            }
        }
    }

    private WebSocketFrame buf(ByteBuf ... parts) {
        if(parts.length == 1) {
            return new TextWebSocketFrame(parts[0]);
        }

        return new TextWebSocketFrame(
                Unpooled.compositeBuffer(parts.length).addComponents(parts)
        );
    }

    public Future<?> disconnect() {
        ch.writeAndFlush(new CloseWebSocketFrame());
        return group.shutdownGracefully();
    }

    public void wsPing() {
        WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[] { 8, 1, 8, 1 }));
        ch.writeAndFlush(frame);
    }

    public ChannelFuture text(String msg) {
        if(StringUtils.isEmpty(msg)) {
            return ch.voidPromise();
        }

        WebSocketFrame frame = new TextWebSocketFrame(msg);
        return ch.writeAndFlush(frame);
    }
}
