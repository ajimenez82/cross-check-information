package com.crosscheck.application.contracts;

import com.crosscheck.application.model.ConversationReference;

/**
 * decode must verify integrity and format, throwing InvalidConversationReferenceException
 * for tampered or unreadable references. The handler checks expiration and revision.
 * encode must produce a non-empty opaque token using authenticated encryption.
 */
public interface ConversationReferenceCodec {
    ConversationReference decode(String token);
    String encode(ConversationReference reference);
}
