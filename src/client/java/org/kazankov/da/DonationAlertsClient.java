package org.kazankov.da;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import okhttp3.internal.http2.Header;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DonationAlertsClient {

    private final ClientPlayerEntity player;
    private CentrifugeClient centrifugeClient;
    private final DonationAlertsAuthorization daAuthorization;

    public DonationAlertsClient(ClientPlayerEntity player){
        this.player = player;
        daAuthorization = new DonationAlertsAuthorization();
    }
    
    private static String getDontAuthString(){
        return "Вы не авторизованы!";
    }

    private void sendAuthUrl(){
        player.sendMessage(Text.literal("Авторизируйтесь здесь:"));
        player.networkHandler.sendChatCommand("tellraw @p [\"\",{\"text\":\"DonationAlerts Login\",\"color\":\"blue\",\"underlined\":true,\"clickEvent\":{\"action\":\"open_url\",\"value\":\"" +
                DonationAlertsAuthorization.getAuthorizationUrl() +
                "\"}}]");
    }

    public String checkDonationsOn(){
        if (!daAuthorization.isAuth()) {
            sendAuthUrl();
            return getDontAuthString();
        }

        if (centrifugeClient != null && centrifugeClient.isEnable()) return "Донаты уже включены!";

        if (centrifugeClient == null) {
            try {
                centrifugeClient = new CentrifugeClient(new URI(CentrifugeClient.WEBSOCKET_URL));
                centrifugeClient.init(daAuthorization.getAccessToken(), daAuthorization.getSocketToken(), daAuthorization.getDaUserId(), this);
                centrifugeClient.connect();
            } catch (java.net.URISyntaxException e) {
                System.out.println("CentrifugeClient не создался: " + e.getMessage());
            }
        }else{
            centrifugeClient.setEnable(true);
        }

        return "Донаты включены!";
    }

    public String checkDonationsOff(){
        if (!daAuthorization.isAuth()) {
            sendAuthUrl();
            return getDontAuthString();
        }

        if (centrifugeClient != null && !centrifugeClient.isEnable()) return "Донаты уже выключены!";

        if (centrifugeClient != null)
            centrifugeClient.setEnable(false);


        return "Донаты выключены!";
    }

    public void addDonation(Donation donation){
        String message = donation.getMessage();
        if (message.startsWith("/whitelist add ") || message.startsWith("/reborn ")) {
            player.networkHandler.sendChatCommand(message.substring(1));
        }
        else player.sendMessage(Text.of("АХАХАХАХАХАХ, школьник облажался"));
    }

}
