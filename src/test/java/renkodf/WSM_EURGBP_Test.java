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
class WSM_EURGBP_Test {
	
	private static final DataFactory factory = new DataFactory("EURGBP", "Re3");
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
		RenkoWSModified r = new RenkoWSModified(firstTick.getDatetime(), firstTick.getClose(), 0.0003);
		
		OHLCV currentBar = new OHLCV();
		// Simulate Websocket "onMessage"
		for (int i = 1; i < rawTicks.size(); i++) {
			OHLCV tick = rawTicks.get(i);
			r.addPrices(tick.getDatetime(), tick.getClose());
			
			OHLCV renko = r.renkoAnimate(mode).get(0); 
			if (!currentBar.equals(renko)) {
				currentBar = renko;
				renkoMap.add(currentBar);
			}
		}
        // Removing first row
		renkoMap.remove(0);
		
		List<OHLCV> ohlcvList = factory.loadOHLCbyMode(mode);
		// Overriding "toString" and "equals" of OHLCV class did the trick
        assertEquals(ohlcvList, renkoMap);
	}
}
