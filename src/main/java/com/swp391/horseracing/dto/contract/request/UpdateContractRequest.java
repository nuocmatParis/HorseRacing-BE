package com.swp391.horseracing.dto.contract.request;

import com.swp391.horseracing.enums.ContractStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateContractRequest {

    ContractStatus status;

    String rejectedReason;

    String cancelReason;

    String contractNote;
}
