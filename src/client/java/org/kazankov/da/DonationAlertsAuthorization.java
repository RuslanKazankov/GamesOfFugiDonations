package org.kazankov.da;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class DonationAlertsAuthorization {

    private static final int appId = SecretConstants.DonationAlertsAppId;
    private static final String apiKey = SecretConstants.DonationAlertsApiKey;
    private static final String redirectUrl = SecretConstants.DonationAlertsRedirectUrl;
    private static final List<String> scopes = Arrays.asList("oauth-user-show", "oauth-donation-subscribe", "oauth-donation-index", "oauth-goal-subscribe", "oauth-poll-subscribe");

    private Header getAuthorizationHeader(String token){
        return new BasicHeader("Authorization", "Bearer " + token);
    }

    private String accessToken;
    private String socketToken;
    private int daUserId;

    private boolean auth;

    public DonationAlertsAuthorization(){
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8080)) {
                System.out.println("Server is listening on port 8080");
                while (true) {
                    Socket socket = serverSocket.accept();
                    System.out.println("New client connected");
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);


                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/plain");
                    out.println();
                    out.println("Вы успешно авторизировались! Наверное...");

                    String requestLine = in.readLine();
                    String[] requestParts = requestLine.split(" ");
                    if (requestParts.length >= 2) {
                        String requestPath = requestParts[1];
                        // Парсим параметры из URL
                        String code = null;
                        if (requestPath.contains("?")) {
                            String queryString = requestPath.substring(requestPath.indexOf("?") + 1);
                            for (String param : queryString.split("&")) {
                                String[] keyValue = param.split("=");
                                if (keyValue.length == 2 && "code".equals(keyValue[0])) {
                                    code = keyValue[1];
                                    break;
                                }
                            }
                        }
                        auth = true;

                        accessToken = getAccessToken(code);
                        System.out.println(accessToken);
                        getDaUser(accessToken);
                    }
                    break;
                }
            } catch (IOException e) {
                System.err.println("Error in server: " + e.getMessage());
                e.printStackTrace();
            }
        });
        serverThread.start();
    }

    public void getDaUser(String token){
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet userRequest = new HttpGet("https://www.donationalerts.com/api/v1/user/oauth");
            userRequest.addHeader(getAuthorizationHeader(token));
            HttpEntity responseEntity = httpClient.execute(userRequest).getEntity();

            JsonObject daUser = new Gson().fromJson(EntityUtils.toString(responseEntity), JsonObject.class);
            System.out.println(daUser);
            JsonObject data = daUser.getAsJsonObject("data");
            socketToken = data.get("socket_connection_token").getAsString();
            daUserId = data.get("id").getAsInt();
            auth = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getAuthorizationUrl(){
        StringBuilder scopesStr = new StringBuilder();
        for (int i = 0; i < scopes.size(); i++){
            scopesStr.append(scopes.get(i));
            if (i != scopes.size() - 1)
                scopesStr.append("%20");
        }
        scopesStr = new StringBuilder(scopesStr.toString());
        return "https://www.donationalerts.com/oauth/authorize?" +
                "client_id=" + appId +
                "&redirect_uri=" + redirectUrl +
                "&response_type=code" +
                "&scope=" + scopesStr;
    }


    public String getAccessToken() {
        return accessToken;
    }

    public String getSocketToken() {
        return socketToken;
    }

    public int getDaUserId() {
        return daUserId;
    }

    public boolean isAuth() {
        return auth;
    }

    private String getAccessToken(String code) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost accessTokenRequest = new HttpPost("https://www.donationalerts.com/oauth/token");
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("client_id", Integer.toString(appId)));
        params.add(new BasicNameValuePair("client_secret", apiKey));
        params.add(new BasicNameValuePair("redirect_uri", redirectUrl));
        params.add(new BasicNameValuePair("code", code));
        accessTokenRequest.setEntity(new UrlEncodedFormEntity(params));
        HttpEntity httpEntity = httpClient.execute(accessTokenRequest).getEntity();
        String result = EntityUtils.toString(httpEntity);
        httpClient.close();
        JsonObject jsonObject = new Gson().fromJson(result, JsonObject.class);
        return jsonObject.get("access_token").getAsString();
    }
}
