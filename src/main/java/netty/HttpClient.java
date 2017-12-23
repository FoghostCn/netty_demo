package netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AsciiString;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class HttpClient {

  private static InternalLogger internalLogger = InternalLoggerFactory.getInstance(HttpClient
    .class);

  private static SslContext sslCtx;
  private static EventLoopGroup eventLoopGroup;
  private URI uri;
  private HttpMethod method;
  private HttpHeaders headers;
  private ByteBuf content;
  private ChannelOption channelOption;
  private Bootstrap bootstrap = new Bootstrap();

  static {
    try {
      sslCtx = SslContextBuilder.forClient()
        .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
      eventLoopGroup = new NioEventLoopGroup();
      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          eventLoopGroup.shutdownGracefully();
        }
      });
    } catch (Exception e) {
      internalLogger.error(e);
    }
  }

  public static void main(String[] args) throws Exception {
    URI u = new URI("https://www.baidu.com/index?id=123#index");
    System.out.println(u.toASCIIString());
  }

  public HttpClient(URI uri) {
    this.uri = uri;
  }

  public static CompletableFuture<FullHttpResponse> get(String url) throws Exception {
    URI uri = new URI(url);
    HttpClient client = new HttpClient(uri);
    client.setMethod(HttpMethod.GET);
    return client.exec();
  }

  public CompletableFuture<FullHttpResponse> exec() throws Exception {
    CompletableFuture<FullHttpResponse> cf = new CompletableFuture<>();
//    Promise<FullHttpResponse> promise = eventLoopGroup.next().newPromise();
    bootstrap
      .group(eventLoopGroup)
      .channel(NioSocketChannel.class)
      .handler(buildChannelHandler(cf))
      .option(ChannelOption.TCP_NODELAY, true);
    ChannelFuture future = bootstrap.connect(uri.getHost(), this.getPort());
    future.addListener(f -> {
      cf.completeExceptionally(f.cause());
      if (!f.isSuccess()) {
        cf.completeExceptionally(f.cause());
      } else {
        future.channel().writeAndFlush(buildRequest());
        future.channel().closeFuture().addListener(ChannelFutureListener.CLOSE);
      }
    });
    return cf;
  }

  private FullHttpRequest buildRequest() {
    FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
      uri.toASCIIString(), content == null ? Unpooled.EMPTY_BUFFER : content);
    request.headers().set(HttpHeaderNames.HOST, uri.getHost());
    request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
    request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP_DEFLATE);
    request.headers().set(HttpHeaderNames.ACCEPT, AsciiString.cached("*/*"));
    if (null != headers) request.headers().add(headers);
    return request;
  }

  private <T extends Channel> ChannelInitializer<T> buildChannelHandler(CompletableFuture<FullHttpResponse>
                                                                          cf) {
    return new ChannelInitializer<T>() {
      @Override
      protected void initChannel(T ch) throws Exception {
        if ("https".equals(uri.getScheme())) {
          ch.pipeline().addFirst(sslCtx.newHandler(ch.alloc()));
        }
        ch.pipeline().addLast(
          new HttpClientCodec(),
          new HttpContentDecompressor(),
          new HttpObjectAggregator(1024 * 1024),
          new SimpleChannelInboundHandler<FullHttpResponse>() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response)
              throws Exception {
              cf.complete(response.retain());
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
              cf.completeExceptionally(cause);
            }
          });
      }
    };
  }

  int getPort() {
    if(uri.getPort() != -1) {
      return uri.getPort();
    }
    if("http".equals(uri.getScheme())) {
      return 80;
    }
    if("https".equals(uri.getScheme())) {
      return 443;
    }
    return -1;
  }

  public static void setSslCtx(SslContext sslCtx) {
    HttpClient.sslCtx = sslCtx;
  }

  public void setChannelOption(ChannelOption channelOption) {
    this.channelOption = channelOption;
  }

  public void setBootstrap(Bootstrap bootstrap) {
    this.bootstrap = bootstrap;
  }

  public void setEventLoopGroup(EventLoopGroup eventLoopGroup) {
    this.eventLoopGroup = eventLoopGroup;
  }

  public void setMethod(HttpMethod method) {
    this.method = method;
  }
}
