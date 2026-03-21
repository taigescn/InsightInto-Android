package com.taiges.insight.into.common.encrypt

import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher


/**
 * RSA 加密工具类
 */
class RsaUtil {

    companion object {
        private const val KEY_ALGORITHM = "RSA"
        private const val ECB_PKCS1_PADDING = "RSA/ECB/PKCS1Padding"

        /**
         * 根据公钥加密数据
         *
         * @param data
         * @param publicKey
         * @return
         */
        fun encrypt(data: ByteArray, publicKey: ByteArray): ByteArray {
            // 获取公钥
            val x509KeySpec = X509EncodedKeySpec(publicKey)
            return try {
                val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)
                val key = keyFactory.generatePublic(x509KeySpec)

                // 对数据加密
                val cipher = Cipher.getInstance(ECB_PKCS1_PADDING)
                cipher.init(Cipher.ENCRYPT_MODE, key)
                cipher.doFinal(data)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

}