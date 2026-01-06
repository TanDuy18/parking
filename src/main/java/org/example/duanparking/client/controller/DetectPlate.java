package org.example.duanparking.client.controller;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DetectPlate {

    private static final String API_URL =
            "https://api.platerecognizer.com/v1/plate-reader/";
    private static final String API_KEY = "9cf7b5b915b8fd12aab6b8fcaad2cae95f39b712";

    public static String detectFromCamera(BufferedImage image) {

        if (image == null) return "";

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            byte[] imageBytes = baos.toByteArray();

            String boundary = "----JavaBoundary" + System.currentTimeMillis();

            HttpURLConnection conn = (HttpURLConnection) new URL(API_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Authorization", "Token " + API_KEY);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream output = conn.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, "UTF-8"), true);

            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"upload\"; filename=\"frame.jpg\"\r\n");
            writer.append("Content-Type: image/jpeg\r\n\r\n");
            writer.flush();

            output.write(imageBytes);
            output.flush();

            writer.append("\r\n");
            writer.append("--").append(boundary).append("--\r\n");
            writer.flush();
            writer.close();

            int code = conn.getResponseCode();
            if (code == 429) {
                System.out.println("API bị giới hạn tần suất");
                return "";
            }

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8")
            );

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            br.close();
            return parsePlate(response.toString());

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String formatVietnamPlate(String plate) {

        if (plate.length() < 7) return plate;

        String soTinh = plate.substring(0, 2);
        String chu = plate.substring(2, plate.length() - 5);
        String so = plate.substring(plate.length() - 5);

        return soTinh + chu + "-" + so.substring(0, 3) + "." + so.substring(3);


    }

    private static String parsePlate(String json) {
        Pattern p = Pattern.compile("\"plate\"\\s*:\\s*\"([A-Za-z0-9\\.\\-]+)\"");
        Matcher m = p.matcher(json);

        if (m.find()) {
            return formatVietnamPlate(m.group(1).toUpperCase());
        }

        return "";
    }
}
