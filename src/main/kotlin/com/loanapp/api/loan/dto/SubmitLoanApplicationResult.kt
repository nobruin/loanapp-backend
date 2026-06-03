package com.loanapp.api.loan.dto

import java.util.UUID

data class SubmitLoanApplicationResult(
    val id: UUID,
    val status: String,
)