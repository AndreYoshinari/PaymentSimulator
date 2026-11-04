package br.com.paymentsimulator.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import br.com.paymentsimulator.data.repository.samplePayment
import br.com.paymentsimulator.domain.model.Payment
import br.com.paymentsimulator.domain.model.PaymentMethod
import br.com.paymentsimulator.domain.model.PaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaymentPersistenceTest {
    private lateinit var database: AppDatabase
    private lateinit var source: RoomPaymentLocalDataSource

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        source = RoomPaymentLocalDataSource(database.paymentDao())
    }

    @After
    fun tearDown() {
        if (::database.isInitialized) {
            database.close()
        }
    }

    @Test
    fun storesTheFieldsNeededForAReceipt() = runBlocking {
        val payment = samplePayment(
            cents = 500,
            quantity = 2,
            method = PaymentMethod.CREDIT_CARD,
        )

        source.upsert(payment)

        val stored = source.findById(payment.id)
        val row = database.paymentDao().findById(payment.id.value.toString())!!.payment
        assertEquals(payment, stored)
        assertEquals(payment, source.findByIdempotencyKey(payment.idempotencyKey))
        assertEquals(1_000L, row.amountInCents)
        assertEquals("BRL", row.currency)
        assertEquals(payment.id.value.toString(), row.id)
        assertEquals(payment.idempotencyKey.value, row.idempotencyKey)
        assertEquals(PaymentMethod.CREDIT_CARD.name, row.method)
        assertEquals(PaymentStatus.PENDING.name, row.status)
        assertEquals(payment.createdAt.toEpochMilli(), row.createdAtEpochMillis)
        assertNull(source.findById(samplePayment(id = "dddddddd-dddd-dddd-dddd-dddddddddddd").id))
    }

    @Test
    fun statusUpdateKeepsTheSamePayment() = runBlocking {
        val pending = samplePayment()
        source.upsert(pending)

        val processing = pending.copy(status = PaymentStatus.PROCESSING)
        source.upsert(processing)
        val approved = processing.copy(status = PaymentStatus.APPROVED)
        source.upsert(approved)

        val stored = source.findById(pending.id)
        val rows = database.paymentDao().findById(pending.id.value.toString())
        assertEquals(approved, stored)
        assertEquals(PaymentStatus.APPROVED.name, rows!!.payment.status)
        assertEquals(pending.id, stored!!.id)
        assertEquals(pending.idempotencyKey, stored.idempotencyKey)
        assertEquals(pending.amount.amountInCents, stored.amount.amountInCents)
        assertEquals(pending.method, stored.method)
        assertEquals(pending.createdAt, stored.createdAt)
        assertEquals(1, rows.items.size)
    }

    @Test
    fun observeAllEmitsHistoryInCreationOrder() = runBlocking {
        val older = samplePayment(
            id = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
            key = "older",
            createdAt = Instant.parse("2026-01-15T12:00:00Z"),
        )
        val newer = samplePayment(
            id = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
            key = "newer",
            createdAt = Instant.parse("2026-01-16T12:00:00Z"),
            status = PaymentStatus.APPROVED,
        )

        source.observeAll().test {
            assertEquals(emptyList<Payment>(), awaitItem())
            source.upsert(newer)
            assertEquals(listOf(newer), awaitItem())
            source.upsert(older)
            assertEquals(listOf(older, newer), awaitItem())

            val updatedOlder = older.copy(status = PaymentStatus.DECLINED)
            source.upsert(updatedOlder)
            assertEquals(listOf(updatedOlder, newer), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun theSameIdempotencyKeyDoesNotReplaceAnotherPayment() = runBlocking {
        val original = samplePayment(
            status = PaymentStatus.APPROVED,
            cents = 500,
            key = "operation-1",
        )
        val other = samplePayment(
            status = PaymentStatus.DECLINED,
            cents = 900,
            key = "operation-1",
            id = "dddddddd-dddd-dddd-dddd-dddddddddddd",
        )

        source.upsert(original)
        source.upsert(other)

        assertEquals(original, source.findById(original.id))
        assertNull(source.findById(other.id))
        source.observeAll().test {
            val history = awaitItem()
            assertEquals(original.id, history.single().id)
            assertEquals(PaymentStatus.APPROVED, history.single().status)
            assertEquals(500L, history.single().amount.amountInCents)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeByIdEmitsTheStoredPayment() = runBlocking {
        val payment = samplePayment(status = PaymentStatus.APPROVED)

        source.observeById(payment.id).test {
            assertNull(awaitItem())
            source.upsert(payment)
            assertEquals(payment, awaitItem())
            val updated = payment.copy(status = PaymentStatus.DECLINED)
            source.upsert(updated)
            assertEquals(updated, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
