package com.budgetwise.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A single income or expense entry. */
public class Transaction {
    public int transactionId;
    public int categoryId;
    public String categoryName;
    public String transactionType;
    public BigDecimal amount;
    public LocalDate transactionDate;
    public String description;
}
