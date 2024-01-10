package renkodf.examples;

import java.util.List;
import java.util.stream.Collectors;

import org.knowm.xchart.OHLCChart;
import org.knowm.xchart.OHLCChartBuilder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.Styler;

import renkodf.Renko;
import renkodf.examples.interfaces.ExampleChart;
import renkodf.wrappers.OHLCV;

public class RenkoChart implements ExampleChart<OHLCChart> {

	public static void main(String[] args) {
		final RenkoChart instance = new RenkoChart();
		final OHLCChart chart = instance.getChart();
		new SwingWrapper<>(chart).displayChart();
	}

	@Override
	public OHLCChart getChart() {
		// Create Chart
		OHLCChart chart = new OHLCChartBuilder().width(800).height(600).title("EURGBP").build();

		// Customize Chart
		chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideS);
		chart.getStyler().setLegendLayout(Styler.LegendLayout.Horizontal);

		// Create Data
		TicksFactory factory = new TicksFactory("EURGBP");
		List<OHLCV> ticksList = factory.getTicksList();

		Renko r = new Renko(ticksList, 0.0003);
		// Only the last 50 renkos for a better view
		List<OHLCV> renkoList = r.renkodf("wicks").subList(111, 161);

		// We doesn't need the Datetime for plot here
		List<Double> openData = renkoList.stream().map(OHLCV::getOpen).collect(Collectors.toList());
		List<Double> highData = renkoList.stream().map(OHLCV::getHigh).collect(Collectors.toList());
		List<Double> lowData = renkoList.stream().map(OHLCV::getLow).collect(Collectors.toList());
		List<Double> closeData = renkoList.stream().map(OHLCV::getClose).collect(Collectors.toList());

		chart.addSeries("Series", null, openData, highData, lowData, closeData);
		chart.getStyler().setToolTipsEnabled(true);
		return chart;
	}

	@Override
	public String getExampleChartName() {
		return getClass().getSimpleName() + " - OHLC Candle Chart";
	}
}
