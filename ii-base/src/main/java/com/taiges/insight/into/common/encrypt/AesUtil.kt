package com.taiges.insight.into.common.encrypt

import android.util.Base64
import com.taiges.insight.into.common.tryBiz
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 *  AES GCM 加解密工具类
 *  使用安全的 GCM 模式替换原有的 ECB 模式
 *  注意：GCM 模式需要处理 IV，因此密文格式为 [IV + 实际密文]
 */
class AesUtil {

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ALGORITHM = "AES"

        // GCM 认证标签长度
        private const val GCM_TAG_LENGTH = 128

        // IV 长度（字节）
        private const val GCM_IV_LENGTH = 12

        /**
         * 加密
         * @param input 待加密的字符串
         * @param secretKey 密钥
         * @return Base64 编码的加密结果，格式为 [IV(12字节) + 实际密文]
         */
        fun encryptStr(input: String, secretKey: String): String {
            return encrypt(input.toByteArray(), secretKey)
        }

        /**
         * 加密
         * @param input 待加密的字节数组
         * @param secretKey 密钥（必须是 16/24/32 字节）
         * @return Base64 编码的加密结果，格式为 [IV(12字节) + 实际密文]
         */
        fun encrypt(input: ByteArray, secretKey: String): String {
            // 1. 生成随机 IV（初始化向量）
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)
            // 2. 创建 GCM 参数规范
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            // 3. 创建密钥规范
            val keySpec = SecretKeySpec(secretKey.toByteArray(), ALGORITHM)
            // 4. 初始化 cipher
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

            // 5. 执行加密
            val cipherText = cipher.doFinal(input)

            // 6. 组合 IV 和密文（IV 需要与密文一起存储）
            val combined = iv + cipherText

            // 7. Base64 编码返回
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        }

        /**
         * 解密
         * @param input Base64 编码的密文，格式为 [IV(12字节) + 实际密文]
         * @param secretKey 密钥（必须是 16/24/32 字节）
         * @return 解密后的字符串
         */
        fun decryptStr(input: String, secretKey: String): String? {
            tryBiz {
                // 1. Base64 解码
                val decoded = Base64.decode(input, Base64.NO_WRAP)

                // 2. 检查数据长度是否足够包含 IV
                require(decoded.size >= GCM_IV_LENGTH) {
                    "The ciphertext is not long enough to extract IV!"
                }

                // 3. 分离 IV 和实际密文
                val iv = decoded.sliceArray(0 until GCM_IV_LENGTH)
                val cipherText = decoded.sliceArray(GCM_IV_LENGTH until decoded.size)

                // 4. 创建 GCM 参数规范
                val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

                // 5. 创建密钥规范
                val keySpec = SecretKeySpec(secretKey.toByteArray(), ALGORITHM)

                // 6. 初始化 cipher
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

                // 7. 执行解密
                val plainText = cipher.doFinal(cipherText)

                // 8. 返回解密结果
                return String(plainText)
            }
            return null
        }
    }
}