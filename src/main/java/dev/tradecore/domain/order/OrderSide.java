package dev.tradecore.domain.order;
import java.math.BigDecimal;
public enum OrderSide{
BUY(1),
SELL(-1);
private final int sign;
private OrderSide(int sign){
    this.sign = sign;
}
public int getSign(){return sign;}
public OrderSide opposite(){
    return this == BUY ? SELL : BUY;
}
public BigDecimal apply(BigDecimal quantity){
    return quantity.multiply(BigDecimal.valueOf(sign));
}
public boolean isBuy(){
    return this == BUY;
}
public boolean isSell(){
    return this == SELL;
}

}