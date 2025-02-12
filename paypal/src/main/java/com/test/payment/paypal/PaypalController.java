package com.test.payment.paypal;


import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PaypalController {
    @Autowired
    private  PaypalService paypalService;

    @GetMapping("/")
    public String home() {
        return "index";
    }

    @PostMapping("/payment/create")
    public RedirectView createPayment(
            @RequestParam("method") String method,
            @RequestParam("amount") String amount,
            @RequestParam("currency") String currency,
            @RequestParam("description") String description,
            @RequestParam("email") String email
    ) {
        try {
            String cancelUrl = "http://localhost:8080/payment/cancel";
            String successUrl = "http://localhost:8080/payment/success";
            Payment payment = paypalService.createPayment(
                    Double.valueOf(amount),
                    currency,
                    method,
                    "sale",
                    description,
                    cancelUrl,
                    successUrl,
                    email
            );

            for (Links links: payment.getLinks()) {
                if (links.getRel().equals("approval_url")) {
                    return new RedirectView(links.getHref());
                }
            }
        } catch (PayPalRESTException e) {
            e.printStackTrace();
        }
        return new RedirectView("/payment/error");
    }

    @GetMapping("/payment/success")
    public String paymentSuccess(
            @RequestParam("paymentId") String paymentId,
            @RequestParam("PayerID") String payerId,
            Model model
    ) {
        try {
            Payment payment = paypalService.executePayment(paymentId, payerId);
            if (payment.getState().equals("approved")) {
                double totalAmount = 0.0;
                double singleAmount =0.0;
                String description = "";
                String email = "";
                // Ciclo  invece di for (int x=0;x<payment.getTransactions().size();x++)
                for (Transaction transaction : payment.getTransactions()) {
                    totalAmount += Double.parseDouble(transaction.getAmount().getTotal());
                    singleAmount = Double.parseDouble(transaction.getAmount().getTotal());
                    description=transaction.getDescription();
                    email=transaction.getPayee().getEmail();
                }
                model.addAttribute("paymentId", payment.getId());
                model.addAttribute("state", payment.getState());
                model.addAttribute("singleAmount", singleAmount);
                model.addAttribute("totalAmount", totalAmount); // Totale del pagamento
                model.addAttribute("currency", payment.getTransactions().get(0).getAmount().getCurrency());
                model.addAttribute("payerEmail", payment.getPayer().getPayerInfo().getEmail());
                model.addAttribute("createTime", payment.getCreateTime());
                model.addAttribute("transactions", payment.getTransactions());

                return "paymentSuccess";
            }
        } catch (PayPalRESTException e) {
            e.printStackTrace();
        }
        return "paymentSuccess";
    }

    @GetMapping("/payment/cancel")
    public String paymentCancel() {
        return "paymentCancel";
    }

    @GetMapping("/payment/error")
    public String paymentError() {
        return "paymentError";
    }
}
