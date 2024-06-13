package org.kazankov;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.kazankov.da.DonationAlertsClient;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class GamesOfFugiDonationsModClient implements ClientModInitializer {
	private ClientPlayerEntity player = null;
	private DonationAlertsClient daClient;

	@Override
	public void onInitializeClient() {
		Event<ClientPlayConnectionEvents.Join> join = ClientPlayConnectionEvents.JOIN;
		join.register((clientPlayNetworkHandler, packetSender, minecraftClient) -> {
			player = minecraftClient.player;
			if (player != null){
				daClient = new DonationAlertsClient(player);
				player.sendMessage(Text.literal("42, братанчик. Я готов обрабатывать донатики."));
			}
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("donations")
						.then(ClientCommandManager.literal("on").executes(context -> {
							if (daClient != null)
								context.getSource().sendFeedback(Text.literal(daClient.checkDonationsOn()));
							else
								context.getSource().sendFeedback(Text.literal("Ты уверен, что ты существуешь?"));
							return 1;
						}))
						.then(ClientCommandManager.literal("off").executes(context -> {
							if (daClient != null)
								context.getSource().sendFeedback(Text.literal(daClient.checkDonationsOff()));
							else
								context.getSource().sendFeedback(Text.literal("Ты уверен, что ты существуешь?"));
							return 1;
						}))
				)
		);
	}
}