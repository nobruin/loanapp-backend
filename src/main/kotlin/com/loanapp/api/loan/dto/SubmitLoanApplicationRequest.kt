package com.loanapp.api.loan.dto

import java.math.BigDecimal

data class SubmitLoanApplicationRequest(
    val amount: BigDecimal,
    val termMonths: Int,
    val purpose: String? = null,
)