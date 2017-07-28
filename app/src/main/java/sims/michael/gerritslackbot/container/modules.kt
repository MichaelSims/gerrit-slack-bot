package sims.michael.gerritslackbot.container

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.salomonbrys.kodein.*
import io.reactivex.Flowable
import net.schmizz.keepalive.KeepAliveProvider
import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import okhttp3.OkHttpClient
import sims.michael.gerritslackbot.*
import sims.michael.gerritslackbot.model.Config
import sims.michael.gerritslackbot.slack.EventGroupToSlackMessageTransformer
import sims.michael.gerritslackbot.slack.SlackNotifier
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.SecureRandom
import javax.net.ssl.SSLContext

enum class ObjectMappers { YAML, JSON }

val defaultModule = Kodein.Module {

    bind() from singleton {
        instance<InputStream>().use {
            instance<ObjectMapper>(ObjectMappers.YAML).readValue(it, Config::class.java)
        }
    }

    bind() from provider { SSHClient(DefaultConfig().apply { keepAliveProvider = KeepAliveProvider.KEEP_ALIVE }) }

    bind(ObjectMappers.YAML) from singleton { ObjectMapper(YAMLFactory()).registerModule(KotlinModule()) }
    bind(ObjectMappers.JSON) from singleton { ObjectMapper().registerModule(KotlinModule()) }

    bind<Flowable<String>>() with singleton { SSHStreamPublisher(provider(), instance<Config>().gerritStreamConfig) }

    bind() from singleton { StringToEventTransformer(instance(ObjectMappers.JSON)) }

    bind<OkHttpClient>() with singleton {
        val config = instance<Config>().httpConfig
        val builder = OkHttpClient.Builder().apply {
            if (config.trustAllSSLCerts) {
                val trustManager = AllTrustingTrustManager()
                val sslContext = SSLContext.getInstance("TLS").apply {
                    init(null, arrayOf(trustManager), SecureRandom())
                }
                sslSocketFactory(sslContext.socketFactory, trustManager)
                hostnameVerifier({ _, _ -> true })
            }
            if (!config.proxyHost.isNullOrEmpty()) {
                proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(config.proxyHost, config.proxyPort)))
            }
        }
        builder.build()
    }

    bind() from singleton { SlackNotifier(instance(), instance(ObjectMappers.JSON), instance<Config>().slackApiUrl) }

    bind<SlackNameResolver>() with singleton { DefaultSlackNameResolver(instance<Config>().gerritToSlackMap) }

    bind() from singleton { EventGroupToSlackMessageTransformer(instance(), instance<Config>().slackMessageConfig) }

    bind() from singleton { EventGroupingTransformer(instance<Config>().changeMatchers) }
}
