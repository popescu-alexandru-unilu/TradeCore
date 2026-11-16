package dev.tradecore.domain.instrument;
import java.util.Currency;
import java.util.Locale;
public record Instrument(String symbol, String name, Currency currency){
    public Instrument{
        if (symbol == null || symbol.isBlank() ){
            throw new IllegalArgumentException(" Symbol must be non-empty");
        }
        symbol = symbol.strip().toUpperCase(Locale.ROOT);
        if (name == null || name.isBlank()){
            throw new IllegalArgumentException("The name must be filled");
        }
        name = name.strip();
        
        if (currency == null){
            throw new IllegalArgumentException("The currency must not be null ");
        }


    }
}