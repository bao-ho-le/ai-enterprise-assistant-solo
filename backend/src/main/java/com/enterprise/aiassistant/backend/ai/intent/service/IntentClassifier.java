package com.enterprise.aiassistant.backend.ai.intent.service;

import com.enterprise.aiassistant.backend.ai.intent.enums.Intent;

public interface IntentClassifier {

    Intent classify(String message);
}
