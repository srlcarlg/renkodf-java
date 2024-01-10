package renkodf.examples;

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

public class TicksFactory {

	private String symbol;
	
	private static final String RESOURCE_PATH = new File("src/test/resources").getAbsolutePath();

	public TicksFactory(String symbol) {
		super();
		this.symbol = symbol;
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
}
