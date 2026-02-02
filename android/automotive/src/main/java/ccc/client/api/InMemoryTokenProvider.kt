package ccc.client.api

import java.util.concurrent.atomic.AtomicReference

class InMemoryTokenProvider : TokenProvider {
    private val tokenRef = AtomicReference<String?>(null)

    override fun token(): String? = tokenRef.get()

    fun setToken(token: String?) {
        tokenRef.set(token)
    }
}
