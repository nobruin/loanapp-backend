package com.loanapp.api.loan

import com.fasterxml.jackson.databind.ObjectMapper
import com.loanapp.api.customer.dto.RegisterCustomerRequest
import com.loanapp.api.loan.dto.SubmitLoanApplicationRequest
import com.loanapp.shared.BaseIntegrationTest
import com.loanapp.support.JwtFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDate

class LoanControllerIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val objectMapper: ObjectMapper,
    ) : BaseIntegrationTest() {

        private fun registerCustomer(
            sub: String,
            cpf: String,
            email: String,
        ) = mockMvc.post("/v1/customers") {
            with(JwtFactory.withJwt(sub = sub, email = email))
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    RegisterCustomerRequest(
                        fullName = "Test User",
                        cpf = cpf,
                        birthDate = LocalDate.of(1990, 1, 1),
                        address = "123 Test St",
                    ),
                )
        }

        private fun submitLoan(
            sub: String = "auth0|loanTestUser",
            body: SubmitLoanApplicationRequest =
                SubmitLoanApplicationRequest(
                    amount = BigDecimal("5000.00"),
                    termMonths = 12,
                ),
        ) = mockMvc.post("/v1/loans/apply") {
            with(JwtFactory.withJwt(sub = sub))
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }

        @Test
        fun `should submit loan application successfully`() {
            val sub = "auth0|loanUser1"
            registerCustomer(sub = sub, cpf = "22233344455", email = "loan1@test.com")

            submitLoan(sub = sub).andExpect {
                status { isCreated() }
                jsonPath("$.id") { exists() }
                jsonPath("$.status") { value("PENDING") }
            }
        }

        @Test
        fun `should return 400 when customer does not exist`() {
            submitLoan(sub = "auth0|unknownUser").andExpect {
                status { isBadRequest() }
                jsonPath("$.message") { value("Customer not found") }
            }
        }

        @Test
        fun `should return 400 when amount is zero`() {
            val sub = "auth0|loanUser2"
            registerCustomer(sub = sub, cpf = "66677788800", email = "loan2@test.com")

            submitLoan(
                sub = sub,
                body = SubmitLoanApplicationRequest(amount = BigDecimal.ZERO, termMonths = 12),
            ).andExpect {
                status { isBadRequest() }
                jsonPath("$.message") { value("Loan amount must be greater than zero") }
            }
        }

        @Test
        fun `should return 400 when amount is negative`() {
            val sub = "auth0|loanUser3"
            registerCustomer(sub = sub, cpf = "33344455566", email = "loan3@test.com")

            submitLoan(
                sub = sub,
                body = SubmitLoanApplicationRequest(amount = BigDecimal("-500.00"), termMonths = 12),
            ).andExpect {
                status { isBadRequest() }
                jsonPath("$.message") { value("Loan amount must be greater than zero") }
            }
        }

        @Test
        fun `should return 400 when term in months is zero or negative`() {
            val sub = "auth0|loanUser4"
            registerCustomer(sub = sub, cpf = "77788899901", email = "loan4@test.com")

            submitLoan(
                sub = sub,
                body = SubmitLoanApplicationRequest(amount = BigDecimal("5000.00"), termMonths = 0),
            ).andExpect {
                status { isBadRequest() }
                jsonPath("$.message") { value("Term must be at least 1 month") }
            }
        }
    }