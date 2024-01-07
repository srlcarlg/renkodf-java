package renkodf;

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

import com.exasol.parquetio.data.Row;
import com.exasol.parquetio.reader.RowParquetReader;

public class Main {

	public static void main(String[] args) {

		String resourcePath = new File("src/test/resources").getAbsolutePath();
		
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
		} catch (final IOException exception) {
		    //
		}
		
        Renkodf r = new Renkodf(rawTicks, 0.0003);
        Map<String, List<String>> renkoMap = r.renkodf("normal");
        renkoMap.keySet().forEach(key -> {
        	System.out.println(key + ": " + renkoMap.get(key));
        });
	}
}
