package com.had.downloader.ui.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QRCodeGenerator {
    fun generateQRCode(content: String, width: Int = 400, height: Int = 400, margin: Int = 0): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, null)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val outputWidth = matrixWidth + 2 * margin
            val outputHeight = matrixHeight + 2 * margin
            val pixels = IntArray(outputWidth * outputHeight)
            for (y in 0 until outputHeight) {
                for (x in 0 until outputWidth) {
                    val px = if (x in margin until matrixWidth + margin && y in margin until matrixHeight + margin) {
                        val bit = bitMatrix.get(x - margin, y - margin)
                        if (bit) Color.BLACK else Color.WHITE
                    } else {
                        Color.WHITE
                    }
                    pixels[y * outputWidth + x] = px
                }
            }
            Bitmap.createBitmap(pixels, outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateQRCodeWithColor(
        content: String,
        width: Int = 400,
        height: Int = 400,
        foreground: Int = Color.BLACK,
        background: Int = Color.WHITE,
        margin: Int = 0
    ): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, null)
            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val outputWidth = matrixWidth + 2 * margin
            val outputHeight = matrixHeight + 2 * margin
            val pixels = IntArray(outputWidth * outputHeight)
            for (y in 0 until outputHeight) {
                for (x in 0 until outputWidth) {
                    val px = if (x in margin until matrixWidth + margin && y in margin until matrixHeight + margin) {
                        val bit = bitMatrix.get(x - margin, y - margin)
                        if (bit) foreground else background
                    } else {
                        background
                    }
                    pixels[y * outputWidth + x] = px
                }
            }
            Bitmap.createBitmap(pixels, outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}