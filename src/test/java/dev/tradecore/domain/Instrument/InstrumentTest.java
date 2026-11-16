package dev.tradecore.domain.instrument;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Currency;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;


public class InstrumentTest{
    private static final Currency USD = Currency.getInstance("USD");
    @Test
    void normalizesSymbolToUpperCase(){
        Instrument instr = new Instrument("  aapl  ","Apple Inc.",USD);
        assertEquals("AAPL",instr.symbol());
    }
    @Test
    void rejectsBlankSymbol(){
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,()-> new Instrument(" ","Apple Inc.",USD) );
        assertEquals(" Symbol must be non-empty", ex.getMessage());
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { " ", "\t"})
    void rejectsInvalidSymbols(String symbol){
        assertThrows(IllegalArgumentException.class,()-> new Instrument(symbol,"Apple Inc.", USD) );
    }
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void rejectsInvalidNames(String name){
        assertThrows(IllegalArgumentException.class,()-> new Instrument("AAPL",name,USD));
    }
    @Test
    void rejectNullCurrency(){
        assertThrows(IllegalArgumentException.class, ()-> new Instrument("AAPL","Apple Inc.", null));
    }

}