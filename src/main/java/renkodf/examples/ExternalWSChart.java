package renkodf.examples;

import java.util.List;
import java.util.stream.Collectors;

import org.knowm.xchart.OHLCChart;
import org.knowm.xchart.OHLCChartBuilder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.Styler.ChartTheme;

import renkodf.Renko;
import renkodf.RenkoWS;
import renkodf.examples.interfaces.ExampleChart;
import renkodf.wrappers.OHLCV;

/**
 * "ws_externaldf_demo.py" equivalent
 */
public class ExternalWSChart implements ExampleChart<OHLCChart> {

	public static final String SERIES_NAME = "series1";

	private OHLCChart ohlcChart;
	private final TicksFactory factory = new TicksFactory("US30");
	private final List<OHLCV> ticksList = factory.getTicksList();
	private final Integer halfIndex = ticksList.size() / 2;
	private RenkoWS r;

	public static void main(String[] args) {
		final ExternalWSChart instance = new ExternalWSChart();
		try {
			instance.loop();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void loop() throws InterruptedException {
		final SwingWrapper<OHLCChart> swingWrapper = new SwingWrapper<>(this.getChart());
		swingWrapper.displayChart();

		// Simulate "onMessage"
		for (int i = halfIndex + 1; i < ticksList.size(); i++) {
			// Java is too fast for this
			Thread.sleep(1);

			updateData(i, ticksList);

			Runnable task = swingWrapper::repaintChart;
			javax.swing.SwingUtilities.invokeLater(task);
		}

		System.out.println("Done!");
	}

	@Override
	public OHLCChart getChart() {
		// Create Chart
		ohlcChart = new OHLCChartBuilder().width(800).height(600)
				.title("US30 Real-time Chart")
				.theme(ChartTheme.Matlab).build();

		// Customize Chart
		ohlcChart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideS);
		ohlcChart.getStyler().setLegendLayout(Styler.LegendLayout.Horizontal);

		// Generate initial data
		List<OHLCV> ticksGET = ticksList.subList(0, halfIndex);
		Renko rr = new Renko(ticksGET, 5D);
		
		r = new RenkoWS(rr.getRSD(null), 5D);

		List<OHLCV> initialList = r.initialLists("wicks");
		ohlcChart.addSeries(SERIES_NAME, null,
				initialList.stream().map(OHLCV::getOpen).collect(Collectors.toList()),
				initialList.stream().map(OHLCV::getHigh).collect(Collectors.toList()),
				initialList.stream().map(OHLCV::getLow).collect(Collectors.toList()),
				initialList.stream().map(OHLCV::getClose).collect(Collectors.toList()));
		return ohlcChart;
	}

	public void updateData(int index, List<OHLCV> ticksLocal) {
		OHLCV currentTick = ticksLocal.get(index);
		r.addPrices(currentTick.getDatetime(), currentTick.getClose());

		List<OHLCV> renkoList = r.renkoAnimate("wicks");

		List<Double> openData = renkoList.stream().map(OHLCV::getOpen).collect(Collectors.toList());
		List<Double> highData = renkoList.stream().map(OHLCV::getHigh).collect(Collectors.toList());
		List<Double> lowData = renkoList.stream().map(OHLCV::getLow).collect(Collectors.toList());
		List<Double> closeData = renkoList.stream().map(OHLCV::getClose).collect(Collectors.toList());

		ohlcChart.updateOHLCSeries(SERIES_NAME, null, openData, highData, lowData, closeData);
	}

	@Override
	public String getExampleChartName() {
		return getClass().getSimpleName() + " - Real-time OHLC Chart";
	}
}