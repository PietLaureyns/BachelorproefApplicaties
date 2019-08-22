import com.github.signaflo.data.visualization.Plots;
import com.github.signaflo.timeseries.TimePeriod;
import com.github.signaflo.timeseries.TimeSeries;
import com.github.signaflo.timeseries.TimeUnit;
import com.github.signaflo.timeseries.forecast.Forecast;
import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class BitcoinData {

    private String fileName;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private DateFormat dateFormat2 = new SimpleDateFormat("dd-MM-yyyy__HH-mm-ss_");
    private TimeSeriesApp app;
    private TreeMap<Date, Double> tenMinuteValues = new TreeMap<>();
    private TreeMap<Date, Double> oneMinuteValues = new TreeMap<>();
    private TreeMap<Date, Double> oneHourValues = new TreeMap<>();

    private int teller10 = 10;
    private int hourTeller = 6;

    private GraphPanel graphPanel1;
    private GraphPanel graphPanel2;
    private SimpleGraphPanel forecastingGraphPanel;

    public BitcoinData(TimeSeriesApp app) {
        this.app = app;
    }

    public TimeSeries getBitcoinTimeSeries(){
        List<Double> list = oneMinuteValues.values().stream().mapToDouble(a -> a).boxed().collect(Collectors.toList());

        double[] series;
        if(list.size() > 20){
            series = new double[20];
            List<Double> list2 = list.subList(list.size()-20,list.size());
            for (int i = 0; i < series.length; i++) {
                series[i] = list2.get(i).doubleValue();
            }
        }else{
            series = new double[list.size()];
            for (int i = 0; i < series.length; i++) {
                series[i] = list.get(i).doubleValue();
            }
        }

        TimeSeries ts = TimeSeries.from(new TimePeriod(TimeUnit.MINUTE, Long.valueOf(1)),series);

        return ts;
    }

    public void setValues(TreeMap<Date, Double> testValues){
        this.oneMinuteValues = testValues;
    }

    public void stop() {
        tenMinuteValues.pollLastEntry();
        oneMinuteValues.pollLastEntry();
        oneHourValues.pollLastEntry();
        this.writeToCsv(tenMinuteValues, "_10min");
        this.writeToCsv(oneMinuteValues, "_1min");
        this.writeToCsv(oneHourValues, "_1uur");
    }

    public void start() {
        graphPanel1 = new GraphPanel(oneMinuteValues, "Bitcoin waarde iedere minuut");
        graphPanel2 = new GraphPanel(oneHourValues, "Bitcoin waarde ieder uur");
        forecastingGraphPanel = new SimpleGraphPanel(oneMinuteValues.values().stream().mapToDouble(a -> a).boxed().collect(Collectors.toList()),
                0, "Bitcoin Forecasting - ARIMA.Model( 1 , 1 , 0 )");
        String startTime = dateFormat2.format(new Date());
        this.fileName = startTime+"Bitcoin";
    }

    public JsonObject getJsonArrayFromUrl(String url){
        String json = readUrl(url);
        JsonParser parser = new JsonParser();
        try{
            return (JsonObject) parser.parse(json);
        } catch(Exception e){
            return null;
        }
    }

    private String readUrl(String urlString){
        BufferedReader reader = null;
        try {
            InputStream inputStream = new URL(urlString).openStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            StringBuffer buffer = new StringBuffer();

            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            inputStream.close();

            return buffer.toString();
        } catch (NullPointerException e) {
            System.out.println("Nullpointer Exception");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Double getBitcoinValue() {
        JsonObject item = getJsonArrayFromUrl("https://blockchain.info/ticker");

        Double bitcoinValue;
        if (item != null) {
            bitcoinValue = item.getAsJsonObject("EUR").get("last").getAsDouble();
        } else {
            System.out.println(dateFormat.format(new Date())+"JsonObject was null");
            bitcoinValue = this.oneMinuteValues.lastEntry().getValue();
        }

        return bitcoinValue;
    }

    public void resetOneMinute() {
        Double bitcoinValue = getBitcoinValue();
        Date date = new Date();
        teller10++;

        if(teller10 >= 10){
            hourTeller++;
            this.tenMinuteValues.put(date, bitcoinValue);

            teller10 = 0;

            if(hourTeller >= 6){
                this.oneHourValues.put(date, bitcoinValue);
                hourTeller = 0;
                graphPanel2.updateGraph(oneHourValues);
            }
        }
        this.oneMinuteValues.put(date, bitcoinValue);
        app.writeBitcoin1MinValues(oneMinuteValues);

        graphPanel1.updateGraph(oneMinuteValues);

        if(oneMinuteValues.size() > 1){
            List<Double> forecastingEstimates = getForecastingEstimates();
            if(forecastingEstimates.size() >= 25){
                forecastingGraphPanel.updateGraph(forecastingEstimates, 19);
            }else{
                forecastingGraphPanel.updateGraph(forecastingEstimates, oneMinuteValues.size()-1);
            }
        }
    }

    public List<Double> getForecastingEstimates() {
        TimeSeries ts = getBitcoinTimeSeries();

        ArimaOrder modelOrder = ArimaOrder.order(1, 1, 0);
        Arima model = Arima.model(ts, modelOrder);

        Forecast forecast;
        int amount = (int) Math.floor(ts.size() / 3);
        if(amount < 1){
            forecast = model.forecast(1);
        }else if(amount > 5){
            forecast = model.forecast(5);
        }else{
            forecast = model.forecast(amount);
        }

        List<Double> points = new ArrayList<>();
        ts.asList().forEach(d -> points.add(d));

        for (Double d : forecast.pointEstimates().asList()) {
            points.add(d);
        }

        return points;
    }

    private void writeToCsv(TreeMap<Date, Double> values, String name) {
        if (!values.isEmpty()) {
            FileWriter fw;
            try {
                fw = new FileWriter(fileName + name + ".txt", true);
                for (Map.Entry<Date, Double> entry : values.entrySet()) {
                    fw.write(dateFormat.format(entry.getKey()) + ";" + entry.getValue() + "\n");
                }
                fw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.out.println("File Not Found.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No Data to write.");
        }
    }
}
