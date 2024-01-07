package renkodf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import renkodf.wrappers.OHLCV;

@TestInstance(Lifecycle.PER_CLASS)
class EURGBP_Test {
	
	private static final DataFactory factory = new DataFactory("EURGBP", "Re3");
	private static Renko r;
	
	@BeforeAll
	void loadTickDataAndBuildRenko() {
		List<OHLCV> rawTicks = factory.getTicksList();
		r = new Renko(rawTicks, 0.0003);
	}
		
	@ParameterizedTest
	@ValueSource(strings = {"normal", "wicks", "nongap", "reverse-wicks", "reverse-nongap", "fake-r-wicks", "fake-r-nongap"})
    @DisplayName("OHLC(all renko-modes) with floating-point-arithmetic should be strictly equal to renkoPython")
	void RenkoJava_ShouldBeStrictlyEqual_RenkoPy (String mode) {
		List<OHLCV> renkoMap = r.renkodf(mode);
		List<OHLCV> ohlcvList = factory.loadOHLCbyMode(mode);
		// Overriding "toString" and "equals" of OHLCV class did the trick
        assertEquals(ohlcvList, renkoMap);
	}
}
