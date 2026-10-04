package com.gkcontas.modulith.order.internal;

import com.gkcontas.modulith.order.OrderDetails;
import com.gkcontas.modulith.order.OrderManagement;
import com.gkcontas.modulith.order.PlaceOrder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * The web slice belongs to the module it serves, and it is internal.
 *
 * <p>No other module calls a controller, so there is no reason for it to sit in the public
 * package. Keeping each module's HTTP surface inside the module is also what makes the
 * module a unit that could be lifted out later: its data, its rules and its endpoints
 * travel together.
 */
@RestController
@RequestMapping("/orders")
class OrderController {

    private final OrderManagement orders;

    OrderController(OrderManagement orders) {
        this.orders = orders;
    }

    record PlaceOrderRequest(
            @NotBlank(message = "must not be blank") String sku,
            @Min(value = 1, message = "must be at least 1") int quantity,
            @NotNull(message = "must not be null")
            @DecimalMin(value = "0.01", message = "must be greater than zero") BigDecimal amount,
            @Email(message = "must be a valid e-mail address")
            @NotBlank(message = "must not be blank") String customerEmail) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    OrderDetails place(@Valid @RequestBody PlaceOrderRequest request) {
        // 202, not 201: the order exists, and whether it will be confirmed depends on
        // listeners that have not run yet. Answering 201 would promise a finished flow.
        return orders.place(new PlaceOrder(request.sku(), request.quantity(),
                request.amount(), request.customerEmail()));
    }

    @GetMapping("/{id}")
    OrderDetails byId(@PathVariable UUID id) {
        return orders.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No order with id " + id));
    }

    @GetMapping
    List<OrderDetails> all() {
        return orders.findAll();
    }
}
