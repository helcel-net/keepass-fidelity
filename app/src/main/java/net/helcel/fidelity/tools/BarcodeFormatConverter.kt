package net.helcel.fidelity.tools

import com.google.zxing.BarcodeFormat

object BarcodeFormatConverter {

    fun stringToFormat(f: String): BarcodeFormat {
        return when (f) {
            "CODE_128" -> BarcodeFormat.CODE_128
            "CODE_39" -> BarcodeFormat.CODE_39
            "CODE_93" -> BarcodeFormat.CODE_93
            "EAN_8" -> BarcodeFormat.EAN_8
            "EAN_13" -> BarcodeFormat.EAN_13
            "CODE_QR" -> BarcodeFormat.QR_CODE
            "UPC_A" -> BarcodeFormat.UPC_A
            "UPC_E" -> BarcodeFormat.UPC_E
            "PDF_417" -> BarcodeFormat.PDF_417
            "AZTEC" -> BarcodeFormat.AZTEC
            "CODABAR" -> BarcodeFormat.CODABAR
            "MAXICODE" -> BarcodeFormat.MAXICODE
            "DATA_MATRIX" -> BarcodeFormat.DATA_MATRIX
            "ITF" -> BarcodeFormat.ITF
            "RSS_14" -> BarcodeFormat.RSS_14
            "RSS_EXPANDED" -> BarcodeFormat.RSS_EXPANDED
            "UPC_EAN" -> BarcodeFormat.UPC_EAN_EXTENSION
            else -> throw Exception("Unsupported Format: $f")


        }
    }

    fun formatToString(f: BarcodeFormat): String {
        return when (f) {
            BarcodeFormat.CODE_39 -> "CODE_39"
            BarcodeFormat.CODE_93 -> "CODE_93"
            BarcodeFormat.CODE_128 -> "CODE_128"
            BarcodeFormat.EAN_8 -> "EAN_8"
            BarcodeFormat.EAN_13 -> "EAN_13"
            BarcodeFormat.QR_CODE -> "CODE_QR"
            BarcodeFormat.UPC_A -> "UPC_A"
            BarcodeFormat.UPC_E -> "UPC_E"
            BarcodeFormat.PDF_417 -> "PDF_417"
            BarcodeFormat.AZTEC -> "AZTEC"
            BarcodeFormat.CODABAR -> "CODABAR"
            BarcodeFormat.MAXICODE -> "MAXICODE"
            BarcodeFormat.DATA_MATRIX -> "DATA_MATRIX"
            BarcodeFormat.ITF -> "ITF"
            BarcodeFormat.RSS_14 -> "RSS_14"
            BarcodeFormat.RSS_EXPANDED -> "RSS_EXPANDED"
            BarcodeFormat.UPC_EAN_EXTENSION -> "UPC_EAN"
            //else -> throw Exception("Unsupported Format: $f")
        }
    }
}
