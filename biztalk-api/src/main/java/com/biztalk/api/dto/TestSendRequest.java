package com.biztalk.api.dto;

import com.biztalk.core.domain.ChannelType;

public record TestSendRequest(
    String userId,
    ChannelType channel,
    String receiverNum,
    String content
) {}
