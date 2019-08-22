import com.cloudera.sparkts.models.ARIMA;
import com.cloudera.sparkts.models.ARIMAModel;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.RootLogger;
import org.apache.spark.SparkConf;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.twitter.TwitterUtils;

import twitter4j.Status;
import twitter4j.auth.Authorization;
import twitter4j.auth.AuthorizationFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationContext;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class TwitterData {

    private String CONSUMER_KEY = "Th1uGQV9i80IrYDhgpKamT5Q3";
    private String CONSUMER_KEY_SECRET = "No4urSpNsnkgfNYYuMGqrwm30s6MPZaLN7Dl2yiI5ywa4R0nK6";
    private String ACCESS_TOKEN = "1683608778-kzWasrwQKXXDW7H9pIQ2qCHucVBjjf6rl8MWkSr";
    private String ACCESS_TOKEN_SECRET = "L0hQTuc59UfQJPFG1Yn7aXvVYlpEDg2GnKrk1fwDtmevW";

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public SimpleGraphPanel twitterGraph;
    public SimpleGraphPanel twitterForecastingGraph1;
    public SimpleGraphPanel twitterForecastingGraph2;

    public static int teller = 0;
    public static int totalTeller = 0;
    public TreeMap<Date, Integer> oneMinuteTwitterValues = new TreeMap<>();
    public static TwitterApp app;
    public String filename;

    public TwitterData(TwitterApp app) {
        this.app = app;
        List<Double> values = new ArrayList<>();
        twitterGraph = new SimpleGraphPanel(values, 0, "Twitter values");
        twitterForecastingGraph1 = new SimpleGraphPanel(values, 0, "Twitter ARIMA model 1");
        twitterForecastingGraph2 = new SimpleGraphPanel(values, 0, "Twitter ARIMA model 2");
    }

    public void startTwitterStream() {
        System.setProperty("twitter4j.oauth.consumerKey", CONSUMER_KEY);
        System.setProperty("twitter4j.oauth.consumerSecret", CONSUMER_KEY_SECRET);
        System.setProperty("twitter4j.oauth.accessToken", ACCESS_TOKEN);
        System.setProperty("twitter4j.oauth.accessTokenSecret", ACCESS_TOKEN_SECRET);

        SparkConf sparkConf = new SparkConf().setAppName("SparkStreamingTwitter").setMaster("local[*]");
        JavaStreamingContext ssc = new JavaStreamingContext(sparkConf, new Duration(2000)); //2 seconden

        RootLogger rootLogger = (RootLogger) Logger.getRootLogger();
        rootLogger.setLevel(Level.ERROR);

        Configuration twitterConf = ConfigurationContext.getInstance();
        Authorization twitterAuth = AuthorizationFactory.getInstance(twitterConf);

        String[] filters = {"Bitcoin", "bitcoin", "btc", "BTC"};
        JavaReceiverInputDStream<Status> stream = TwitterUtils.createStream(ssc, twitterAuth, filters);

        JavaDStream<Status> tweets = stream.filter((status) -> {
            teller++;
            totalTeller++;
            System.out.println(totalTeller + " - " + teller);
            app.updateAantalTweets(totalTeller);
            return status.getText() != null;
        });

        tweets.dstream().count().print();

        ssc.start();
        ssc.awaitTermination();
    }

    public void resetOneMinute() {
        System.out.println("Twitter reset one minute");
        oneMinuteTwitterValues.put(new Date(), teller);
        teller = 0;
        app.writeTwitter1MinValues(oneMinuteTwitterValues);
        updatePlots();

    }

    public void stop() {
        writeToCsv(oneMinuteTwitterValues);
    }

    public void writeToCsv(TreeMap<Date, Integer> values) {
        if (!values.isEmpty()) {
            FileWriter fw;
            try {
                fw = new FileWriter(filename, false);
                for (Map.Entry<Date, Integer> entry : values.entrySet()) {
                    fw.write(dateFormat.format(entry.getKey()) + ";" + entry.getValue() + "\n");
                }
                fw.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.out.println(filename + ": File Not Found.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println(filename + ": No Data to write.");
        }
    }

    public void updatePlots() {
        try {
            List<Double> twitterList = oneMinuteTwitterValues.values().stream().mapToDouble(i -> i).boxed().collect(Collectors.toList());
            twitterGraph.updateGraph(twitterList, twitterList.size(), "Twitter Values");

            if (twitterList.size() > 20) {
                twitterList = twitterList.subList(twitterList.size() - 20, twitterList.size());
            }

            double[] values = twitterList.stream().mapToDouble(i -> i).toArray();
            org.apache.spark.mllib.linalg.Vector vector = Vectors.dense(values);
            ARIMAModel arima = ARIMA.autoFit(vector, 5, 2, 5);

            org.apache.spark.mllib.linalg.Vector forecast = arima.forecast(vector, 5);
            List<Double> forecastList = new ArrayList<>();
            forecastList.addAll(twitterList);
            forecastList.addAll(Arrays.stream(forecast.toArray()).boxed().collect(Collectors.toList()).subList(forecast.toArray().length - 5, forecast.toArray().length));
            twitterForecastingGraph1.updateGraph(forecastList, twitterList.size() - 1, "Twitter ARIMA model: " + arima.p() + " , " + arima.d() + " , " + arima.q());
        } catch (Exception e) {
            System.out.println("Error in Twitter updateplot" + e.toString());
        }
    }
}
