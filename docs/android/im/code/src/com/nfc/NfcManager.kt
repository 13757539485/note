package com.hfc.nfc_base

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcBarcode
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

object NfcManager {
    private val TAG = "NfcManager"
    private val ndefFilter =
        IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply { addDataType("*/*") }
    private val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED).apply {
        addCategory("android.intent.category.DEFAULT")
    }
    private val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
    private val intentFiltersArray = arrayOf(ndefFilter, techFilter, tagFilter)
    private val techListsArray = arrayOf(
        arrayOf<String>(MifareClassic::class.java.name),
        arrayOf<String>(NdefFormatable::class.java.name),
        arrayOf<String>(NfcF::class.java.name),
        arrayOf<String>(NfcA::class.java.name),
        arrayOf<String>(NfcB::class.java.name),
        arrayOf<String>(NfcV::class.java.name),
        arrayOf<String>(IsoDep::class.java.name),
        arrayOf<String>(MifareUltralight::class.java.name),
        arrayOf<String>(NfcBarcode::class.java.name),
        arrayOf<String>(Ndef::class.java.name),
    )
    private var nfcAdapter: NfcAdapter? = null

    private fun checkAdapter(activity: Activity): Boolean {
        if (nfcAdapter == null) {
            nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        }
        if (nfcAdapter == null) {
            Toast.makeText(activity, "设备不支持NFC", Toast.LENGTH_SHORT).show()
            activity.finish()
            return false
        }
        if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(activity, "NFC未开启，请开启NFC功能", Toast.LENGTH_SHORT).show()
            val intent = Intent(Settings.ACTION_NFC_SETTINGS)
            activity.startActivity(intent)
            return false
        }
        return true
    }

    fun enableNfcReaderMode(activity: Activity, block: (tag: NdefMessage?) -> Unit) {
        if (!checkAdapter(activity)) return
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_BARCODE
        nfcAdapter?.enableReaderMode(activity, { tag ->
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                try {
                    Log.e(TAG, "enableNfcReaderMode: ${ndef.ndefMessage} ${ndef.cachedNdefMessage}")
                    block(ndef.cachedNdefMessage)
                } finally {
                    ndef.close()
                }
            } else {
                Log.e(TAG, "only support read ndef")
            }

        }, flags, null)
    }

    fun enableNfcDispatcher(activity: Activity) {
        if (!checkAdapter(activity)) return
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            activity, 0, intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        nfcAdapter?.enableForegroundDispatch(
            activity,
            pendingIntent,
            intentFiltersArray,
            techListsArray
        )
    }

    fun disableNfcReaderMode(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }

    fun disableNfcDispatcher(activity: Activity) {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    fun parseNdefMessagesToText(message: NdefMessage, block: (content: String) -> Unit) {
        message.records.forEach { record ->
            if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                record.type.contentEquals(NdefRecord.RTD_TEXT)
            ) {
                try {
                    block(readTextFromRecord(record))
                } catch (e: UnsupportedEncodingException) {
                    Log.e(TAG, "Unsupported Encoding", e)
                }
            } else {
                /* 可以在这里添加对其他类型NDEF记录的支持 */
                Log.e(TAG, "other type: ${record.type}" )
            }
        }
    }

    @Throws(UnsupportedEncodingException::class)
    private fun readTextFromRecord(record: NdefRecord): String {
        val payload = record.payload
        val textEncoding = if ((payload[0].toInt() and 0x80) == 0) "UTF-8" else "UTF-16"
        val languageSize = payload[0].toInt() and 0x3F
        return String(
            payload, languageSize + 1,
            payload.size - languageSize - 1, Charset.forName(textEncoding)
        )
    }
}