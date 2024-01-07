package renkodf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;

import com.exasol.parquetio.data.Row;
import com.exasol.parquetio.reader.RowParquetReader;

import renkodf.wrappers.OHLCV;

public class DataFactory {
	
	private String symbol;
	private String renkoString;
	
	private static final String RESOURCE_PATH = new File("src/test/resources").getAbsolutePath();
	private static final List<String> OHLV_STRINGS = List.of("open", "high", "low", "close", "volume");
		
	public DataFactory(String symbol, String renkoString) {
		super();
		this.symbol = symbol;
		this.renkoString = renkoString;
	}

	public List<OHLCV> getTicksList() {
		List<OHLCV> rawTicks = new ArrayList<>();
		final Path path = new Path(RESOURCE_PATH + String.format("/%s_T1_cT.parquet", symbol));
		final Configuration conf = new Configuration();
		try (final ParquetReader<Row> reader = RowParquetReader.builder(HadoopInputFile.fromPath(path, conf)).build()) {
			Row row = reader.read();
	        while (row != null) {
	            Row loopRow = row;
	            rawTicks.add(new OHLCV(loopRow.getValue("datetime"),
	            	Double.valueOf(loopRow.getValue("bid").toString()))
        		);
	            row = reader.read();
	        }
	        return rawTicks;
		} catch (final IOException exception) {return rawTicks;}
	}
	
	public List<OHLCV> loadOHLCbyMode(String mode) {
		List<OHLCV> ohlcvList = new ArrayList<>();
		
		final Path path = new Path(RESOURCE_PATH + String.format("/%s/%s_%s_%s.parquet", symbol, symbol, renkoString, mode));
		final Configuration conf = new Configuration();
		try (final ParquetReader<Row> reader = RowParquetReader.builder(HadoopInputFile.fromPath(path, conf)).build()) {
			Row row = reader.read();
	        while (row != null) {
	            Row loopRow = row;
	            Object datetime = loopRow.getValue("datetime");
	            
	            Double[] ohlcvArray = {0D, 0D, 0D, 0D, 0D};
	            for (int i = 0; i < OHLV_STRINGS.size(); i++) {
	            	ohlcvArray[i] = Double.valueOf(loopRow.getValue(OHLV_STRINGS.get(i)).toString());
				}
	            
	            ohlcvList.add(
	            	new OHLCV(datetime, ohlcvArray[0], ohlcvArray[1],ohlcvArray[2], ohlcvArray[3], ohlcvArray[4])
	            );
	            row = reader.read();
	        }
		} catch (final IOException exception) {}
		
		return ohlcvList;
	}
}
