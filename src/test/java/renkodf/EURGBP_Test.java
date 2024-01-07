package renkodf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.exasol.parquetio.data.Row;
import com.exasol.parquetio.reader.RowParquetReader;

@TestInstance(Lifecycle.PER_CLASS)
class EURGBP_Test {
	
	static final String resourcePath = new File("src/test/resources").getAbsolutePath();
	static final List<String> ohlcHeaders = List.of("open", "high", "low", "close", "volume");
	Renkodf r;
	
	@BeforeAll
	void loadTickDataAndBuildRenko() {
		Map<String, ArrayList<Object>> rawTicks = new LinkedHashMap<>();
		rawTicks.put("datetime", new ArrayList<>());
		rawTicks.put("close", new ArrayList<>());
		
		final Path path = new Path(resourcePath + "/EURGBP_T1_cT.parquet");
		final Configuration conf = new Configuration();
		try (final ParquetReader<Row> reader = RowParquetReader
				.builder(HadoopInputFile.fromPath(path, conf)).build()) {
			Row row = reader.read();
	        while (row != null) {
	            Row loopRow = row;
	            rawTicks.get("datetime").add(loopRow.getValue("datetime"));
	            rawTicks.get("close").add(loopRow.getValue("bid"));
	            row = reader.read();
	        }
		} catch (final IOException exception) {}
		
		r = new Renkodf(rawTicks, 0.0003);
	}
	
	Map<String, List<String>> loadOHLCbyMode(String mode) {
    	Map<String, List<String>> ohlcMap = new LinkedHashMap<>();
    	ohlcHeaders.forEach(name -> ohlcMap.put(name, new ArrayList<>()));
    	
		final Path path = new Path(resourcePath + String.format("/EURGBP/EURGBP_Re3_%s.parquet", mode));
		final Configuration conf = new Configuration();
		try (final ParquetReader<Row> reader = RowParquetReader
				.builder(HadoopInputFile.fromPath(path, conf)).build()) {
			Row row = reader.read();
	        while (row != null) {
	            Row loopRow = row;
	            ohlcHeaders.forEach(name -> ohlcMap.get(name).add(loopRow.getValue(name).toString()));
	            row = reader.read();
	        }
		} catch (final IOException exception) {}
		
		return ohlcMap;
	}
	
	@ParameterizedTest
	@ValueSource(strings = {"normal", "wicks", "nongap", "reverse-wicks", "reverse-nongap", "fake-r-wicks", "fake-r-nongap"})
    @DisplayName("OHLC(all renko-modes) with floating-point-arithmetic should be strictly equal to renkoPython")
	void RenkoJava_ShouldBeStrictlyEqual_RenkoPy (String mode) {
		Map<String, List<String>> renkoMap = r.renkodf(mode);
		Map<String, List<String>> ohlcMap = loadOHLCbyMode(mode);
			
        assertEquals(ohlcMap.get("open"), renkoMap.get("open"));
        assertEquals(ohlcMap.get("high"), renkoMap.get("high"));
        assertEquals(ohlcMap.get("low"), renkoMap.get("low"));
        assertEquals(ohlcMap.get("close"), renkoMap.get("close"));	
	}
}
