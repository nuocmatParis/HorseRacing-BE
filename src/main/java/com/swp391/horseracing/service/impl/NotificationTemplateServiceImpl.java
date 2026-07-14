package com.swp391.horseracing.service.impl;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.swp391.horseracing.dto.notification.NotificationMessage;
import com.swp391.horseracing.entity.NotificationEvent;
import com.swp391.horseracing.exception.AppException;
import com.swp391.horseracing.exception.ErrorCode;
import com.swp391.horseracing.service.NotificationPolicyService;
import com.swp391.horseracing.service.NotificationTemplateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationTemplateServiceImpl implements NotificationTemplateService {
    ObjectMapper objectMapper;
    NotificationPolicyService policyService;

    @Override
    public NotificationMessage build(NotificationEvent event) {
        Map<String, Object> payload = readPayload(event.getPayloadJson());
        String title;
        String content;
        switch (event.getEventType()) {
            case TOURNAMENT_PUBLISHED -> {
                title = "Giải đấu mới đã được công bố";
                content = "Giải đấu " + value(payload, "tournamentName") + " đã được công bố.";
            }
            case REGISTRATION_APPROVED -> {
                title = "Đăng ký đã được duyệt";
                content = "Đăng ký tham gia " + value(payload, "tournamentName") + " của bạn đã được duyệt.";
            }
            case REGISTRATION_REJECTED -> {
                title = "Đăng ký bị từ chối";
                content = "Đăng ký tham gia " + value(payload, "tournamentName") + " bị từ chối. Lý do: " + value(payload, "reason") + ".";
            }
            case REGISTRATION_WITHDRAWN -> {
                title = "Đăng ký đã được rút";
                content = "Đăng ký tại " + value(payload, "tournamentName") + " đã được rút. Lý do: " + value(payload, "reason") + ".";
            }
            case CONTRACT_INVITED -> {
                title = "Bạn nhận được lời mời hợp đồng";
                content = "Bạn được mời thi đấu cùng ngựa " + value(payload, "horseName") + " tại " + value(payload, "tournamentName") + ".";
            }
            case CONTRACT_ACCEPTED -> {
                title = "Jockey đã chấp nhận hợp đồng";
                content = "Jockey " + value(payload, "jockeyName") + " đã chấp nhận hợp đồng cho ngựa " + value(payload, "horseName") + ".";
            }
            case CONTRACT_REJECTED -> {
                title = "Hợp đồng bị từ chối";
                content = "Hợp đồng cho ngựa " + value(payload, "horseName") + " bị từ chối. Lý do: " + value(payload, "reason") + ".";
            }
            case CONTRACT_APPROVED -> {
                title = "Hợp đồng đã được duyệt";
                content = "Hợp đồng cho ngựa " + value(payload, "horseName") + " đã được Admin duyệt.";
            }
            case CONTRACT_CANCELLED -> {
                title = "Hợp đồng đã bị hủy";
                content = "Hợp đồng cho ngựa " + value(payload, "horseName") + " đã bị hủy. Lý do: " + value(payload, "reason") + ".";
            }
            case SCHEDULE_PUBLISHED -> {
                title = "Lịch thi đấu đã được công bố";
                content = "Lịch thi đấu của " + value(payload, "tournamentName") + " đã được công bố.";
            }
            case RACE_RESCHEDULED -> {
                title = "Lịch race đã thay đổi";
                content = "Race " + value(payload, "raceName") + " được chuyển từ "
                        + value(payload, "oldStartTime") + " sang " + value(payload, "newStartTime")
                        + ". Lý do: " + value(payload, "reason") + ".";
            }
            case RACE_CANCELLED -> {
                title = "Race đã bị hủy";
                content = "Race " + value(payload, "raceName") + " đã bị hủy. Lý do: " + value(payload, "reason") + ".";
            }
            case RACE_STARTED -> {
                title = "Race đã bắt đầu";
                content = "Race " + value(payload, "raceName") + " đã bắt đầu.";
            }
            case ENTRY_SCRATCHED, PREDICTED_ENTRY_SCRATCHED -> {
                title = "Ngựa đã bị loại khỏi race";
                content = "Ngựa " + value(payload, "horseName") + " đã bị scratched khỏi race " + value(payload, "raceName") + ".";
            }
            case HORSE_INSPECTION_FAILED -> {
                title = "Ngựa không đạt kiểm tra";
                content = "Ngựa " + value(payload, "horseName") + " không đạt kiểm tra trước race " + value(payload, "raceName") + ".";
            }
            case JOCKEY_INSPECTION_FAILED -> {
                title = "Jockey không đạt kiểm tra";
                content = "Jockey " + value(payload, "jockeyName") + " không đạt kiểm tra trước race " + value(payload, "raceName") + ".";
            }
            case RACE_RESULT_PUBLISHED -> {
                title = "Kết quả race đã được công bố";
                content = "Kết quả chính thức của race " + value(payload, "raceName") + " đã được công bố.";
            }
            case PREDICTION_SCORED -> {
                title = "Dự đoán đã được chấm điểm";
                content = "Dự đoán của bạn nhận được " + value(payload, "points") + " điểm.";
            }
            case PREDICTION_VOIDED -> {
                title = "Dự đoán đã bị vô hiệu";
                content = "Dự đoán của bạn đã bị vô hiệu. Lý do: " + value(payload, "reason") + ".";
            }
            case PRIZE_RECEIVED -> {
                title = "Tiền thưởng đã được chuyển";
                content = "Tiền thưởng race " + value(payload, "raceName") + " đã được chuyển vào ví.";
            }
            case JOCKEY_PAYOUT_RELEASED -> {
                title = "Khoản thanh toán Jockey đã được giải ngân";
                content = "Khoản thanh toán còn lại của hợp đồng đã được chuyển vào ví Jockey.";
            }
            case ROUND_TRANSITION_BLOCKED -> {
                title = "Round transition is blocked";
                content = "Round " + value(payload, "roundName")
                        + " cannot advance because at least one race has fewer than four eligible finishers.";
            }
            default -> throw new AppException(ErrorCode.NOTIFICATION_EVENT_INVALID);
        }
        return new NotificationMessage(title, content, policyService.showToast(event.getEventType()));
    }

    private Map<String, Object> readPayload(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new AppException(ErrorCode.NOTIFICATION_PAYLOAD_INVALID);
        }
    }

    private String value(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }
}
