package games.strategy.triplea.ui.statistics;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.apache.commons.text.WordUtils;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.Marker;
import org.knowm.xchart.style.markers.None;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Table;

import games.strategy.engine.data.GameData;
import games.strategy.engine.history.Round;
import games.strategy.engine.stats.Statistics;
import games.strategy.engine.stats.StatisticsAggregator;
import games.strategy.triplea.ui.mapdata.MapData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class StatisticsDialog extends JPanel {
  private static final int PADDING = 4;
  private static final XYChartBuilder XY_CHART_DEFAULTS =
      new XYChartBuilder().theme(Styler.ChartTheme.Matlab).xAxisTitle("Round");
  private static final Marker MARKER = new None();
  private static final BasicStroke STROKE = new BasicStroke(2.0f);

  @Getter
  @RequiredArgsConstructor
  private static class OverTimeChart {
    private final String title;
    private final String axisTitle;
    private final Table<String, Round, Double> data;
  }
  
  public enum ChartMode {
	  PLAYERS,
	  ALLIANCES,
	  PLAYERS_AND_ALLIANCES;
	  
	  @Override
	public String toString() {
		return WordUtils.capitalizeFully(name().replace('_', ' '));
	}
  }
  
  private final GameData gameData;
  private final MapData mapData;
  
  private Map<String, OverTimeChart> charts;
  private ChartMode selectedMode;

  public StatisticsDialog(final GameData gameData, final MapData mapData) {
	  this.gameData = gameData;
	  this.mapData = mapData;
	  
	  this.charts = new HashMap<>();
	this.selectedMode = ChartMode.PLAYERS;
	
	setLayout(new BorderLayout(PADDING, PADDING));
	setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
	
	final JTabbedPane tabs = new JTabbedPane();
	
    final JLabel label = new JLabel("Computing statistics from game history...");
    add(label, BorderLayout.CENTER);
    
    final JComboBox<ChartMode> mode = new JComboBox<>(ChartMode.values());
    mode.setEditable(false);
    mode.setSelectedItem(this.selectedMode);
    add(mode, BorderLayout.PAGE_START);
    
    mode.addActionListener(event -> {
    	this.selectedMode = mode.getModel().getElementAt(mode.getSelectedIndex());
    	refreshCharts(tabs);
    });
    
    Runnable addChartsToUi = () -> {
    	remove(label);
        for (final OverTimeChart chart : charts.values()) {
          tabs.addTab(chart.getTitle(), createChart(chart));
        }
        add(tabs, BorderLayout.CENTER);
        doLayout();
    };

    Thread t = new Thread(() -> {
    	final Statistics statistics = new StatisticsAggregator(gameData, mapData).aggregate();
    	statistics.getOverTimeStatistics().forEach((key, value) ->
    		charts.put(key.getName(), new OverTimeChart(key.getName(), key.getAxisLabel(), value)));
    	
    	SwingUtilities.invokeLater(addChartsToUi);
    }, "game-stats-calc");
    t.setDaemon(true);
    t.start();
  }
  
  private void refreshCharts(JTabbedPane tabs) {
	  for (int i = 0; i < tabs.getTabCount(); i++) {
		  final OverTimeChart chartData = charts.get(tabs.getTitleAt(i));
		  tabs.setComponentAt(i, createChart(chartData));
	  }
  }

  private JPanel createChart(final OverTimeChart chartData) {
    final XYChart chart = XY_CHART_DEFAULTS
    	.title(chartData.getTitle())
    	.yAxisTitle(chartData.getAxisTitle())
    	.build();
    
    if (selectedMode == ChartMode.PLAYERS_AND_ALLIANCES) {
    	chart.setYAxisGroupTitle(0, chartData.getAxisTitle() + " - " + ChartMode.PLAYERS);
    	chart.setYAxisGroupTitle(1, chartData.getAxisTitle() + " - " + ChartMode.ALLIANCES);
    }
    
    chartData
        .getData()
        .rowMap()
        .forEach((key, value) -> {
        	final boolean isAlliance = gameData.getAllianceTracker().getAlliances().contains(key);
        	if (selectedMode == ChartMode.ALLIANCES && !isAlliance) { return; }
        	if (selectedMode == ChartMode.PLAYERS && isAlliance) { return; }
        	
        	XYSeries series = chart.addSeries(key, ImmutableList.copyOf(value.values()));
        	
        	if (selectedMode == ChartMode.PLAYERS_AND_ALLIANCES) {
        		series.setYAxisGroup(isAlliance? 1 : 0);
        	}
        	
        	series.setSmooth(true);
        	series.setMarker(MARKER);
        	series.setLineStyle(STROKE);
        	
        	Color factionColor = mapData.getPlayerColor(key);
        	series.setLineColor(factionColor);
        	series.setMarkerColor(factionColor);
        });
    
    chart.getStyler().setYAxisMin(0.0);
    
    return new XChartPanel<>(chart);
  }
}
