package fr.Boulldogo.WatchLogs.Discord;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.security.auth.login.LoginException;

import com.vdurmont.emoji.EmojiParser;

import fr.Boulldogo.WatchLogs.Main;
import fr.Boulldogo.WatchLogs.Database.DatabaseManager;
import fr.Boulldogo.WatchLogs.Discord.Commands.InfoCommand;
import fr.Boulldogo.WatchLogs.Discord.Commands.ProjectCommand;
import fr.Boulldogo.WatchLogs.Discord.Commands.SetupCommand;
import fr.Boulldogo.WatchLogs.Discord.Commands.SlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class SetupDiscordBot extends ListenerAdapter {

    private final Map<String, SlashCommand> commands = new HashMap<>();
    private JDA jda;
    private final Main plugin;
    @SuppressWarnings("unused")
	private Random random = new Random();
    @SuppressWarnings("unused")
	private final DatabaseManager databaseManager;

    public SetupDiscordBot(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void startBot() throws LoginException {
        String token = plugin.getConfig().getString("discord.bot-token");

        if(token == null || token.isEmpty()) {
            throw new LoginException("[Discord-Module] Discord token is missing in configuration. Discord Module is disabled.");
        }

        JDABuilder jdaBuilder = JDABuilder.create(token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS);
        jdaBuilder.setActivity(Activity.playing(EmojiParser.parseToUnicode(plugin.getConfig().contains("discord.activity_message") ? plugin.getConfig().getString("discord.activity_message") : ":eyes: See you with WatchLogs plugin")));
        jdaBuilder.addEventListeners(this);
        jdaBuilder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
        jdaBuilder.setAutoReconnect(true);

        jda = jdaBuilder.build();

        registerCommands();
        updateCommands();

        displayBotInfo();
    }

    private void registerCommands() {
        addCommand(new InfoCommand(plugin, this));
        addCommand(new ProjectCommand());
        addCommand(new SetupCommand(plugin));
    }

    private void addCommand(SlashCommand command) {
        commands.put(command.getName(), command);
    }

    private void updateCommands() {
        List<CommandData> commandData = new ArrayList<>();
        for (SlashCommand command : commands.values()) {
            commandData.add(command.getCommandData());
        }
        jda.updateCommands()
            .addCommands(commandData)
            .queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        SlashCommand command = commands.get(event.getName());
        if(command != null) {
            command.execute(event);
        }
    }

    public JDA getJDA() {
        return jda;
    }
    
    public void sendDirectLogs(int id, String type, String data, String username, String worldName, String location) {
        EmbedBuilder builder = new EmbedBuilder();
        String colorString = plugin.getConfig().getString("discord.log_embed_color");
        Color embedColor = null;

        if(colorString != null) {
            if(colorString.equalsIgnoreCase("RANDOM")) {
                embedColor = generateRandomColor();
            } else {
                try {
                    embedColor = hexToColor(colorString);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid color format for embed color in config: " + colorString);
                    embedColor = Color.RED; 
                }
            }
        } else {
            embedColor = Color.RED; 
        }
        builder.setAuthor(username, null, plugin.getConfig().getString("discord.url-head-image").replace("%p", username));
        builder.setColor(embedColor);
        builder.setTitle(plugin.getLang().getString("discord.log_title") + " " +  type);
        builder.addField(plugin.getLang().getString("discord.log_player"), username, true);
        builder.addField(plugin.getLang().getString("discord.log_world"), worldName, true);
        builder.addField(plugin.getLang().getString("discord.log_action_id"), String.valueOf(id), true);
        builder.addField(plugin.getLang().getString("discord.log_location"), plugin.getConfig().getBoolean("discord.enable-logs-coords") ? location : plugin.getLang().getString("discord.log_location_hidden"), true);
        builder.addField(plugin.getLang().getString("discord.log_data"), data, true);
        builder.setFooter(translateEmoji(":white_check_mark: Generated by WatchLogs plugin by Boulldogo, type /project for project !"));
        builder.setTimestamp(Instant.now());
        String channelId = plugin.getConfig().getString("discord.live_logs_channel_id");
        if(channelId != null && !channelId.isEmpty()) {
            TextChannel channel = jda.getTextChannelById(channelId);
            if(channel != null) {
                channel.sendMessageEmbeds(builder.build()).queue();
            } else {
                plugin.getLogger().warning("The channel with ID " + channelId + " could not be found.");
            }
        } else {
            plugin.getLogger().warning("Channel ID is not set or is invalid in the configuration.");
        }
    }
    
    public static Color hexToColor(String hex) {
        if(hex.startsWith("#")) {
            hex = hex.substring(1); 
        }
        
        if(hex.length() != 6) {
            throw new IllegalArgumentException("Invalid hex color format: " + hex);
        }
        
        int red = Integer.parseInt(hex.substring(0, 2), 16);
        int green = Integer.parseInt(hex.substring(2, 4), 16);
        int blue = Integer.parseInt(hex.substring(4, 6), 16);

        return new Color(red, green, blue);
    }

    
    public static Color generateRandomColor() {
        Random random = new Random();
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        return new Color(red, green, blue);
    }
    
    public boolean isBotOnline() {
        return jda != null && jda.getStatus() == Status.CONNECTED;
    }
    
    public String translateEmoji(String s) {
    	return EmojiParser.parseToUnicode(s);
    }

    private void displayBotInfo() {
        User selfUser = jda.getSelfUser();
        String userName = selfUser.getName();
        @SuppressWarnings("deprecation")
		String discriminator = selfUser.getDiscriminator();
        String userId = selfUser.getId();

        plugin.getLogger().info(EmojiParser.parseToUnicode("[Discord-Module] Login with client: " + userName + "#" + discriminator + " (ID: " + userId + ")"));
    }
}
