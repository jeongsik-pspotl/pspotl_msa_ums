package com.biztalk.core.domain;

import java.time.LocalDateTime;

public record BizTalkMessage(
    String messageId,
    String userId,
    ChannelType channel,
    String receiverNum,
    String content,
    int retryCount,
    LocalDateTime createdAt
) {}
