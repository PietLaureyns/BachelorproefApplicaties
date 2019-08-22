import com.cloudera.sparkts.models.ARIMA;
import com.cloudera.sparkts.models.ARIMAModel;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.spark.mllib.linalg.Vectors;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class BitcoinData {

    public String filename;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private TwitterApp app;
    private TreeMap<Date, Double> oneMinuteValues = new TreeMap<>();

    public SimpleGraphPanel bitcoinGraph;
    public SimpleGraphPanel bitcoinForecastingGraph1;
    public SimpleGraphPanel bitcoinForecastingGraph2;

    public BitcoinData(TwitterApp app) {
        this.app = app;
        List<Double> values = new ArrayList<>();
        bitcoinGraph = new SimpleGraphPanel(values, 0, "Bitcoin values");
        bitcoinForecastingGraph1 = new SimpleGraphPanel(values, 0, "Bitcoin ARIMA model 1");
        bitcoinForecastingGraph2 = new SimpleGraphPanel(values, 0, "Bitcoin ARIMA model 2");
    }

    public void stop() {
        this.writeToCsv(oneMinuteValues);
    }

    public JsonObject getJsonArrayFromUrl(String url) {
        String json = readUrl(url);
        JsonParser parser = new JsonParser();
        try {
            return (JsonObject) parser.parse(json);
        } catch (Exception e) {
            return null;
        }
    }

    private String readUrl(String urlString) {
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
            System.out.println(dateFormat.format(new Date()) + "JsonObject was null");
            bitcoinValue = this.oneMinuteValues.lastEntry().getValue();
        }

        return bitcoinValue;
    }

    public void resetOneMinute() {
        Date date = new Date();
        Double bitcoinValue = getBitcoinValue();
        this.oneMinuteValues.put(date, bitcoinValue);
        app.writeBitcoin1MinValues(oneMinuteValues);
        updatePlots();
    }

    public void updatePlots() {
        try {
            List<Double> twitterList = oneMinuteValues.values().stream().mapToDouble(i -> i).boxed().collect(Collectors.toList());
            bitcoinGraph.updateGraph(twitterList, twitterList.size(), "Bitcoin Values");

            if (twitterList.size() > 20) {
                twitterList = twitterList.subList(twitterList.size() - 20, twitterList.size());
            }

            double[] values = twitterList.stream().mapToDouble(i -> i).toArray();
            org.apache.spark.mllib.linalg.Vector vector = Vectors.dense(values);
            ARIMAModel arima = ARIMA.fitModel(1, 0, 0, vector, true, "", null);
            ARIMAModel arima2 = ARIMA.autoFit(vector, 5, 2, 5);

            org.apache.spark.mllib.linalg.Vector forecast = arima.forecast(vector, 5);
            List<Double> forecastList = new ArrayList<>();
            forecastList.addAll(twitterList);
            forecastList.addAll(Arrays.stream(forecast.toArray()).boxed().collect(Collectors.toList()).subList(forecast.toArray().length - 5, forecast.toArray().length));
            bitcoinForecastingGraph1.updateGraph(forecastList, twitterList.size() - 1, "Bitcoin ARIMA1 model: " + arima.p() + " , " + arima.d() + " , " + arima.q());

            org.apache.spark.mllib.linalg.Vector forecast2 = arima2.forecast(vector, 5);
            List<Double> forecastList2 = new ArrayList<>();
            forecastList2.addAll(twitterList);
            forecastList2.addAll(Arrays.stream(forecast2.toArray()).boxed().collect(Collectors.toList()).subList(forecast2.toArray().length - 5, forecast2.toArray().length));
            bitcoinForecastingGraph2.updateGraph(forecastList2, twitterList.size() - 1, "Bitcoin ARIMA2 model: " + arima2.p() + " , " + arima2.d() + " , " + arima2.q());

        } catch (Exception e) {
            System.out.println("Error in Bitcoin updateplot" + e.toString());
        }
    }

    private void writeToCsv(TreeMap<Date, Double> values) {
        if (!values.isEmpty()) {
            FileWriter fw;
            try {
                fw = new FileWriter(filename, false);
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
