package dev.tradecore.domain.order;

public enum OrderStatus{
PENDING,
PARTIALLY_FILLED,
FILLED,
CANCELLED;

public boolean canTransitionTo(OrderStatus nextStatus){
    if (nextStatus ==null)return false;
    return switch(this){
        case PENDING -> nextStatus == CANCELLED || nextStatus == FILLED || nextStatus ==PARTIALLY_FILLED;
        case PARTIALLY_FILLED -> nextStatus == CANCELLED || nextStatus == FILLED || nextStatus ==PARTIALLY_FILLED;
        case CANCELLED,FILLED -> false;
    };
}

}