package com.gkcontas.modulith.payment.internal;

import com.gkcontas.modulith.payment.PaymentManagement;
import com.gkcontas.modulith.payment.PaymentRecord;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
class PaymentController {

    private final PaymentManagement payments;

    PaymentController(PaymentManagement payments) {
        this.payments = payments;
    }

    @GetMapping
    List<PaymentRecord> history() {
        return payments.history();
    }
}
