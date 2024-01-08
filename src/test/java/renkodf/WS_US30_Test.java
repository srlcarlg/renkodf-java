package renkodf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import renkodf.wrappers.OHLCV;

@TestInstance(Lifecycle.PER_CLASS)
class WS_US30_Test {

	private static final DataFactory factory = new DataFactory("US30", "Re50");
	private static List<OHLCV> rawTicks = new ArrayList<>();
	
	@BeforeAll
	void loadTickData() {
		rawTicks = factory.getTicksList();
	}
		
	@ParameterizedTest
	@ValueSource(strings = {"normal", "wicks", "nongap", "reverse-wicks", "reverse-nongap", "fake-r-wicks", "fake-r-nongap"})
    @DisplayName("OHLC(all renko-modes) with floating-point-arithmetic should be strictly equal to renkoPython")
	void RenkoJava_ShouldBeStrictlyEqual_RenkoPy (String mode) {
		List<OHLCV> renkoMap = new ArrayList<>();
		
		// First Tick "onOpen" or first data "onMessage"
		OHLCV firstTick = rawTicks.get(0);
		RenkoWS r = new RenkoWS(firstTick.getDatetime(), firstTick.getClose(), 5D);
		
		// Simulate Websocket "onMessage"
		for (int i = 1; i < rawTicks.size(); i++) {
			OHLCV tick = rawTicks.get(i);
			r.addPrices(tick.getDatetime(), tick.getClose());
			// renkoMap = r.renkoAnimate(mode);
		}
		renkoMap = r.renkoAnimate(mode);
		
		// Remove last row because it's a 'Forming' Renko
		renkoMap.remove(renkoMap.size()-1);
		
		List<OHLCV> ohlcvList = factory.loadOHLCbyMode(mode);
		// Overriding "toString" and "equals" of OHLCV class did the trick
        assertEquals(ohlcvList, renkoMap);
	}
}
