package com.fantasylibrary.android.data.repository

import com.fantasylibrary.android.data.network.ApiEnvelope
import com.fantasylibrary.android.data.network.Failure
import com.fantasylibrary.android.data.network.FailureKind
import com.fantasylibrary.android.data.network.Failures
import com.fantasylibrary.android.data.network.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement

/** 调用一个「data 必须有值」的接口。 */
internal suspend fun <T> callApi(
    label: String,
    block: suspend () -> ApiEnvelope<T>,
): Outcome<T> = try {
    val envelope = block()
    val failure = envelope.failureOrNull()
    when {
        failure != null -> Outcome.Err(failure)
        envelope.data != null -> Outcome.Ok(envelope.data)
        else -> Outcome.Err(
            Failure(FailureKind.BAD_RESPONSE, "$label：接口返回成功，但 data 为空"),
        )
    }
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Throwable) {
    Outcome.Err(Failures.fromThrowable(error))
}

/** 调用一个「只关心成功与否」的接口（login / logout 的 data 为 null）。 */
internal suspend fun callApiUnit(
    label: String,
    block: suspend () -> ApiEnvelope<JsonElement>,
): Outcome<Unit> = try {
    val envelope = block()
    val failure = envelope.failureOrNull()
    if (failure != null) Outcome.Err(failure) else Outcome.Ok(Unit)
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (error: Throwable) {
    Outcome.Err(Failures.fromThrowable(error))
}
