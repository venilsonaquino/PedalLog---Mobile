package com.pedallog.app.data.mapper

import com.pedallog.app.domain.model.PedalPoint
import com.pedallog.app.domain.model.PedalSession
import com.pedallog.app.domain.model.SessionId
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import java.io.BufferedReader

object GzipCsvUtils {

    fun decompressAndParsePoints(compressedData: ByteArray): List<PedalPoint> {
        val decompressedString = decompress(compressedData)
        return WatchPointCsvParser.parse(decompressedString)
    }

    fun decompressAndParse(compressedData: ByteArray): Pair<PedalSession, List<PedalPoint>> {
        val decompressedString = decompress(compressedData)
        return LegacySessionCsvParser.parse(decompressedString)
    }

    private fun decompress(compressedData: ByteArray): String {
        return try {
            val byteArrayInputStream = ByteArrayInputStream(compressedData)
            val gzipInputStream = GZIPInputStream(byteArrayInputStream)
            val reader = BufferedReader(InputStreamReader(gzipInputStream, "UTF-8"))
            val text = reader.readText()
            reader.close()
            gzipInputStream.close()
            text
        } catch (exception: Exception) {
            exception.printStackTrace()
            ""
        }
    }
}
