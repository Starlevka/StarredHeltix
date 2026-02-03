package set.starlev.utils

import kotlin.jvm.JvmName

/**
 * Расширения для исправления ошибок несоответствия типов в диапазонах (ClosedRange).
 * Позволяет проверять вхождение чисел разных типов (например, Int в диапазоне Byte).
 * Используем @JvmName для избежания конфликтов сигнатур JVM из-за стирания типов Generics.
 */

// Byte Range Extensions
@JvmName("containsShortInByteRange")
operator fun ClosedRange<Byte>.contains(value: Short): Boolean = value >= start && value <= endInclusive

@JvmName("containsIntInByteRange")
operator fun ClosedRange<Byte>.contains(value: Int): Boolean = value >= start && value <= endInclusive

@JvmName("containsLongInByteRange")
operator fun ClosedRange<Byte>.contains(value: Long): Boolean = value >= start && value <= endInclusive

@JvmName("containsFloatInByteRange")
operator fun ClosedRange<Byte>.contains(value: Float): Boolean = value >= start && value <= endInclusive

@JvmName("containsDoubleInByteRange")
operator fun ClosedRange<Byte>.contains(value: Double): Boolean = value >= start && value <= endInclusive

// Short Range Extensions
@JvmName("containsByteInShortRange")
operator fun ClosedRange<Short>.contains(value: Byte): Boolean = value >= start && value <= endInclusive

@JvmName("containsIntInShortRange")
operator fun ClosedRange<Short>.contains(value: Int): Boolean = value >= start && value <= endInclusive

@JvmName("containsLongInShortRange")
operator fun ClosedRange<Short>.contains(value: Long): Boolean = value >= start && value <= endInclusive

@JvmName("containsFloatInShortRange")
operator fun ClosedRange<Short>.contains(value: Float): Boolean = value >= start && value <= endInclusive

@JvmName("containsDoubleInShortRange")
operator fun ClosedRange<Short>.contains(value: Double): Boolean = value >= start && value <= endInclusive

// Int Range Extensions
@JvmName("containsByteInIntRange")
operator fun ClosedRange<Int>.contains(value: Byte): Boolean = value >= start && value <= endInclusive

@JvmName("containsShortInIntRange")
operator fun ClosedRange<Int>.contains(value: Short): Boolean = value >= start && value <= endInclusive

@JvmName("containsLongInIntRange")
operator fun ClosedRange<Int>.contains(value: Long): Boolean = value >= start && value <= endInclusive

@JvmName("containsFloatInIntRange")
operator fun ClosedRange<Int>.contains(value: Float): Boolean = value >= start && value <= endInclusive

@JvmName("containsDoubleInIntRange")
operator fun ClosedRange<Int>.contains(value: Double): Boolean = value >= start && value <= endInclusive

// Long Range Extensions
@JvmName("containsByteInLongRange")
operator fun ClosedRange<Long>.contains(value: Byte): Boolean = value >= start && value <= endInclusive

@JvmName("containsShortInLongRange")
operator fun ClosedRange<Long>.contains(value: Short): Boolean = value >= start && value <= endInclusive

@JvmName("containsIntInLongRange")
operator fun ClosedRange<Long>.contains(value: Int): Boolean = value >= start && value <= endInclusive

@JvmName("containsFloatInLongRange")
operator fun ClosedRange<Long>.contains(value: Float): Boolean = value >= start && value <= endInclusive

@JvmName("containsDoubleInLongRange")
operator fun ClosedRange<Long>.contains(value: Double): Boolean = value >= start && value <= endInclusive

// Float Range Extensions
@JvmName("containsDoubleInFloatRange")
operator fun ClosedRange<Float>.contains(value: Double): Boolean = value >= start && value <= endInclusive

// Double Range Extensions
@JvmName("containsIntInDoubleRange")
operator fun ClosedRange<Double>.contains(value: Int): Boolean = value >= start && value <= endInclusive
