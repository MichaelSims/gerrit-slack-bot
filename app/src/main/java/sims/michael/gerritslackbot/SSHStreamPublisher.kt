package sims.michael.gerritslackbot

import io.reactivex.Flowable
import io.reactivex.rxkotlin.toFlowable
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import org.reactivestreams.Subscriber
import java.io.InputStream

private const val DEFAULT_COMMAND = "gerrit stream-events"

class SSHStreamPublisher(sshClientProvider: () -> SSHClient, config: Config) : Flowable<String>() {

    private val log = logger()

    data class Config(
            val username: String,
            val host: String,
            val port: Int = SSHClient.DEFAULT_PORT,
            val privateKeyLocations: List<String> = emptyList(),
            val command: String = DEFAULT_COMMAND
    )

    private data class CloseableResources(val sshClient: SSHClient, val session: Session, val inputStream: InputStream)

    private val delegate = Flowable.using(
            {
                val sshClient = sshClientProvider()
                sshClient.apply {
                    addHostKeyVerifier(PromiscuousVerifier())
                    connect(config.host, config.port)
                    connection.keepAlive.keepAliveInterval = 5
                    if (config.privateKeyLocations.isEmpty()) {
                        authPublickey(config.username)
                    } else {
                        authPublickey(config.username, *config.privateKeyLocations.toTypedArray())
                    }
                }
                val session: Session = sshClient.startSession().apply {
                    allocateDefaultPTY()
                }
                val inputStream = session.exec(config.command).inputStream
                CloseableResources(sshClient, session, inputStream)
            },
            {
                it.inputStream.bufferedReader().lineSequence().toFlowable()
            },
            {
                quietly { it.inputStream.close() }
                quietly { it.session.close() }
                quietly { it.sshClient.disconnect() }
            }
    )

    private inline fun quietly(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            log.error("Caught exception, ignoring", e)
        }
    }

    override fun subscribeActual(s: Subscriber<in String>) = delegate.subscribe(s)
}
