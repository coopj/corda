package com.r3corda.protocols

import com.r3corda.core.contracts.Attachment
import com.r3corda.core.crypto.Party
import com.r3corda.core.crypto.SecureHash
import com.r3corda.core.crypto.sha256
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * Given a set of hashes either loads from from local storage  or requests them from the other peer. Downloaded
 * attachments are saved to local storage automatically.
 */
class FetchAttachmentsProtocol(requests: Set<SecureHash>,
                               otherSide: Party) : FetchDataProtocol<Attachment, ByteArray>(requests, otherSide) {

    companion object {
        const val TOPIC = "platform.fetch.attachment"
    }

    override val topic: String get() = TOPIC

    override fun load(txid: SecureHash): Attachment? = serviceHub.storageService.attachments.openAttachment(txid)

    override fun convert(wire: ByteArray): Attachment {
        return object : Attachment {
            override fun open(): InputStream = ByteArrayInputStream(wire)
            override val id: SecureHash = wire.sha256()
            override fun equals(other: Any?) = (other is Attachment) && other.id == id
            override fun hashCode(): Int = id.hashCode()
        }
    }

    override fun maybeWriteToDisk(downloaded: List<Attachment>) {
        for (attachment in downloaded) {
            serviceHub.storageService.attachments.importAttachment(attachment.open())
        }
    }
}