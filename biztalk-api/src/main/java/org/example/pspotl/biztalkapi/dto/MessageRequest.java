package org.example.pspotl.biztalkapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class MessageRequest {

    @NotBlank(message = "메시지 고유 ID(messageId)는 필수입니다.")
    private String messageId;

    @NotBlank(message = "수신자 번호(recipient)는 필수입니다.")
    private String recipient;

    @NotBlank(message = "메시지 내용(content)은 필수입니다.")
    @Size(max = 1000, message = "메시지 내용은 최대 1000자까지 허용됩니다.")
    private String content;
}