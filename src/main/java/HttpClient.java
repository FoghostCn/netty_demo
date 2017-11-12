import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;

import java.net.URI;

public class HttpClient extends DefaultPromise<FullHttpResponse> {

  private static SslContext sslCtx;
  private String url;
  private ChannelOption channelOption;
  private Bootstrap bootstrap = new Bootstrap();
  private EventLoopGroup eventLoopGroup;

  static {
    try {
      sslCtx = SslContextBuilder.forClient()
          .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
    } catch (Exception ignored) {
    }
  }

  @Override
  public Promise<FullHttpResponse> addListener(GenericFutureListener<? extends Future<? super FullHttpResponse>> listener) {

    return super.addListener(listener);
  }

  public static Promise<FullHttpResponse> get(String url) throws Exception {
    HttpClient client = new HttpClient();
    URI uri = new URI(url);
    client.setUrl(url);
    Bootstrap b = new Bootstrap()
//        .group(this.eventLoopGroup)
        .channel(NioSocketChannel.class)
        .option(ChannelOption.TCP_NODELAY, true)
        .handler(
            new ChannelInitializer<SocketChannel>() {
              @Override
              protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(
                    sslCtx.newHandler(ch.alloc()),
                    new HttpClientCodec(),
                    new HttpObjectAggregator(64 * 1024),
                    new HttpContentDecompressor(),
                    new SimpleChannelInboundHandler<FullHttpResponse>() {
                      @Override
                      protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
                        if (!response.headers().isEmpty()) {
                          response.headers().forEach(System.out::println);
                          System.out.println();
                          System.out.println(response.content().toString(CharsetUtil.UTF_8));
                        }
                      }
                    });
              }
            });
    ChannelFuture future = b.connect(uri.getHost(), 443).sync();
    future.addListener(f -> {
      if (f.isSuccess()) {
        System.out.println("connection established");
      } else {
        System.out.println("failed");
        f.cause().printStackTrace();
      }
    });
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
        "/");
    request.headers().set(HttpHeaderNames.HOST, uri.getHost());
    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
    future.channel().writeAndFlush(request);
    future.channel().closeFuture().addListener(ChannelFutureListener.CLOSE);
    return null;
  }

  public static SslContext getSslCtx() {
    return sslCtx;
  }

  public static void setSslCtx(SslContext sslCtx) {
    HttpClient.sslCtx = sslCtx;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public ChannelOption getChannelOption() {
    return channelOption;
  }

  public void setChannelOption(ChannelOption channelOption) {
    this.channelOption = channelOption;
  }

  public Bootstrap getBootstrap() {
    return bootstrap;
  }

  public void setBootstrap(Bootstrap bootstrap) {
    this.bootstrap = bootstrap;
  }

  public EventLoopGroup getEventLoopGroup() {
    return eventLoopGroup;
  }

  public void setEventLoopGroup(EventLoopGroup eventLoopGroup) {
    this.eventLoopGroup = eventLoopGroup;
  }
}
