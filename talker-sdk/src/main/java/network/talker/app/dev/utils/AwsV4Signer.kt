package network.talker.app.dev.utils

import com.amazonaws.util.BinaryUtils
import com.amazonaws.util.DateUtils
import com.google.common.collect.ImmutableMap
import com.google.common.hash.Hashing
import org.apache.commons.lang3.StringUtils
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.Date
import java.util.Optional
import java.util.StringJoiner
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Suppress("SpellCheckingInspection")
internal object AwsV4Signer {
    fun sign(
        uri: URI, accessKey: String, secretKey: String,
        sessionToken: String, wssUri: URI, region: String,
        dateMilli: Long
    ): URI {
        val amzDate = getTimeStamp(dateMilli)
        val datestamp = getDateStamp(dateMilli)
        val queryParamsMap =
            buildQueryParamsMap(uri, accessKey, sessionToken, region, amzDate, datestamp)
        val canonicalQuerystring = getCanonicalizedQueryString(queryParamsMap)
        val canonicalRequest = getCanonicalRequest(uri, canonicalQuerystring)
        val stringToSign =
            signString(amzDate, createCredentialScope(region, datestamp), canonicalRequest)
        val signatureKey =
            getSignatureKey(secretKey, datestamp, region, AwsV4SignerConstants.SERVICE)
        val signature = BinaryUtils.toHex(hmacSha256(stringToSign, signatureKey))
        val signedCanonicalQueryString =
            canonicalQuerystring + "&" + AwsV4SignerConstants.X_AMZ_SIGNATURE + "=" + signature
        return URI.create(
            wssUri.scheme + "://" + wssUri.host + "/?" + getCanonicalUri(uri).substring(
                1
            ) + signedCanonicalQueryString
        )
    }

    fun sign(
        uri: URI, accessKey: String, secretKey: String,
        sessionToken: String, region: String, dateMillis: Long
    ): URI {
        val wssUri = URI.create("wss://" + uri.host)
        return sign(uri, accessKey, secretKey, sessionToken, wssUri, region, dateMillis)
    }

    private fun buildQueryParamsMap(
        uri: URI,
        accessKey: String,
        sessionToken: String?,
        region: String?,
        amzDate: String,
        datestamp: String?
    ): Map<String, String> {
        val queryParamsBuilder = ImmutableMap.builder<String, String>()
            .put(
                AwsV4SignerConstants.X_AMZ_ALGORITHM,
                AwsV4SignerConstants.ALGORITHM_AWS4_HMAC_SHA_256
            )
            .put(
                AwsV4SignerConstants.X_AMZ_CREDENTIAL,
                urlEncode(accessKey + "/" + createCredentialScope(region, datestamp))
            )
            .put(AwsV4SignerConstants.X_AMZ_DATE, amzDate)
            .put(AwsV4SignerConstants.X_AMZ_EXPIRES, "299")
            .put(AwsV4SignerConstants.X_AMZ_SIGNED_HEADERS, AwsV4SignerConstants.SIGNED_HEADERS)
        if (StringUtils.isNotEmpty(sessionToken)) {
            queryParamsBuilder.put(
                AwsV4SignerConstants.X_AMZ_SECURITY_TOKEN,
                urlEncode(sessionToken)
            )
        }
        if (StringUtils.isNotEmpty(uri.query)) {
            val params =
                uri.query.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (param in params) {
                val index = param.indexOf('=')
                if (index > 0) {
                    val paramKey = param.substring(0, index)
                    val paramValue = urlEncode(param.substring(index + 1))
                    queryParamsBuilder.put(paramKey, paramValue)
                }
            }
        }
        return queryParamsBuilder.build()
    }

    private fun getCanonicalizedQueryString(queryParamsMap: Map<String, String>): String {
        val queryKeys: List<String> = ArrayList(queryParamsMap.keys)
        Collections.sort(queryKeys)
        val builder = StringBuilder()
        for (i in queryKeys.indices) {
            builder.append(queryKeys[i]).append("=").append(queryParamsMap[queryKeys[i]])
            if (queryKeys.size - 1 > i) {
                builder.append("&")
            }
        }
        return builder.toString()
    }

    private fun createCredentialScope(region: String?, datestamp: String?): String {
        return StringJoiner("/")
            .add(datestamp)
            .add(region)
            .add(AwsV4SignerConstants.SERVICE)
            .add(AwsV4SignerConstants.AWS4_REQUEST_TYPE)
            .toString()
    }

    private fun getCanonicalRequest(uri: URI, canonicalQuerystring: String?): String {
        val payloadHash =
            Hashing.sha256().hashString(StringUtils.EMPTY, StandardCharsets.UTF_8).toString()
        val canonicalUri = getCanonicalUri(uri)
        val canonicalHeaders = "host:" + uri.host + AwsV4SignerConstants.NEW_LINE_DELIMITER
        return StringJoiner(AwsV4SignerConstants.NEW_LINE_DELIMITER)
            .add(AwsV4SignerConstants.METHOD)
            .add(canonicalUri)
            .add(canonicalQuerystring)
            .add(canonicalHeaders)
            .add(AwsV4SignerConstants.SIGNED_HEADERS)
            .add(payloadHash)
            .toString()
    }

    private fun getCanonicalUri(uri: URI): String {
        return Optional.of(uri.path)
            .filter { s: String? -> !StringUtils.isEmpty(s) }
            .orElse("/")
    }

    private fun signString(amzDate: String?, credentialScope: String?, canonicalRequest: String?): String {
        return StringJoiner(AwsV4SignerConstants.NEW_LINE_DELIMITER)
            .add(AwsV4SignerConstants.ALGORITHM_AWS4_HMAC_SHA_256)
            .add(amzDate)
            .add(credentialScope)
            .add(Hashing.sha256().hashString(canonicalRequest, StandardCharsets.UTF_8).toString())
            .toString()
    }

    private fun urlEncode(str: String?): String {
        try {
            return URLEncoder.encode(str, StandardCharsets.UTF_8.name())
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message, e)
        }
    }

    private fun hmacSha256(data: String, key: ByteArray?): ByteArray {
        val algorithm = "HmacSHA256"
        val mac: Mac
        try {
            mac = Mac.getInstance(algorithm)
            mac.init(SecretKeySpec(key, algorithm))
            return mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
        } catch (e: Exception) {
            throw IllegalArgumentException(e.message, e)
        }
    }

    private fun getSignatureKey(
        key: String,
        dateStamp: String,
        regionName: String,
        serviceName: String
    ): ByteArray {
        val kSecret = "AWS4$key".toByteArray(StandardCharsets.UTF_8)
        val kDate = hmacSha256(dateStamp, kSecret)
        val kRegion = hmacSha256(regionName, kDate)
        val kService = hmacSha256(serviceName, kRegion)
        return hmacSha256(AwsV4SignerConstants.AWS4_REQUEST_TYPE, kService)
    }

    private fun getTimeStamp(dateMilli: Long): String {
        return DateUtils.format(AwsV4SignerConstants.TIME_PATTERN, Date(dateMilli))
    }

    private fun getDateStamp(dateMilli: Long): String {
        return DateUtils.format(AwsV4SignerConstants.DATE_PATTERN, Date(dateMilli))
    }
}