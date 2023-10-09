package com.loohp.hkbuseta.utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

public class HTTPRequestUtils {

    public static String getTextResponse(String link) {
        try {
            URL url = new URL(link);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    return reader.lines().collect(Collectors.joining());
                }
            } else {
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }

    public static JSONObject getJSONResponseWithPercentageCallback(String link, long customContentLength, Consumer<Float> percentageCallback) {
        try {
            URL url = new URL(link);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                float contentLength = customContentLength >= 0 ? customContentLength : connection.getContentLengthLong();
                try (InputStream inputStream = connection.getInputStream()) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int readTotal = 0;
                    int nRead;
                    byte[] data = new byte[16384];
                    while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                        readTotal += nRead;
                        buffer.write(data, 0, nRead);
                        percentageCallback.accept(Math.max(0F, Math.min(readTotal / contentLength, 1F)));
                    }
                    percentageCallback.accept(1F);
                    return new JSONObject(new String(buffer.toByteArray(), 0, buffer.size(), StandardCharsets.UTF_8));
                }
            } else {
                return null;
            }
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    public static JSONObject getJSONResponse(String link) {
        try {
            URL url = new URL(link);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String reply = reader.lines().collect(Collectors.joining());
                    return new JSONObject(reply);
                }
            } else {
                return null;
            }
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    public static JSONObject postJSONResponse(String link, JSONObject body) {
        try {
            URL url = new URL(link);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String reply = reader.lines().collect(Collectors.joining());
                    return new JSONObject(reply);
                }
            } else {
                return null;
            }
        } catch (IOException | JSONException e) {
            return null;
        }
    }

    public static InputStream getInputStream(String link) throws IOException {
        URLConnection connection = new URL(link).openConnection();
        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);
        connection.addRequestProperty("User-Agent", "Mozilla/5.0");
        connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
        connection.addRequestProperty("Pragma", "no-cache");
        return connection.getInputStream();
    }

    public static byte[] download(String link) throws IOException {
        try (InputStream is = getInputStream(link)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] byteChunk = new byte[4096];
            int n;
            while ((n = is.read(byteChunk)) > 0) {
                baos.write(byteChunk, 0, n);
            }
            return baos.toByteArray();
        }
    }

    public static long getContentSize(String link) {
        try {
            URLConnection connection = new URL(link).openConnection();
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setRequestMethod("HEAD");
            }
            return connection.getContentLengthLong();
        } catch (IOException e) {
            return -1;
        }
    }

    public static String getContentType(String link) {
        try {
            URLConnection connection = new URL(link).openConnection();
            connection.setUseCaches(false);
            connection.setDefaultUseCaches(false);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0");
            connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate");
            connection.addRequestProperty("Pragma", "no-cache");
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).setRequestMethod("HEAD");
            }
            return connection.getContentType();
        } catch (IOException e) {
            return "";
        }
    }

}

