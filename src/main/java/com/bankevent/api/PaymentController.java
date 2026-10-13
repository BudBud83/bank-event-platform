package com.bankevent.api;

import com.bankevent.api.dto.InitiatePaymentRequest;
import com.bankevent.api.dto.PaymentResponse;
import com.bankevent.payment.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    /**
     * Accepts a transfer. Returns immediately with status INITIATED.
     * Poll GET /api/payments/{id} until SETTLED, FAILED, or FLAGGED.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PaymentResponse initiate(@Valid @RequestBody InitiatePaymentRequest request) {
        return payments.initiate(request);
    }

    @GetMapping
    public List<PaymentResponse> list() {
        return payments.list();
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable String paymentId) {
        return payments.get(paymentId);
    }
}
