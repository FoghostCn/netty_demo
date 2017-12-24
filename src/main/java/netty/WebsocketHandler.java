package netty;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Foghost
 * @since 2017/12/11
 */
@Sharable
public class WebsocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
  private static InternalLogger logger = InternalLoggerFactory.getInstance(WebsocketHandler.class);

  private static final AttributeKey<QueryStringDecoder> key = AttributeKey.newInstance("query");

  private static final Map<String, ChannelGroup> rooms = new ConcurrentHashMap<>();
  private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    ctx.channel().closeFuture().addListener(future -> logger.info(ctx.channel().id() + " channel closed"));
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
    channels.add(ctx.channel());
    channels.writeAndFlush(new TextWebSocketFrame("hahahahah")).addListener(future -> {
      if (future.isSuccess() && logger.isInfoEnabled()) {
        logger.info("all room write success");
      } else if(!future.isSuccess()){
        future.cause().printStackTrace();
      }
    });
    logger.info(msg.text());
    QueryStringDecoder queryStringDecoder = ctx.channel().attr(key).get();
    logger.info(queryStringDecoder.toString());
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (evt instanceof HandshakeComplete) {
      onHandshakeComplete(ctx, (HandshakeComplete) evt);
      return;
    }
    if (evt instanceof IdleStateEvent) {
      onIdleStateEvent(ctx, (IdleStateEvent) evt);
      return;
    }
    super.userEventTriggered(ctx, evt);
  }

  private static void onHandshakeComplete(ChannelHandlerContext ctx, HandshakeComplete complete) {
    logger.info(ctx.channel().id() + " handshake complete ");
    ctx.channel().attr(key).set(new QueryStringDecoder(complete.requestUri()));
    rooms.putIfAbsent(ctx.channel().id().asLongText(), new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
    rooms.get(ctx.channel().id().asLongText()).add(ctx.channel());
    channels.add(ctx.channel());
  }

  private static void onIdleStateEvent(ChannelHandlerContext ctx, IdleStateEvent evt) {
    logger.info(ctx.channel().id() + " idle");
    if (evt.isFirst()) {
      ctx.channel().writeAndFlush(new PingWebSocketFrame());
    } else {
      ctx.close();
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
  }
}
