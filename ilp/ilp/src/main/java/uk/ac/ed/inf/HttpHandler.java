package uk.ac.ed.inf;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class HttpHandler {

    String currentWebServer;
    private static final HttpClient client = HttpClient.newHttpClient();

    /**
     * HttpHandler will take URL in String format and turns into URL to make requests.
     * @param localHost is our machine.
     * @param port is the server port. For example 9898.
     */

    public HttpHandler(String localHost, String port) {
        this.currentWebServer = "http://" + localHost + ":" + port;
    }

    /**
     * Getting requests for any given directory.
     * @param directory
     * @return output.
     * @throws RuntimeException when sending request is failed.
     * @throws java.net.ConnectException when HttpClient cannot connect to the server.
     * @throws  MalformedURLException when the URL is not right.
     * @throws  InterruptedException
     * @throws  IOException
     */
    public String getRequest(String directory) {
        HttpResponse<String> httpResponse = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(this.currentWebServer + directory))
                    .build();
            httpResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
            int httpResponseCode = httpResponse.statusCode();
            if (httpResponseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed with :" + httpResponseCode);
            }
        } catch (java.net.ConnectException e) {
            System.err.println("Unable to connect to the server:" + this.currentWebServer);
            System.exit(1);
        } catch (InterruptedException e) {
            System.err.println("Interrupted.");
        } catch (MalformedURLException e) {
            System.err.println("Wrong URL Format.");

        } catch (IOException e) {
            System.err.println("Client cannot send the request(I/O PROBLEM).");

        }
        return httpResponse.body();
    }
}


