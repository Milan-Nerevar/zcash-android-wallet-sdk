package cash.z.wallet.sdk.sample.demoapp.fixture

import cash.z.ecc.android.sdk.model.Zatoshi

object ZatoshiFixture {
    @Suppress("MagicNumber")
    const val ZATOSHI_LONG = 123456789L

    fun new(value: Long = ZATOSHI_LONG) = Zatoshi(value)
}
