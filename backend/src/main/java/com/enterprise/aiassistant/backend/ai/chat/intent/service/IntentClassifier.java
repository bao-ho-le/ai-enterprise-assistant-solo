package com.enterprise.aiassistant.backend.ai.chat.intent.service;

import com.enterprise.aiassistant.backend.ai.chat.intent.enums.Intent;

public interface IntentClassifier {

    Intent classify(String message);
}
