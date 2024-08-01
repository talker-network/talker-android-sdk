package com.dev.talkersdk.utils

object AwsV4SignerConstants {
    const val ALGORITHM_AWS4_HMAC_SHA_256: String = "AWS4-HMAC-SHA256"
    const val AWS4_REQUEST_TYPE: String = "aws4_request"
    const val SERVICE: String = "kinesisvideo"
    const val X_AMZ_ALGORITHM: String = "X-Amz-Algorithm"
    const val X_AMZ_CREDENTIAL: String = "X-Amz-Credential"
    const val X_AMZ_DATE: String = "X-Amz-Date"
    const val X_AMZ_EXPIRES: String = "X-Amz-Expires"
    const val X_AMZ_SECURITY_TOKEN: String = "X-Amz-Security-Token"
    const val X_AMZ_SIGNATURE: String = "X-Amz-Signature"
    const val X_AMZ_SIGNED_HEADERS: String = "X-Amz-SignedHeaders"
    const val NEW_LINE_DELIMITER: String = "\n"
    const val DATE_PATTERN: String = "yyyyMMdd"
    const val TIME_PATTERN: String = "yyyyMMdd'T'HHmmss'Z'"
    const val METHOD: String = "GET"
    const val SIGNED_HEADERS: String = "host"
}
