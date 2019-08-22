import com.github.signaflo.timeseries.TimePeriod;
import com.github.signaflo.timeseries.TimeSeries;
import com.github.signaflo.timeseries.TimeUnit;
import com.github.signaflo.timeseries.forecast.Forecast;
import com.github.signaflo.timeseries.model.arima.Arima;
import com.github.signaflo.timeseries.model.arima.ArimaOrder;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Flow;
import java.util.stream.Collectors;

public class TwitterSubscriber implements Flow.Subscriber<Tweet> {

    public Flow.Subscription subscription;
    private String fileName;
    private Date startDate = new Date();
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private DateFormat dateFormat2 = new SimpleDateFormat("dd-MM-yyyy__HH-mm-ss_");
    private TimeSeriesApp app;

    private int tenMinuteTeller = 0;
    private int oneMinuteTeller = 0;
    private int oneHourTeller = 0;

    private int totalCounter = 0;
    private int tenMinCounter = 0;
    private int hourCounter = 0;

    private TreeMap<Date, Double> oneMinuteValues = new TreeMap<>();
    private TreeMap<Date, Double> tenMinuteValues = new TreeMap<>();
    private TreeMap<Date, Double> oneHourValues = new TreeMap<>();
    private TreeMap<Date, Double> oneMinuteValuesTemp = new TreeMap<>();
    private TreeMap<Date, Double> tenMinuteValuesTemp = new TreeMap<>();
    private TreeMap<Date, Double> oneHourValuesTemp = new TreeMap<>();

    private GraphPanel graphPanel1;
    private GraphPanel graphPanel2;
    private SimpleGraphPanel forecastingGraphPanel;

    public TwitterSubscriber(TimeSeriesApp app) {
        this.app = app;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(Tweet tweet) {
        oneMinuteTeller++;
        totalCounter++;
        app.updateAantalTweets(this.totalCounter);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    public void onComplete() {
        System.out.println("Done.");
    }


    public TimeSeries getTwitterTimeSeries() {
        List<Double> list = oneMinuteValues.values().stream().collect(Collectors.toList());

        double[] series;
        if (list.size() > 20) {
            series = new double[20];
            List<Double> list2 = list.subList(list.size() - 20, list.size());
            for (int i = 0; i < series.length; i++) {
                series[i] = list2.get(i).doubleValue();
            }
        } else {
            series = new double[list.size()];
            for (int i = 0; i < series.length; i++) {
                series[i] = list.get(i).doubleValue();
            }
        }

        TimeSeries ts = TimeSeries.from(new TimePeriod(TimeUnit.MINUTE, Long.valueOf(1)), series);

        return ts;
    }

    public void resetOneMinute() {
        Date date = new Date();

        tenMinCounter++;
        tenMinuteTeller += oneMinuteTeller;

        oneMinuteValues.put(date, (double) this.oneMinuteTeller);
        oneMinuteValuesTemp.put(date, (double) oneMinuteTeller);
        oneMinuteTeller = 0;

        if (tenMinCounter >= 10) {
            oneHourTeller += tenMinuteTeller;
            hourCounter++;
            if (hourCounter >= 6) {
                oneHour(date);
            }

            tenMinuteValues.put(date, (double) tenMinuteTeller);
            tenMinuteValuesTemp.put(date, (double) tenMinuteTeller);
            tenMinCounter = 0;
            tenMinuteTeller = 0;
        }

        this.app.writeTwitter1MinValues(oneMinuteValues);

        graphPanel1.updateGraph(oneMinuteValues);

        if (oneMinuteValues.size() > 1) {
            List<Double> forecastingEstimates = getForecastingEstimates();
            if (forecastingEstimates.size() >= 25) {
                forecastingGraphPanel.updateGraph(forecastingEstimates, 19);
            } else {
                forecastingGraphPanel.updateGraph(forecastingEstimates, oneMinuteValues.size() - 1);
            }
        }
    }

    public List<Double> getForecastingEstimates() {
        TimeSeries ts = getTwitterTimeSeries();

        ArimaOrder modelOrder = ArimaOrder.order(1, 1, 0);
        Arima model = Arima.model(ts, modelOrder);

        Forecast forecast;
        int amount = (int) Math.floor(ts.size() / 3);
        if (amount < 1) {
            forecast = model.forecast(1);
        } else if (amount > 5) {
            forecast = model.forecast(5);
        } else {
            forecast = model.forecast(amount);
        }

        List<Double> points = new ArrayList<>();
        ts.asList().forEach(d -> points.add(d));

        for (Double d : forecast.pointEstimates().asList()) {
            points.add(d);
        }

        return points;
    }

    private void oneHour(Date date) {
        oneHourValues.put(date, (double) oneHourTeller);
        oneHourValuesTemp.put(date, (double) oneHourTeller);
        hourCounter = 0;
        oneHourTeller = 0;
        graphPanel2.updateGraph(oneHourValues);
        writeToCsvOneHour();
    }


    public void start() {
        graphPanel1 = new GraphPanel(oneMinuteValues, "Aantal Tweets iedere minuut");
        graphPanel2 = new GraphPanel(oneHourValues, "Aantal Tweets ieder uur");
        forecastingGraphPanel = new SimpleGraphPanel(oneMinuteValues.values().stream().mapToDouble(a -> a).boxed().collect(Collectors.toList()),
                0, "Twitter Forecasting - ARIMA.Model( 1 , 1 , 0 )");

        this.startDate = new Date();
        String startTime = dateFormat2.format(startDate);
        this.fileName = startTime + "Twitter";
        this.totalCounter = 0;
        this.tenMinuteTeller = 0;
        this.oneMinuteTeller = 0;
        this.oneHourTeller = 0;
    }


    public void writeToCsvOneHour() {
        writeToCsv(oneMinuteValuesTemp, "_1min_Temp");
        writeToCsv(tenMinuteValuesTemp, "_10min_Temp");
        writeToCsv(oneHourValuesTemp, "_1uur_Temp");
        writeSummaryToCsv("_Summary_Temp");
        oneMinuteValuesTemp = new TreeMap<>();
        tenMinuteValuesTemp = new TreeMap<>();
        oneHourValuesTemp = new TreeMap<>();
    }

    public void stop() {
        writeToCsv(tenMinuteValues, "_10min");
        writeToCsv(oneMinuteValues, "_1min");
        writeToCsv(oneHourValues, "_1uur");
        writeSummaryToCsv("_Summary");
    }

    public void writeToCsv(TreeMap<Date, Double> values, String name) {
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
                System.out.println(name + ": File Not Found.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(name + ": No Data to write.");
        }
    }

    public void writeSummaryToCsv(String name) {
        FileWriter fw;
        try {
            fw = new FileWriter(fileName + name + ".txt", true);
            fw.write("Starttijd: " + dateFormat.format(startDate) + "\n");
            fw.write("Eindtijd: " + dateFormat.format(new Date()) + "\n");
            long diffInMillieseconds = new Date().getTime() - startDate.getTime();
            double seconds = (diffInMillieseconds / 1000);
            double minutes = seconds / 60;
            double hours = minutes / 60;

            DecimalFormat df = new DecimalFormat("0.000");

            fw.write("Totaal RunTime: " + df.format(hours) + " uur of " + df.format(minutes) + " minuten of " + df.format(seconds) + " seconden \n\n");
            fw.write("Totaal aantal tweets: " + this.totalCounter + "\n\n");

            fw.write("Aantal 1 minuut waarden: " + this.oneMinuteValues.size() + "\n");


            if (this.oneHourValues.size() > 0) {
                fw.write("Aantal 60 minuten waarden: " + this.oneHourValues.size() + "\n\n");
                fw.write("Gemiddelde 60 minuut waarde: " + df.format(this.oneHourValues.values().stream().mapToDouble(a -> a).average().getAsDouble()) + "\n\n");
            }

            fw.write("Gemiddelde 1 minuut waarde: " + df.format(this.oneMinuteValues.values().stream().mapToDouble(a -> a).average().getAsDouble()) + "\n");

            fw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println(name + ": File Not Found.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
