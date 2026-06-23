package com.swp391.horseracing.controller;

import com.swp391.horseracing.dto.common.ApiResponse;
import com.swp391.horseracing.dto.invoice.response.InvoiceResponse;
import com.swp391.horseracing.dto.invoice.response.PaymentResponse;
import com.swp391.horseracing.service.InvoiceService;
import com.swp391.horseracing.service.PaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/invoices")
public class InvoiceController {
    InvoiceService invoiceService;
    PaymentService paymentService;

    @GetMapping("/my-invoices")
    public ApiResponse<List<InvoiceResponse>> getMyInvoices(){
        return ApiResponse.<List<InvoiceResponse>>builder()
                .result(invoiceService.getMyInvoices())
                .build();
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<PaymentResponse> payInvoice(@PathVariable UUID id){
        return ApiResponse.<PaymentResponse>builder()
                .message("Pay invoice successfully")
                .result(paymentService.payInvoice(id))
                .build();

    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PaymentResponse> refundInvoice(@PathVariable UUID id){
        return ApiResponse.<PaymentResponse>builder()
                .message("Refund invoice successfully")
                .result(paymentService.refundInvoice(id))
                .build();
    }
}
