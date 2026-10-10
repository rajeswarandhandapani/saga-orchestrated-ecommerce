package com.rajeswaran.ecommerce.contracts.order;

import com.rajeswaran.ecommerce.contracts.Command;

public sealed interface OrderCommand extends Command permits ConfirmOrder, RejectOrder, CancelOrder {
}
