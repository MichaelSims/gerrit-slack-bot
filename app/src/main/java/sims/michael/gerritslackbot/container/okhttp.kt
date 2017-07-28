package sims.michael.gerritslackbot.container

import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class AllTrustingTrustManager : X509TrustManager {
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    override fun checkClientTrusted(certs: Array<out X509Certificate>, authType: String) = Unit
    override fun checkServerTrusted(certs: Array<out X509Certificate>, authType: String) = Unit
}
