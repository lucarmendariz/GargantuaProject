package com.example.trazabilidadapi.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LotHistory {
    private int historyId;
    private int lotId;
    private String actionType;
    private String operator;
    private LocalDateTime actionTimestamp;
    private BigDecimal quantityAffected;
    private String ocrRawText;
    private String observations;
}