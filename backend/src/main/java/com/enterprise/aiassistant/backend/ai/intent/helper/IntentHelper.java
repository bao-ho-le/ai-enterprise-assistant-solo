package com.enterprise.aiassistant.backend.ai.intent.helper;

import com.enterprise.aiassistant.backend.ai.intent.enums.Intent;
import org.springframework.stereotype.Component;

import java.util.Locale;

import static com.enterprise.aiassistant.backend.ai.intent.service.IntentClassifierImpl.FALLBACK_INTENT;

@Component
public class IntentHelper {

    // Model đôi khi bọc giá trị trong dấu nháy hoặc thêm xuống dòng, nên chuẩn hoá
    // trước khi parse; chỉ chấp nhận đúng tên enum, không so khớp chuỗi con.
    public Intent parseIntent(String rawIntent) {

        if (rawIntent == null) {
            return FALLBACK_INTENT;
        }

        String normalized = rawIntent
                .replace("\"", "")
                .replace("'", "")
                .replace("`", "")
                .trim()
                .toUpperCase(Locale.ROOT);

        try {
            return Intent.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return FALLBACK_INTENT;
        }
    }
}
