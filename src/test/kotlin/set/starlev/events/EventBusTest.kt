package set.starlev.events

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Юнит-тесты для [EventBus].
 *
 * Покрывает:
 * — Базовая подписка и публикация.
 * — Порядок вызова по приоритету.
 * — Отмена события (Cancellable) прерывает дальнейшие обработчики.
 * — Unsubscribe убирает подписку.
 * — clear() удаляет все подписки.
 * — Ошибка в одном handler'е не ломает остальные.
 *
 * Каждый тест чистит реестр в [tearDown], чтобы не влиять на другие тесты.
 */
class EventBusTest {

    @After
    fun tearDown() {
        EventBus.clear()
    }

    @Test
    fun `subscribe and post — handler called exactly once`() {
        var called = 0
        EventBus.subscribe<TestEvent> { _ -> called++ }

        EventBus.post(TestEvent("hello"))

        assertEquals("Handler должен быть вызван 1 раз", 1, called)
    }

    @Test
    fun `multiple posts — handler called for each post`() {
        var called = 0
        EventBus.subscribe<TestEvent> { _ -> called++ }

        EventBus.post(TestEvent("a"))
        EventBus.post(TestEvent("b"))
        EventBus.post(TestEvent("c"))

        assertEquals("Handler должен быть вызван 3 раза", 3, called)
    }

    @Test
    fun `post without subscribers — no exception`() {
        // Не должно бросить NoSuchElementException или подобное.
        EventBus.post(TestEvent("nobody listens"))
    }

    @Test
    fun `priority order — lower priority runs first`() {
        val calls = mutableListOf<String>()
        EventBus.subscribe<TestEvent>(priority = 0) { _ -> calls.add("normal") }
        EventBus.subscribe<TestEvent>(priority = -10) { _ -> calls.add("high") }
        EventBus.subscribe<TestEvent>(priority = 10) { _ -> calls.add("low") }

        EventBus.post(TestEvent("x"))

        assertEquals(
            "Handlers должны вызываться по приоритету (меньше = раньше)",
            listOf("high", "normal", "low"),
            calls
        )
    }

    @Test
    fun `priority insertion — stable ordering for same priority`() {
        val calls = mutableListOf<String>()
        EventBus.subscribe<TestEvent>(priority = 0) { _ -> calls.add("first") }
        EventBus.subscribe<TestEvent>(priority = 0) { _ -> calls.add("second") }
        EventBus.subscribe<TestEvent>(priority = 0) { _ -> calls.add("third") }

        EventBus.post(TestEvent("x"))

        // При равном приоритете сохраняется порядок вставки.
        assertEquals(listOf("first", "second", "third"), calls)
    }

    @Test
    fun `cancelled event — stops further handlers`() {
        val calls = mutableListOf<String>()
        EventBus.subscribe<TestCancellableEvent>(priority = -10) { event ->
            event.cancel()
            calls.add("first")
        }
        EventBus.subscribe<TestCancellableEvent>(priority = 0) { _ -> calls.add("second") }
        EventBus.subscribe<TestCancellableEvent>(priority = 10) { _ -> calls.add("third") }

        EventBus.post(TestCancellableEvent())

        assertEquals(
            "После cancel() остальные handlers не должны вызываться",
            listOf("first"),
            calls
        )
    }

    @Test
    fun `cancelled flag — isCancelled returns true after cancel`() {
        val event = TestCancellableEvent()
        assertFalse("Свежее событие не должно быть cancelled", event.isCancelled())
        event.cancel()
        assertTrue("После cancel() событие должно быть cancelled", event.isCancelled())
    }

    @Test
    fun `unsubscribe — handler no longer called`() {
        var called = false
        val sub = EventBus.subscribe<TestEvent> { _ -> called = true }
        sub.unsubscribe()

        EventBus.post(TestEvent("x"))

        assertFalse("Handler не должен быть вызван после unsubscribe", called)
    }

    @Test
    fun `unsubscribe only this handler — others still work`() {
        var firstCalled = false
        var secondCalled = false
        val sub1 = EventBus.subscribe<TestEvent> { _ -> firstCalled = true }
        EventBus.subscribe<TestEvent> { _ -> secondCalled = true }

        sub1.unsubscribe()
        EventBus.post(TestEvent("x"))

        assertFalse("first не должен быть вызван", firstCalled)
        assertTrue("second должен быть вызван", secondCalled)
    }

    @Test
    fun `clear all — no subscribers remain`() {
        var called = false
        EventBus.subscribe<TestEvent> { _ -> called = true }
        EventBus.clear()

        EventBus.post(TestEvent("x"))

        assertFalse("После clear() handler не должен быть вызван", called)
        assertEquals(
            "После clear() реестр должен быть пуст",
            0,
            EventBus.subscriberCount(TestEvent::class.java)
        )
    }

    @Test
    fun `clear specific event type — other types unaffected`() {
        var testCalled = false
        var otherCalled = false
        EventBus.subscribe<TestEvent> { _ -> testCalled = true }
        EventBus.subscribe<OtherEvent> { _ -> otherCalled = true }

        EventBus.clear(TestEvent::class.java)
        EventBus.post(TestEvent("x"))
        EventBus.post(OtherEvent())

        assertFalse("TestEvent handler не должен быть вызван", testCalled)
        assertTrue("OtherEvent handler должен быть вызван", otherCalled)
    }

    @Test
    fun `subscriber count — tracks registered handlers`() {
        EventBus.subscribe<TestEvent> { _ -> /* handler 1 */ }
        EventBus.subscribe<TestEvent> { _ -> /* handler 2 */ }
        EventBus.subscribe<OtherEvent> { _ -> /* handler 3 */ }

        assertEquals(2, EventBus.subscriberCount(TestEvent::class.java))
        assertEquals(1, EventBus.subscriberCount(OtherEvent::class.java))
    }

    @Test
    fun `error in handler — does not stop others`() {
        val calls = mutableListOf<String>()
        EventBus.subscribe<TestEvent>(priority = -10) { _ ->
            throw RuntimeException("Boom!")
        }
        EventBus.subscribe<TestEvent>(priority = 0) { _ -> calls.add("second") }
        EventBus.subscribe<TestEvent>(priority = 10) { _ -> calls.add("third") }

        // Должны быть вызваны второй и третий, несмотря на ошибку в первом.
        EventBus.post(TestEvent("x"))

        assertEquals(listOf("second", "third"), calls)
    }

    // ===== Тестовые события =====

    data class TestEvent(val data: String)

    class OtherEvent

    class TestCancellableEvent : Cancellable {
        override var cancelled: Boolean = false
    }
}