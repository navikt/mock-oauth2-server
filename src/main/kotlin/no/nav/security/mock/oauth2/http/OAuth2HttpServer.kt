package no.nav.security.mock.oauth2.http

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFactory
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ServerChannel
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpHeaders
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.HttpServerKeepAliveHandler
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.ssl.SslHandler
import io.netty.handler.stream.ChunkedStream
import io.netty.handler.stream.ChunkedWriteHandler
import io.netty.util.CharsetUtil
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import javax.net.ssl.SSLHandshakeException
import kotlin.properties.Delegates
import mu.KotlinLogging
import no.nav.security.mock.oauth2.extensions.asOAuth2HttpRequest
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest

private val log = KotlinLogging.logger { }

interface OAuth2HttpServer : AutoCloseable {
    fun start(requestHandler: RequestHandler) = start(port = 0, requestHandler = requestHandler)
    fun start(port: Int = 0, requestHandler: RequestHandler = { OAuth2HttpResponse(status = 404, body = "no requesthandler configured") }) =
        start(InetAddress.getByName("localhost"), port, requestHandler)

    fun start(inetAddress: InetAddress, port: Int, requestHandler: RequestHandler): OAuth2HttpServer
    fun stop(): OAuth2HttpServer
    override fun close() {
        stop()
    }

    fun port(): Int
    fun url(path: String): HttpUrl
    fun sslConfig(): Ssl?
}

class MockWebServerWrapper@JvmOverloads constructor(
    val ssl: Ssl? = null
) : OAuth2HttpServer {
    val mockWebServer: MockWebServer = MockWebServer()

    override fun start(inetAddress: InetAddress, port: Int, requestHandler: RequestHandler): OAuth2HttpServer = apply {
        mockWebServer.start(inetAddress, port)
        mockWebServer.dispatcher = MockWebServerDispatcher(requestHandler)
        if (ssl != null) {
            mockWebServer.useHttps(ssl.sslContext().socketFactory, false)
        }
        log.debug("started server on address=$inetAddress and port=${mockWebServer.port}, httpsEnabled=${ssl != null}")
    }

    override fun stop(): OAuth2HttpServer = apply {
        mockWebServer.shutdown()
    }

    override fun port(): Int = mockWebServer.port

    override fun url(path: String): HttpUrl = mockWebServer.url(path)
    override fun sslConfig(): Ssl? = ssl

    internal class MockWebServerDispatcher(
        private val requestHandler: RequestHandler,
        private val responseQueue: BlockingQueue<MockResponse> = LinkedBlockingQueue()
    ) : Dispatcher() {

        override fun dispatch(request: RecordedRequest): MockResponse =
            responseQueue.peek()?.let {
                responseQueue.take()
            } ?: requestHandler.invoke(request.asOAuth2HttpRequest()).toMockResponse()

        private fun OAuth2HttpResponse.toMockResponse(): MockResponse =
            MockResponse()
                .setHeaders(this.headers)
                .setResponseCode(this.status)
                .let {
                    if (this.body != null) it.setBody(this.body) else it.setBody("")
                }
    }
}

class NettyWrapper @JvmOverloads constructor(
    val ssl: Ssl? = null
) : OAuth2HttpServer {
    private val masterGroup = NioEventLoopGroup()
    private val workerGroup = NioEventLoopGroup()
    private var closeFuture: ChannelFuture? = null
    private lateinit var address: InetSocketAddress
    private var port by Delegates.notNull<Int>()

    override fun start(inetAddress: InetAddress, port: Int, requestHandler: RequestHandler): OAuth2HttpServer =
        apply {
            val bootstrap = ServerBootstrap()
            bootstrap.group(masterGroup, workerGroup)
                .channelFactory(ChannelFactory<ServerChannel> { NioServerSocketChannel() })
                .childHandler(
                    object : ChannelInitializer<SocketChannel>() {
                        public override fun initChannel(ch: SocketChannel) {
                            if (ssl != null) {
                                ch.pipeline().addFirst("ssl", ssl.nettySslHandler())
                            }
                            ch.pipeline().addLast("codec", HttpServerCodec())
                            ch.pipeline().addLast("keepAlive", HttpServerKeepAliveHandler())
                            ch.pipeline().addLast("aggregator", HttpObjectAggregator(Int.MAX_VALUE))
                            ch.pipeline().addLast("streamer", ChunkedWriteHandler())
                            ch.pipeline().addLast("routes", RouterChannelHandler(requestHandler))
                        }
                    }
                )
                .option(ChannelOption.SO_BACKLOG, 1000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)

            val channel = bootstrap.bind(inetAddress, port).sync().channel()
            this.address = channel.localAddress() as InetSocketAddress
            this.port = port
            this.closeFuture = channel.closeFuture()
            log.debug("started server on address=${this.address} and port=$port")
        }

    override fun stop() = apply {
        closeFuture?.cancel(false)
        workerGroup.shutdownGracefully()
        masterGroup.shutdownGracefully()
    }

    override fun port(): Int = if (port > 0) port else address.port

    override fun url(path: String): HttpUrl {
        val scheme = if (ssl != null) {
            "https"
        } else {
            "http"
        }
        return HttpUrl.Builder()
            .scheme(scheme)
            .host(address.address.canonicalHostName)
            .port(port())
            .build()
            .resolve(path)!!
    }

    override fun sslConfig(): Ssl? = ssl

    private fun Ssl.nettySslHandler(): SslHandler = SslHandler(sslEngine())

    internal class RouterChannelHandler(val requestHandler: RequestHandler) : SimpleChannelInboundHandler<FullHttpRequest>() {
        @Deprecated("Deprecated in ChannelInboundHandlerAdapter")
        override fun exceptionCaught(ctx: ChannelHandlerContext, throwable: Throwable) {
            val msg = throwable.message ?: ""
            val ignoreError = "certificate_unknown"

            // have not been able to determine why this error is thrown or how to fix it, but it does not seem to affect the server
            if (throwable.cause is SSLHandshakeException && msg.contains(ignoreError)) {
                log.debug("received $ignoreError error from netty channel, ignoring")
            } else {
                log.error("error in netty channel handler", throwable)
                ctx.close()
            }
        }

        override fun channelRead0(ctx: ChannelHandlerContext, request: FullHttpRequest) {
            val address = ctx.channel().remoteAddress() as InetSocketAddress
            val port = (ctx.channel().localAddress() as InetSocketAddress).port
            val scheme = if (ctx.pipeline().get(SslHandler::class.java) == null) "http" else "https"
            val (response, stream) = requestHandler(request.asOAuth2HttpRequest(scheme, address, port)).asNettyResponse()
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)

            ctx.write(response)
            ctx.write(stream)
            ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
        }

        private fun OAuth2HttpResponse.asNettyResponse(): Pair<DefaultHttpResponse, ChunkedStream> =
            DefaultHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus(this.status, "")
            ).apply {
                this@asNettyResponse.headers.forEach { (key, values) -> headers().set(key, values) }
            } to ChunkedStream(this.body?.byteInputStream() ?: "".byteInputStream())

        private fun FullHttpRequest.asOAuth2HttpRequest(scheme: String, address: InetSocketAddress, port: Int) =
            OAuth2HttpRequest(
                this.headers().toOkHttpHeaders(),
                this.method().name(),
                this.requestUrl(scheme, address, port),
                content().toString(CharsetUtil.UTF_8)
            )

        private fun FullHttpRequest.requestUrl(scheme: String, address: InetSocketAddress, port: Int): HttpUrl =
            HttpUrl.Builder()
                .scheme(scheme)
                .host(hostAndPortFromHostHeader()?.first ?: address.hostName)
                .port(hostAndPortFromHostHeader()?.second ?: port)
                .build()
                .resolve(this.uri())!!

        private fun FullHttpRequest.hostAndPortFromHostHeader(): Pair<String, Int>? =
            this.headers()["Host"]?.let {
                if (it.substringAfter(":").toIntOrNull() != null) {
                    it.substringBefore(":") to it.substringAfter(":").toInt()
                } else {
                    null
                }
            }

        private fun HttpHeaders.toOkHttpHeaders(): Headers {
            val headers = Headers.headersOf().newBuilder()
            this.forEach {
                headers.add(it.key, it.value)
            }
            return headers.build()
        }
    }
}
