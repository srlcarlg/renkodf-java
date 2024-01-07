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

import renkodf.Renko;
import renkodf.wrappers.OHLCV;

public class Example {

	public static void main(String[] args) {
		
		String resourcePath = new File("src/test/resources").getAbsolutePath();
		List<OHLCV> rawTicks = new ArrayList<>();
		
		final Path path = new Path(resourcePath + "/EURGBP_T1_cT.parquet");
		final Configuration conf = new Configuration();
		try (final ParquetReader<Row> reader = RowParquetReader
				.builder(HadoopInputFile.fromPath(path, conf)).build()) {
			Row row = reader.read();
	        while (row != null) {
	            Row loopRow = row;
	            rawTicks.add(
            		new OHLCV(loopRow.getValue("datetime"),
	            		Double.valueOf(loopRow.getValue("bid").toString()))
        		);
	            row = reader.read();
	        }
		} catch (final IOException exception) {}
		
		Renko r = new Renko(rawTicks, 0.0003);
        List<OHLCV> renkoList = r.renkodf("normal");
        renkoList.forEach(x -> System.out.println(x.toString()));
        // BigDecimal.valueOf(x.getClose()).setScale(5, RoundingMode.HALF_UP)
        System.out.println(renkoList.size());
	}
}
