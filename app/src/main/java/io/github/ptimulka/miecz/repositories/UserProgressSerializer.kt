package io.github.ptimulka.miecz.repositories

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import io.github.ptimulka.miecz.data.UserProgress
import java.io.InputStream
import java.io.OutputStream

object UserProgressSerializer : Serializer<UserProgress> {
    override val defaultValue: UserProgress = UserProgress.newBuilder()
        .setCurrentSectionId(1)
        .setShieldsCount(5)
        .build()

    override suspend fun readFrom(input: InputStream): UserProgress {
        try {
            return UserProgress.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: UserProgress, output: OutputStream) = t.writeTo(output)
}
